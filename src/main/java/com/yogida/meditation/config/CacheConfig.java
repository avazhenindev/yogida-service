package com.yogida.meditation.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Caffeine in-memory caches.
 *
 * <ul>
 *   <li><b>entitlement</b> — caches the resolved {@code EntitlementSnapshot} per user.
 *       The TTL comes from {@code app.revenuecat.entitlement-cache-ttl} and defaults to
 *       24 hours, set deliberately long to stay within RevenueCat's free-tier request budget.
 *       That is safe only because the cached value carries its own {@code expiresAt} and is
 *       re-evaluated against the clock on every read — caching a bare boolean for a day would
 *       keep a lapsed subscriber premium for the remainder of the window.</li>
 * </ul>
 *
 * <p>The second cache, <b>entitlementOutage</b>, is a short-lived marker rather than an answer.
 * It exists because failing closed must not also fail loudly: an unreachable RevenueCat is
 * deliberately never cached as "not entitled", so without this marker every premium item in a
 * catalogue response would retry the dead endpoint independently — one outage turning into a
 * request-multiplying storm against a service that is already struggling. The marker suppresses
 * repeat attempts for {@code app.revenuecat.outage-suppression} (30 seconds by default), which
 * is short enough that recovery is noticed almost immediately.
 *
 * <p>Invalidation is layered, because none of the layers is sufficient alone. A webhook evicts
 * and refreshes immediately, but Caffeine is per-instance and in-memory, so an eviction only
 * reaches the instance that received the webhook and nothing survives a restart. The durable
 * {@code user_entitlement} projection covers both, and its own staleness window
 * ({@code app.revenuecat.projection-max-age}) is the real upper bound on how long entitlement
 * can be wrong when a webhook is missed entirely.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ENTITLEMENT_CACHE = "entitlement";
    public static final String ENTITLEMENT_OUTAGE_CACHE = "entitlementOutage";

    @Bean
    public CacheManager cacheManager(RevenueCatProperties revenueCatProperties) {
        CaffeineCacheManager manager = new CaffeineCacheManager(ENTITLEMENT_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(revenueCatProperties.entitlementCacheTtl()));
        // The outage marker lives in its own manager because it needs a radically shorter TTL:
        // the entitlement answer is cached for a day, an outage for seconds.
        manager.registerCustomCache(ENTITLEMENT_OUTAGE_CACHE, Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(revenueCatProperties.outageSuppression())
                .build());
        return manager;
    }
}
