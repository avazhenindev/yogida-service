package com.yogida.meditation.enums;

/**
 * Discriminates the origin and intent of an SSE message pushed to connected clients.
 */
public enum SseMessageType {

    /** Manually triggered test message; used for connectivity verification. */
    TEST,

    /** Message originating from a RevenueCat webhook event. */
    RC
}
