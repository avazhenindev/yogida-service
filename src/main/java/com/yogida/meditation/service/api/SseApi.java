package com.yogida.meditation.service.api;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Contract for user-scoped Server-Sent Events.
 */
public interface SseApi {

    /**
     * Opens a new SSE stream for the given user and registers it in the emitter registry.
     * The emitter is automatically removed on completion, timeout, or error.
     *
     * @param keycloakUserId the authenticated user's Keycloak subject
     * @return a new {@link SseEmitter} ready to be returned from the controller
     */
    SseEmitter subscribe(String keycloakUserId);

    /**
     * Pushes {@code payload} as an SSE event to all active connections of the given user.
     * Dead emitters are silently removed. If the user has no active connections, this is a no-op.
     *
     * @param keycloakUserId the user to notify
     * @param payload        the object to serialize as JSON event data
     */
    void publishToUser(String keycloakUserId, Object payload);
}
