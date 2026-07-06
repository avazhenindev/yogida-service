package com.yogida.meditation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Minimal projection of the RevenueCat Subscriber API response used for reconciliation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RevenueCatSubscriberResponse(Subscriber subscriber) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Subscriber(Map<String, Entitlement> entitlements) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entitlement(
            @JsonProperty("expires_date") OffsetDateTime expiresDate,
            @JsonProperty("product_identifier") String productIdentifier) {
    }
}
