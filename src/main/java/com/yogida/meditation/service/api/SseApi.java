package com.yogida.meditation.service.api;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Contract for user-scoped Server-Sent Events.
 */
public interface SseApi {

    /** The SSE event name every entitlement notification is published under. */
    String ENTITLEMENT_UPDATE_EVENT = "entitlement-update";

    /**
     * Opens a new SSE stream for the given user and registers it in the emitter registry.
     * If an emitter with the same {@code clientId} already exists for the user (e.g. a
     * reconnect after backgrounding the app), the stale emitter is completed and replaced.
     * The emitter is automatically removed on completion, timeout, or error.
     *
     * @param keycloakUserId the authenticated user's Keycloak subject
     * @param clientId       stable per-app-instance connection identifier
     * @return a new {@link SseEmitter} ready to be returned from the controller
     */
    SseEmitter subscribe(String keycloakUserId, String clientId);

    /**
     * Signals every active connection of the given user that their entitlement may have
     * changed, so the client re-queries its own customer info.
     *
     * <p>The payload is deliberately just the originating event type — a hint, not state.
     * Entitlement itself is never pushed over this channel: the client is the one holding a
     * RevenueCat SDK session, and a value pushed from here would be a second, racier source
     * of truth for something the client can read authoritatively.
     *
     * <p>Dead emitters are removed. When the user has no deliverable connection the event is
     * retained in a bounded per-user pending queue and flushed on their next subscribe.
     *
     * @param keycloakUserId the user whose connections should be signalled
     * @param eventType      the originating RevenueCat event type, or a short token for
     *                       synthetic events; sent verbatim as the SSE data field
     */
    void publishToUser(String keycloakUserId, String eventType);
}
