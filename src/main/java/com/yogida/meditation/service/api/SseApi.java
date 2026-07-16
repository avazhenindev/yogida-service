package com.yogida.meditation.service.api;

import com.yogida.meditation.enums.SseMessageType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Contract for user-scoped Server-Sent Events.
 */
public interface SseApi {

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
     * Pushes a typed {@link com.yogida.meditation.dto.SseEvent} envelope to all active connections
     * of the given user. Dead emitters are silently removed. When the user has no deliverable
     * connection, the event is retained in a bounded per-user pending queue and flushed on the
     * user's next subscribe.
     *
     * @param keycloakUserId the user to notify
     * @param type           discriminates the origin/intent of the event
     * @param payload        the object to serialize as JSON event data; may be {@code null}
     */
    void publishToUser(String keycloakUserId, SseMessageType type, Object payload);
}
