package com.yogida.meditation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * RevenueCat integration configuration.
 *
 * @param entitlementId           RevenueCat entitlement identifier that grants premium access
 * @param webhookAuthToken        shared secret expected in the webhook Authorization header
 * @param apiKey                  RevenueCat secret API key for the Subscriber API
 * @param apiBaseUrl              RevenueCat REST API base URL
 * @param entitlementCacheTtl     how long a resolved entitlement stays in the in-memory cache.
 *                                Long by design — see {@link CacheConfig} — and safe to keep
 *                                long because what is cached carries its own expiry and is
 *                                re-evaluated against the clock on every read.
 * @param projectionMaxAge        how old the durable projection may be before a read refreshes
 *                                it from RevenueCat. This is the real staleness bound on
 *                                entitlement, and the backstop for a webhook that never arrived.
 * @param webhookLedgerRetention  how long processed webhook event ids are remembered for
 *                                deduplication before being purged
 * @param outageSuppression       how long a failed RevenueCat lookup suppresses further attempts
 *                                for the same user. Bounds the request amplification an outage
 *                                would otherwise cause; deliberately short, so recovery is picked
 *                                up almost at once. Never an entitlement answer — only a marker
 *                                that asking again right now is pointless.
 */
@ConfigurationProperties(prefix = "app.revenuecat")
public record RevenueCatProperties(
    String entitlementId,
    String webhookAuthToken,
    String apiKey,
    String apiBaseUrl,
    Duration entitlementCacheTtl,
    Duration projectionMaxAge,
    Duration webhookLedgerRetention,
    Duration outageSuppression
) {
    public RevenueCatProperties {
        if (entitlementId == null || entitlementId.isBlank()) {
            entitlementId = "premium";
        }
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://api.revenuecat.com/v1";
        }
        if (entitlementCacheTtl == null || entitlementCacheTtl.isNegative() || entitlementCacheTtl.isZero()) {
            entitlementCacheTtl = Duration.ofHours(24);
        }
        if (projectionMaxAge == null || projectionMaxAge.isNegative() || projectionMaxAge.isZero()) {
            projectionMaxAge = Duration.ofHours(24);
        }
        if (webhookLedgerRetention == null || webhookLedgerRetention.isNegative() || webhookLedgerRetention.isZero()) {
            webhookLedgerRetention = Duration.ofDays(30);
        }
        if (outageSuppression == null || outageSuppression.isNegative() || outageSuppression.isZero()) {
            outageSuppression = Duration.ofSeconds(30);
        }
    }
}
