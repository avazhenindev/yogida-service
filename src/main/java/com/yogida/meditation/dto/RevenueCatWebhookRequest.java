package com.yogida.meditation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Incoming RevenueCat webhook payload (snake_case JSON).
 * Only fields relevant to entitlement projection are mapped.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatWebhookRequest(
        Event event,
        @JsonProperty("api_version") String apiVersion) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Event(
        String id,
        String type,
        @JsonProperty("app_user_id") String appUserId,
        @JsonProperty("original_app_user_id") String originalAppUserId,
        @JsonProperty("product_id") String productId,
        String store,
        String environment,
        @JsonProperty("entitlement_ids") List<String> entitlementIds,
        @JsonProperty("purchased_at_ms") Long purchasedAtMs,
        @JsonProperty("expiration_at_ms") Long expirationAtMs
    ) {
    }
}
