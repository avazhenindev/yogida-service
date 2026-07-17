package com.yogida.meditation.service;

import com.yogida.meditation.dto.SseEvent;
import com.yogida.meditation.enums.SseMessageType;
import com.yogida.meditation.service.api.SseApi;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Manages per-user SSE emitter registrations and publishes entitlement update events.
 *
 * <p>The emitter registry is keyed by {@code keycloakUserId}, with a nested map keyed by
 * {@code clientId} (a stable per-app-instance identifier sent by the client). A reconnect
 * from the same client replaces its previous emitter instead of accumulating stale ones;
 * distinct clients (multiple devices) coexist. Emitters are removed automatically on
 * completion, timeout, or error.
 *
 * <p>Events that cannot be delivered (no active connection, or all sends fail) are kept in
 * a bounded per-user pending queue and flushed to the client on its next subscribe.
 */
@Log4j2
@Service
public class SseService implements SseApi {

    /**
     * Upper bound of undelivered events retained per user; oldest are dropped first.
     */
    private static final int MAX_PENDING_EVENTS_PER_USER = 20;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, SseEmitter>> registry =
        new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<SseEvent>> pendingEvents =
        new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String keycloakUserId, String clientId) {
        SseEmitter emitter = new SseEmitter(0L);
        ConcurrentHashMap<String, SseEmitter> userEmitters =
            registry.computeIfAbsent(keycloakUserId, k -> new ConcurrentHashMap<>());

        SseEmitter previous = userEmitters.put(clientId, emitter);
        if (previous != null) {
            // Same client reconnected (e.g. app foregrounded) before Tomcat noticed the
            // dead connection — complete the stale emitter instead of keeping both.
            log.debug("SseService > Replacing stale emitter for user {} client {}", keycloakUserId, clientId);
            previous.complete();
        }

        Runnable remove = () -> removeEmitter(keycloakUserId, clientId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ex -> remove.run());

        log.debug("SseService > Registered emitter for user {} client {}; active connections: {}",
            keycloakUserId, clientId, userEmitters.size());

        // Send an initial event immediately so the first bytes travel through the network
        // path (including the Cloudflare tunnel) and the client onopen fires promptly.
        // Without this, the response contains only headers; proxies may buffer it until
        // the first keepalive arrives (up to 25 s later).
        try {
            emitter.send(SseEmitter.event().name("connected").data(""));
        } catch (IOException | IllegalStateException e) {
            log.warn("SseService > Failed to send initial connected event for user {}: {}", keycloakUserId, e.getMessage());
            removeEmitter(keycloakUserId, clientId, emitter);
            return emitter;
        }

        // Deliver events that were published while the user had no usable connection.
        flushPendingEvents(keycloakUserId, clientId, emitter);

        return emitter;
    }

    @Override
    public void publishToUser(String keycloakUserId, SseMessageType type, Object payload) {
        log.debug("SseService > Publishing {} event to user {}: {}", type, keycloakUserId, payload);
        SseEvent envelope = new SseEvent(type, payload);

        ConcurrentHashMap<String, SseEmitter> userEmitters = registry.get(keycloakUserId);
        if (userEmitters == null || userEmitters.isEmpty()) {
            enqueuePendingEvent(keycloakUserId, envelope);
            return;
        }

        int sent = 0;
        for (Map.Entry<String, SseEmitter> entry : userEmitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event()
                    .name("entitlement-update")
                    .data(envelope, MediaType.APPLICATION_JSON));
                log.debug(payload != null
                        ? "SseService > Pushed entitlement update to user {} client {}: {}"
                        : "SseService > Pushed entitlement update to user {} client {} (no payload)",
                    keycloakUserId, entry.getKey(), payload);
                sent++;
            } catch (IOException | IllegalStateException e) {
                // Dead client (broken pipe surfaces as IllegalStateException from
                // ResponseBodyEmitter). Evict; must never break the publishing caller
                // (e.g. the RevenueCat webhook).
                log.debug("SseService > Evicting dead emitter for user {} client {}: {}",
                    keycloakUserId, entry.getKey(), e.getMessage());
                userEmitters.remove(entry.getKey(), entry.getValue());
            }
        }

        if (sent == 0) {
            enqueuePendingEvent(keycloakUserId, envelope);
            return;
        }

        log.info("SseService > Pushed entitlement update to {} connection(s) for user {}",
            sent, keycloakUserId);
    }

    private void removeEmitter(String keycloakUserId, String clientId, SseEmitter emitter) {
        ConcurrentHashMap<String, SseEmitter> userEmitters = registry.get(keycloakUserId);
        // Two-arg remove: only removes when this emitter is still the registered one,
        // so completing a replaced (stale) emitter cannot evict its replacement.
        if (userEmitters != null && userEmitters.remove(clientId, emitter)) {
            log.debug("SseService > Removed emitter for user {} client {}; remaining: {}",
                keycloakUserId, clientId, userEmitters.size());
        }
    }

    private void enqueuePendingEvent(String keycloakUserId, SseEvent event) {
        ConcurrentLinkedDeque<SseEvent> queue =
            pendingEvents.computeIfAbsent(keycloakUserId, k -> new ConcurrentLinkedDeque<>());
        queue.offerLast(event);
        while (queue.size() > MAX_PENDING_EVENTS_PER_USER) {
            queue.pollFirst(); // drop oldest
        }
        log.info("SseService > No deliverable SSE connection for user {}; event queued (pending: {})",
            keycloakUserId, queue.size());
    }

    private void flushPendingEvents(String keycloakUserId, String clientId, SseEmitter emitter) {
        ConcurrentLinkedDeque<SseEvent> queue = pendingEvents.get(keycloakUserId);
        if (queue == null || queue.isEmpty()) {
            return;
        }

        int flushed = 0;
        SseEvent event;
        while ((event = queue.pollFirst()) != null) {
            log.debug("SseService > Flushing pending event to user {} client {}: {}",
                keycloakUserId, clientId, event.data());
            try {
                emitter.send(SseEmitter.event()
                    .name("entitlement-update")
                    .data(event, MediaType.APPLICATION_JSON));
                flushed++;
            } catch (IOException | IllegalStateException e) {
                queue.offerFirst(event); // keep order; retry on the next reconnect
                log.debug("SseService > Flush interrupted for user {} client {}: {}",
                    keycloakUserId, clientId, e.getMessage());
                removeEmitter(keycloakUserId, clientId, emitter);
                break;
            }
        }

        if (flushed > 0) {
            log.info("SseService > Flushed {} pending event(s) to user {} client {}; remaining: {}",
                flushed, keycloakUserId, clientId, queue.size());
        }
    }

    /**
     * Sends an SSE comment keepalive to every active emitter every 25 seconds.
     * Prevents Cloudflare and other reverse proxies from closing idle connections.
     * Dead emitters discovered during the sweep are removed from the registry.
     */
    @Scheduled(fixedDelay = 25_000)
    public void sendKeepAlives() {
        int total = 0;
        for (Map.Entry<String, ConcurrentHashMap<String, SseEmitter>> userEntry : registry.entrySet()) {
            ConcurrentHashMap<String, SseEmitter> userEmitters = userEntry.getValue();
            for (Map.Entry<String, SseEmitter> entry : userEmitters.entrySet()) {
                try {
                    entry.getValue().send(SseEmitter.event().comment("keepalive"));
                    total++;
                } catch (IOException | IllegalStateException e) {
                    userEmitters.remove(entry.getKey(), entry.getValue());
                }
            }
        }
        if (total > 0) {
            log.debug("SseService > Sent keepalive to {} active connection(s)", total);
        }
    }
}
