package com.yogida.meditation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RevenueCat integration configuration.
 *
 * @param entitlementId    RevenueCat entitlement identifier that grants premium access
 * @param webhookAuthToken shared secret expected in the webhook Authorization header
 * @param apiKey           RevenueCat secret API key for the Subscriber API
 * @param apiBaseUrl       RevenueCat REST API base URL
 */
@ConfigurationProperties(prefix = "app.revenuecat")
public record RevenueCatProperties(
    String entitlementId,
    String webhookAuthToken,
    String apiKey,
    String apiBaseUrl
) {
    public RevenueCatProperties {
        if (entitlementId == null || entitlementId.isBlank()) {
            entitlementId = "premium";
        }
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://api.revenuecat.com/v1";
        }
    }
}
