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

    /**
     * Self-reference via Spring proxy — required so @Cacheable/@CacheEvict AOP applies on internal calls.
     */
    @Lazy
    @Autowired
    private EntitlementService self;

    /**
     * Returns true when the media item requires a premium subscription.
     */
    public boolean isPremium(MediaEntity media) {
        return media.isRequiresPremiumSubscription();
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
        boolean premium = isPremium(media);
        log.debug("EntitlementService > Checking entitlement for user {} and media {} (premium: {})", user.getKeycloakUserId(), media.getId(), premium);
        if (!premium) {
            log.debug("EntitlementService > Media {} is not premium, granting access to user {}", media.getId(), user.getKeycloakUserId());
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
        log.debug("EntitlementService > Checking RevenueCat entitlement for user {}", keycloakUserId);
        var response = subscriberClient.getSubscriber(keycloakUserId);
        response.ifPresentOrElse(r -> log.debug("EntitlementService > RevenueCat response: {}", r), () -> {
            throw new RuntimeException("EntitlementService > RevenueCat response: empty, check logs");
        });

        return response.map(RevenueCatSubscriberResponse::subscriber).map(RevenueCatSubscriberResponse.Subscriber::entitlements).map(entitlements -> entitlements.get(properties.entitlementId())).filter(ent -> {
            OffsetDateTime expiresDate = ent.expiresDate();
            boolean offsetDateTimeAfter = expiresDate.isAfter(OffsetDateTime.now());
            if (offsetDateTimeAfter) {
                log.debug("EntitlementService > User {} has active entitlement {} expiring at {}", keycloakUserId, properties.entitlementId(), expiresDate);
            } else {
                log.debug("EntitlementService > User {} has active entitlements {}", keycloakUserId, expiresDate);
            }
            return offsetDateTimeAfter;
        }).isPresent();
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
