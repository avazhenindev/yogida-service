package com.yogida.meditation.dto;

import com.yogida.meditation.enums.SseMessageType;

/**
 * Envelope for every SSE event pushed to connected clients.
 *
 * @param type discriminates the origin/intent of the event
 * @param data event-specific payload; may be {@code null} for type {@link SseMessageType#TEST}
 */
public record SseEvent(SseMessageType type, Object data) {}
