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
        // RevenueCat's docs are explicit that a lookup should search aliases as well as
        // original_app_user_id: they are all ids the same subscriber has been seen under.
        List<String> aliases,
        // TRANSFER carries neither app_user_id nor original_app_user_id — it is the one
        // entitlement-affecting event that names two different subscribers instead of one.
        // Leaving these unmapped is why a transfer resolved to no user at all.
        @JsonProperty("transferred_from") List<String> transferredFrom,
        @JsonProperty("transferred_to") List<String> transferredTo,
        @JsonProperty("product_id") String productId,
        String store,
        String environment,
        @JsonProperty("entitlement_ids") List<String> entitlementIds,
        @JsonProperty("purchased_at_ms") Long purchasedAtMs,
        @JsonProperty("expiration_at_ms") Long expirationAtMs
    ) {
    }
}
