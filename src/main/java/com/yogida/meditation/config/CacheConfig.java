package com.yogida.meditation.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configures Caffeine in-memory caches.
 * <ul>
 *   <li><b>entitlement</b> — caches RevenueCat subscriber entitlement results per user.
 *       The cache is invalidated immediately via {@code evictUserEntitlement()} when a
 *       RevenueCat webhook signals a subscription change (purchase, renewal, cancellation,
 *       expiration). The 24-hour TTL acts as a safety-net only — it is NOT the primary
 *       invalidation mechanism and is set intentionally long to stay well within RevenueCat's
 *       API rate limits (≤ 1 000 req/month on the free tier).</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("entitlement");
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(24, TimeUnit.HOURS));
        return manager;
    }
}
