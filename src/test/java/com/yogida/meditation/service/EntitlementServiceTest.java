package com.yogida.meditation.service;

import com.yogida.meditation.config.CacheConfig;
import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.dto.RevenueCatSubscriberResponse;
import com.yogida.meditation.entity.UserEntitlementEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the three entitlement defects this class was rewritten to fix: a RevenueCat outage
 * throwing instead of denying, a lifetime entitlement's null {@code expires_date} raising a
 * NullPointerException, and a transient failure being remembered as "not entitled".
 */
@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    private static final String USER = "kc-user-1";
    private static final String ENTITLEMENT_ID = "premium";

    @Mock private RevenueCatSubscriberClient subscriberClient;
    @Mock private EntitlementProjectionService projectionService;

    private ConcurrentMapCacheManager cacheManager;
    private EntitlementService service;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager(
                CacheConfig.ENTITLEMENT_CACHE, CacheConfig.ENTITLEMENT_OUTAGE_CACHE);
        RevenueCatProperties properties = new RevenueCatProperties(
                ENTITLEMENT_ID, "token", "key", "https://api.revenuecat.com/v1",
                Duration.ofHours(24), Duration.ofHours(24), Duration.ofDays(30),
                Duration.ofSeconds(30));
        service = new EntitlementService(subscriberClient, properties, projectionService, cacheManager);
    }

    // --- fail closed -------------------------------------------------------

    @Test
    void revenueCatUnavailable_deniesAccessInsteadOfThrowing() {
        when(subscriberClient.getSubscriber(USER))
                .thenReturn(new SubscriberLookup.Unavailable("503"));
        when(projectionService.find(USER)).thenReturn(Optional.empty());

        assertThat(service.isUserPremium(USER)).isFalse();
    }

    /**
     * The important half of failing closed: an outage must not be remembered. Caching it would
     * lock the user out for the whole TTL — a full day at the default — long after RevenueCat
     * recovered.
     */
    @Test
    void revenueCatUnavailable_isNeitherCachedNorProjected() {
        when(subscriberClient.getSubscriber(USER))
                .thenReturn(new SubscriberLookup.Unavailable("503"));
        when(projectionService.find(USER)).thenReturn(Optional.empty());

        service.isUserPremium(USER);

        assertThat(cacheManager.getCache(CacheConfig.ENTITLEMENT_CACHE).get(USER)).isNull();
        verify(projectionService, never()).upsert(any(), any(), any());
    }

    /** A 404 is RevenueCat telling us the answer, not failing to answer. It is cacheable. */
    @Test
    void unknownSubscriber_isAnAuthoritativeNegativeAndIsRemembered() {
        when(subscriberClient.getSubscriber(USER)).thenReturn(new SubscriberLookup.Unknown());
        when(projectionService.find(USER)).thenReturn(Optional.empty());

        assertThat(service.isUserPremium(USER)).isFalse();
        verify(projectionService).upsert(eq(USER), any(EntitlementSnapshot.class), eq(null));
        assertThat(cacheManager.getCache(CacheConfig.ENTITLEMENT_CACHE).get(USER)).isNotNull();
    }

    /**
     * Failing closed must not also fail loudly. Because an outage is deliberately never cached
     * as an answer, without a suppression marker every premium item in a catalogue response
     * would independently retry the dead endpoint — turning one outage into a request storm.
     */
    @Test
    void repeatedLookupsDuringAnOutage_hitRevenueCatOnlyOnce() {
        when(subscriberClient.getSubscriber(USER))
                .thenReturn(new SubscriberLookup.Unavailable("503"));
        when(projectionService.find(USER)).thenReturn(Optional.empty());

        for (int i = 0; i < 5; i++) {
            assertThat(service.isUserPremium(USER)).isFalse();
        }

        verify(subscriberClient, times(1)).getSubscriber(USER);
    }

    /** A webhook is new information, so it must refresh even while the marker is set. */
    @Test
    void webhookRefresh_ignoresTheOutageMarker() {
        when(projectionService.find(USER)).thenReturn(Optional.empty());
        when(subscriberClient.getSubscriber(USER))
                .thenReturn(new SubscriberLookup.Unavailable("503"));
        service.isUserPremium(USER);

        givenSubscriberWithEntitlement(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
        service.refreshUserEntitlement(USER, "evt-9");

        assertThat(service.isUserPremium(USER)).isTrue();
        verify(projectionService).upsert(eq(USER), any(EntitlementSnapshot.class), eq("evt-9"));
    }

    // --- null expires_date -------------------------------------------------

    @Test
    void nullExpiresDate_isTreatedAsNonExpiring() {
        givenSubscriberWithEntitlement(null);
        when(projectionService.find(USER)).thenReturn(Optional.empty());

        assertThat(service.isUserPremium(USER)).isTrue();
    }

    @Test
    void futureExpiresDate_grantsAccess() {
        givenSubscriberWithEntitlement(OffsetDateTime.now(ZoneOffset.UTC).plusDays(3));
        when(projectionService.find(USER)).thenReturn(Optional.empty());

        assertThat(service.isUserPremium(USER)).isTrue();
    }

    @Test
    void pastExpiresDate_deniesAccess() {
        givenSubscriberWithEntitlement(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        when(projectionService.find(USER)).thenReturn(Optional.empty());

        assertThat(service.isUserPremium(USER)).isFalse();
    }

    @Test
    void missingEntitlementsMap_deniesAccessWithoutNpe() {
        when(subscriberClient.getSubscriber(USER)).thenReturn(new SubscriberLookup.Found(
                new RevenueCatSubscriberResponse(new RevenueCatSubscriberResponse.Subscriber(null))));
        when(projectionService.find(USER)).thenReturn(Optional.empty());

        assertThat(service.isUserPremium(USER)).isFalse();
    }

    @Test
    void differentEntitlementId_deniesAccess() {
        when(subscriberClient.getSubscriber(USER)).thenReturn(new SubscriberLookup.Found(
                new RevenueCatSubscriberResponse(new RevenueCatSubscriberResponse.Subscriber(
                        Map.of("some_other_entitlement", new RevenueCatSubscriberResponse.Entitlement(
                                OffsetDateTime.now(ZoneOffset.UTC).plusDays(30), "prod"))))));
        when(projectionService.find(USER)).thenReturn(Optional.empty());

        assertThat(service.isUserPremium(USER)).isFalse();
    }

    // --- the cache carries an expiry, so a long TTL stays safe --------------

    /**
     * The reason an {@code EntitlementSnapshot} is cached rather than a boolean. A cached
     * positive whose own expiry has since passed must stop granting access without waiting for
     * the 24-hour TTL or a webhook.
     */
    @Test
    void cachedEntitlementThatHasSinceExpired_deniesAccessWithoutRefetching() {
        cacheManager.getCache(CacheConfig.ENTITLEMENT_CACHE).put(USER,
                new EntitlementSnapshot(true, Instant.now().minusSeconds(60), "prod_month"));

        assertThat(service.isUserPremium(USER)).isFalse();
        verify(subscriberClient, never()).getSubscriber(any());
    }

    @Test
    void cacheHit_shortCircuitsRevenueCat() {
        cacheManager.getCache(CacheConfig.ENTITLEMENT_CACHE).put(USER,
                new EntitlementSnapshot(true, null, "lifetime"));

        assertThat(service.isUserPremium(USER)).isTrue();
        verify(subscriberClient, never()).getSubscriber(any());
    }

    // --- the durable projection --------------------------------------------

    /**
     * The projection is what survives a restart and a second instance, so a fresh row must be
     * answerable without a RevenueCat call.
     */
    @Test
    void freshProjection_answersWithoutCallingRevenueCat() {
        when(projectionService.find(USER)).thenReturn(Optional.of(
                projection(true, Instant.now().plusSeconds(3600), Instant.now())));

        assertThat(service.isUserPremium(USER)).isTrue();
        verify(subscriberClient, never()).getSubscriber(any());
    }

    @Test
    void staleProjection_isRefreshedFromRevenueCat() {
        when(projectionService.find(USER)).thenReturn(Optional.of(
                projection(true, null, Instant.now().minus(Duration.ofDays(2)))));
        givenSubscriberWithEntitlement(OffsetDateTime.now(ZoneOffset.UTC).plusDays(1));

        assertThat(service.isUserPremium(USER)).isTrue();
        verify(subscriberClient).getSubscriber(USER);
    }

    /** A projected row that has since expired denies access even while still "fresh". */
    @Test
    void freshProjectionPastItsExpiry_deniesAccess() {
        when(projectionService.find(USER)).thenReturn(Optional.of(
                projection(true, Instant.now().minusSeconds(60), Instant.now())));

        assertThat(service.isUserPremium(USER)).isFalse();
    }

    // --- helpers -----------------------------------------------------------

    private void givenSubscriberWithEntitlement(OffsetDateTime expiresAt) {
        when(subscriberClient.getSubscriber(USER)).thenReturn(new SubscriberLookup.Found(
                new RevenueCatSubscriberResponse(new RevenueCatSubscriberResponse.Subscriber(
                        Map.of(ENTITLEMENT_ID, new RevenueCatSubscriberResponse.Entitlement(
                                expiresAt, "prod_month"))))));
    }

    private UserEntitlementEntity projection(boolean entitled, Instant expiresAt, Instant refreshedAt) {
        UserEntitlementEntity row = new UserEntitlementEntity();
        row.setKeycloakUserId(USER);
        row.setEntitled(entitled);
        row.setExpiresAt(expiresAt);
        row.setRefreshedAt(refreshedAt);
        return row;
    }
}
