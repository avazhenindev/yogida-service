package com.yogida.meditation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Whether the app may show a banner for a forwarded entitlement event.
 *
 * <p>Every RevenueCat event that resolves to a known user is forwarded over SSE regardless of
 * these flags, so the app always refreshes. They decide only the {@code bannerAllowed} bit on each
 * frame; which types get a banner, and what it says, is the app's call.
 *
 * <p>A record of its own rather than two more components on {@link RevenueCatProperties}, which
 * tests construct positionally.
 *
 * @param bannerEnabled        global switch. Off, no frame allows a banner.
 * @param sandboxBannerEnabled whether events from RevenueCat's SANDBOX environment may show one.
 *                             Real customers never produce SANDBOX events, so production can turn
 *                             this off to silence TestFlight and App Review traffic without a new
 *                             app build.
 */
@ConfigurationProperties(prefix = "app.entitlement-events")
public record EntitlementEventProperties(Boolean bannerEnabled, Boolean sandboxBannerEnabled) {
    // Defaults live in application.properties, not here.
    public EntitlementEventProperties {
        bannerEnabled = ConfigValues.requireFlag(bannerEnabled, "app.entitlement-events.banner-enabled");
        sandboxBannerEnabled = ConfigValues.requireFlag(
                sandboxBannerEnabled, "app.entitlement-events.sandbox-banner-enabled");
    }

    /**
     * Whether an event from the given RevenueCat {@code environment} may show a banner. Anything
     * other than SANDBOX, including a missing value, counts as production.
     */
    public boolean bannerAllowedFor(String environment) {
        return bannerEnabled && (sandboxBannerEnabled || !"SANDBOX".equalsIgnoreCase(environment));
    }
}
