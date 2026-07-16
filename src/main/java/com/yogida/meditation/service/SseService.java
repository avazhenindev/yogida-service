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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages per-user SSE emitter registrations and publishes entitlement update events.
 *
 * <p>The emitter registry uses a {@link ConcurrentHashMap} keyed by {@code keycloakUserId}
 * with {@link CopyOnWriteArrayList} values so that multiple concurrent tabs or devices per
 * user are supported. Emitters are removed automatically on completion, timeout, or error.
 */
@Log4j2
@Service
public class SseService implements SseApi {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> registry =
            new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(String keycloakUserId) {
        SseEmitter emitter = new SseEmitter(0L);
        registry.computeIfAbsent(keycloakUserId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> removeEmitter(keycloakUserId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ex -> remove.run());

        log.debug("SseService > Registered emitter for user {}; active connections: {}",
                keycloakUserId,
                registry.getOrDefault(keycloakUserId, new CopyOnWriteArrayList<>()).size());
        if (registry.getOrDefault(keycloakUserId, new CopyOnWriteArrayList<>()).size() > 1) {
            log.warn("SseService > Multiple emitters registered for user {} — stale connection likely present",
                    keycloakUserId);
        }
        return emitter;
    }

    @Override
    public void publishToUser(String keycloakUserId, SseMessageType type, Object payload) {
        CopyOnWriteArrayList<SseEmitter> emitters = registry.get(keycloakUserId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("SseService > No active SSE connections for user {}", keycloakUserId);
            return;
        }

        SseEvent envelope = new SseEvent(type, payload);
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("entitlement-update")
                        .data(envelope, MediaType.APPLICATION_JSON));
            } catch (IOException e) {
                log.warn("SseService > Failed to send SSE event to user {}: {}", keycloakUserId, e.getMessage());
                dead.add(emitter);
            }
        }

        if (!dead.isEmpty()) {
            emitters.removeAll(dead);
        }

        log.info("SseService > Pushed entitlement update to {} connection(s) for user {}",
                emitters.size(), keycloakUserId);
    }

    private void removeEmitter(String keycloakUserId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = registry.get(keycloakUserId);
        if (emitters != null) {
            emitters.remove(emitter);
            log.debug("SseService > Removed emitter for user {}; remaining: {}", keycloakUserId, emitters.size());
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
        for (Map.Entry<String, CopyOnWriteArrayList<SseEmitter>> entry : registry.entrySet()) {
            CopyOnWriteArrayList<SseEmitter> emitters = entry.getValue();
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("keepalive"));
                    total++;
                } catch (IOException e) {
                    dead.add(emitter);
                }
            }
            if (!dead.isEmpty()) {
                emitters.removeAll(dead);
            }
        }
        if (total > 0) {
            log.debug("SseService > Sent keepalive to {} active connection(s)", total);
        }
    }
}