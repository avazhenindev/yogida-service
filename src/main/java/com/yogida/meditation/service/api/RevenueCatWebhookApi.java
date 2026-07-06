package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.RevenueCatWebhookRequest;

/**
 * Contract for idempotent RevenueCat webhook event processing.
 */
public interface RevenueCatWebhookApi {

    /**
     * Projects a RevenueCat webhook event onto the local user_subscription state
     * and records a compact audit row. Duplicate event ids are ignored.
     */
    void processEvent(RevenueCatWebhookRequest request);
}
