package com.yogida.meditation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * RevenueCat integration configuration.
 *
 * @param entitlementId           RevenueCat entitlement identifier that grants premium access
 * @param webhookAuthToken        shared secret expected in the webhook Authorization header
 * @param webhookSigningSecret    HMAC signing secret from the RevenueCat dashboard's "HMAC webhook
 *                                signing" toggle. Set, every delivery must carry a valid
 *                                X-RevenueCat-Webhook-Signature; blank, the header is not checked
 *                                and webhookAuthToken is the only gate.
 * @param webhookSignatureTolerance how far the signature's own timestamp may sit from this clock
 *                                before the delivery is rejected as a replay. RevenueCat re-signs
 *                                every attempt, so this covers clock skew and the latency of one
 *                                POST — not the 5/10/20/40/80-minute retry ladder.
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
    String webhookSigningSecret,
    Duration webhookSignatureTolerance,
    String apiKey,
    String apiBaseUrl,
    Duration entitlementCacheTtl,
    Duration projectionMaxAge,
    Duration webhookLedgerRetention,
    Duration outageSuppression
) {
    // Defaults live in application.properties, not here. webhookAuthToken, webhookSigningSecret
    // and apiKey are deliberately unvalidated: all three default to empty so a local stack runs
    // without RevenueCat credentials, and the webhook path checks the secrets itself.
    public RevenueCatProperties {
        entitlementId = ConfigValues.requireText(entitlementId, "app.revenuecat.entitlement-id");
        apiBaseUrl = ConfigValues.requireText(apiBaseUrl, "app.revenuecat.api-base-url");
        entitlementCacheTtl = ConfigValues.requirePositive(
                entitlementCacheTtl, "app.revenuecat.entitlement-cache-ttl");
        projectionMaxAge = ConfigValues.requirePositive(
                projectionMaxAge, "app.revenuecat.projection-max-age");
        webhookLedgerRetention = ConfigValues.requirePositive(
                webhookLedgerRetention, "app.revenuecat.webhook-ledger-retention");
        outageSuppression = ConfigValues.requirePositive(
                outageSuppression, "app.revenuecat.outage-suppression");
        webhookSignatureTolerance = ConfigValues.requirePositive(
                webhookSignatureTolerance, "app.revenuecat.webhook-signature-tolerance");
    }
}
