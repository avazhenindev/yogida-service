package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.RevenueCatWebhookRequest;

/**
 * Contract for RevenueCat webhook event processing.
 */
public interface RevenueCatWebhookApi {

    /**
     * Processes a RevenueCat webhook event. On entitlement-changing events
     * (INITIAL_PURCHASE, RENEWAL, CANCELLATION, EXPIRATION) evicts the cached
     * entitlement entry for the affected user so the next entitlement check
     * fetches fresh data from the RevenueCat Subscriber API.
     */
    void processEvent(RevenueCatWebhookRequest request);
}
