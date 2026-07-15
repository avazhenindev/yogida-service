package com.yogida.meditation.service;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.dto.RevenueCatSubscriberResponse;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Service for checking media entitlement.
 * Entitlement is resolved live from the RevenueCat Subscriber API and cached
 * in Caffeine for up to 5 minutes per user. Cache is evicted immediately on
 * relevant RevenueCat webhook events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementService {

    private final RevenueCatSubscriberClient subscriberClient;
    private final RevenueCatProperties properties;

    /** Self-reference via Spring proxy — required so @Cacheable/@CacheEvict AOP applies on internal calls. */
    @Lazy
    @Autowired
    private EntitlementService self;

    /**
     * Returns true when the media item is premium (has at least one associated subscription plan).
     */
    public boolean isPremium(MediaEntity media) {
        return media.getMediaSubscriptions() != null && !media.getMediaSubscriptions().isEmpty();
    }

    /**
     * Returns true when the user is entitled to access the given media item.
     * Free media always returns true. For premium media, delegates to the
     * cached RevenueCat entitlement check.
     *
     * @param user  the authenticated app user
     * @param media the media item to check
     * @return true if the user may access the media
     */
    public boolean isEntitled(AppUserEntity user, MediaEntity media) {
        if (!isPremium(media)) {
            return true;
        }
        return self.isUserPremium(user.getKeycloakUserId());
    }

    /**
     * Checks the RevenueCat Subscriber API for an active premium entitlement.
     * Result is cached per user for up to 5 minutes (see {@link com.yogida.meditation.config.CacheConfig}).
     * Returns false (deny access) when the RC API is unavailable — secure by default.
     *
     * @param keycloakUserId the user's Keycloak subject, used as the RC app user id
     * @return true when the user has a non-expired premium entitlement
     */
    @Cacheable(value = "entitlement", key = "#keycloakUserId")
    public boolean isUserPremium(String keycloakUserId) {
        return subscriberClient.getSubscriber(keycloakUserId)
                .map(RevenueCatSubscriberResponse::subscriber)
                .map(RevenueCatSubscriberResponse.Subscriber::entitlements)
                .map(entitlements -> entitlements.get(properties.entitlementId()))
                .filter(ent -> ent.expiresDate() == null || ent.expiresDate().isAfter(OffsetDateTime.now()))
                .isPresent();
    }

    /**
     * Evicts the cached entitlement for a user so the next {@link #isUserPremium} call
     * fetches fresh data from the RevenueCat Subscriber API.
     *
     * @param keycloakUserId the user's Keycloak subject
     */
    @CacheEvict(value = "entitlement", key = "#keycloakUserId")
    public void evictUserEntitlement(String keycloakUserId) {
        log.debug("EntitlementService > Cache evicted for user: {}", keycloakUserId);
    }
}
