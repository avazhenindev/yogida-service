package com.yogida.meditation.service;

import com.yogida.meditation.config.CacheConfig;
import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.dto.RevenueCatSubscriberResponse;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.UserEntitlementEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves whether a user may access premium media.
 *
 * <p>RevenueCat is the single source of truth. Three layers sit in front of it, in order:
 * an in-memory Caffeine cache, a durable {@code user_entitlement} projection, and finally the
 * Subscriber API itself. Each layer holds an {@link EntitlementSnapshot} — a flag plus its
 * expiry — rather than a bare boolean, so a stale entry stops granting access on its own
 * schedule instead of persisting until something evicts it.
 *
 * <p><b>Failure is closed.</b> When RevenueCat cannot be reached the answer is {@code false},
 * and that answer is neither cached nor projected: an outage denies premium content for the
 * duration of the outage and not a second longer. The previous implementation threw on an
 * unreachable RevenueCat, which surfaced as a 500 on the whole catalogue — contradicting the
 * "secure by default" contract stated in this class's own Javadoc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementService {

    private final RevenueCatSubscriberClient subscriberClient;
    private final RevenueCatProperties properties;
    private final EntitlementProjectionService projectionService;
    private final CacheManager cacheManager;

    /**
     * Returns true when the media item requires a premium subscription.
     */
    public boolean isPremium(MediaEntity media) {
        return media.isRequiresPremiumSubscription();
    }

    /**
     * Returns true when the user is entitled to access the given media item.
     * Free media always returns true; premium media delegates to {@link #isUserPremium}.
     */
    public boolean isEntitled(AppUserEntity user, MediaEntity media) {
        if (!isPremium(media)) {
            return true;
        }
        return isUserPremium(user.getKeycloakUserId());
    }

    /**
     * Whether the user currently holds the premium entitlement.
     *
     * @param keycloakUserId the user's Keycloak subject, which doubles as the RevenueCat app user id
     * @return true when a non-expired premium entitlement is known to exist; false when it does
     *         not, and false when RevenueCat cannot be reached
     */
    public boolean isUserPremium(String keycloakUserId) {
        Instant now = Instant.now();

        EntitlementSnapshot cached = cachedSnapshot(keycloakUserId);
        if (cached != null) {
            return cached.grantsAccessAt(now);
        }

        Optional<UserEntitlementEntity> projected = projectionService.find(keycloakUserId);
        if (projected.isEmpty() && isSuppressedByOutage(keycloakUserId)) {
            // RevenueCat just failed for this user and there is nothing to fall back on.
            // Deny without asking again — see CacheConfig for why this marker exists.
            return false;
        }
        if (projected.isPresent() && isFresh(projected.get(), now)) {
            EntitlementSnapshot snapshot = toSnapshot(projected.get());
            putCache(keycloakUserId, snapshot);
            return snapshot.grantsAccessAt(now);
        }

        return resolveFromRevenueCat(keycloakUserId, null, now)
                .map(snapshot -> snapshot.grantsAccessAt(now))
                // RevenueCat unreachable and no usable projection: deny, and remember nothing.
                .orElse(false);
    }

    /**
     * Re-reads a user's entitlement from RevenueCat and refreshes both the projection and the
     * cache. Called from the webhook path, which is what makes a subscription change visible
     * immediately rather than at the next cache expiry.
     *
     * <p>When RevenueCat is unreachable the cache entry is evicted rather than overwritten, so
     * the next read retries instead of serving a value the event just invalidated.
     */
    public void refreshUserEntitlement(String keycloakUserId, String sourceEventId) {
        // Deliberately does not consult the outage marker: a webhook is new information, and
        // suppressing the refresh would discard the very signal that prompted it.
        Optional<EntitlementSnapshot> refreshed =
                resolveFromRevenueCat(keycloakUserId, sourceEventId, Instant.now());
        if (refreshed.isEmpty()) {
            log.warn("EntitlementService > Could not refresh entitlement for {} (event {}); "
                    + "evicting cache so the next read retries", keycloakUserId, sourceEventId);
            evictUserEntitlement(keycloakUserId);
        }
    }

    /**
     * Drops the cached entitlement for a user so the next read resolves it again.
     * In-memory and per-instance: the durable projection is what covers restarts and replicas.
     */
    public void evictUserEntitlement(String keycloakUserId) {
        Cache cache = cache();
        if (cache != null) {
            cache.evict(keycloakUserId);
        }
        log.debug("EntitlementService > Cache evicted for user: {}", keycloakUserId);
    }

    /**
     * Asks RevenueCat and, when the answer is authoritative, records it in the projection and
     * the cache. Returns empty only when RevenueCat could not be reached — the one case where
     * nothing may be remembered.
     */
    private Optional<EntitlementSnapshot> resolveFromRevenueCat(
            String keycloakUserId, String sourceEventId, Instant now) {
        SubscriberLookup lookup = subscriberClient.getSubscriber(keycloakUserId);

        if (lookup instanceof SubscriberLookup.Unavailable unavailable) {
            log.warn("EntitlementService > RevenueCat unavailable for user {} ({}); denying premium access",
                    keycloakUserId, unavailable.reason());
            // Marks the failure, never the answer: the entitlement cache stays untouched so a
            // recovery is picked up as soon as this short marker lapses.
            markOutage(keycloakUserId);
            return Optional.empty();
        }

        EntitlementSnapshot snapshot = snapshotOf(lookup);
        projectionService.upsert(keycloakUserId, snapshot, sourceEventId);
        putCache(keycloakUserId, snapshot);
        log.debug("EntitlementService > Resolved entitlement for {} from RevenueCat: entitled={} expiresAt={}",
                keycloakUserId, snapshot.entitled(), snapshot.expiresAt());
        return Optional.of(snapshot);
    }

    /**
     * Projects a RevenueCat lookup onto a snapshot.
     *
     * <p>Every dereference here is guarded, because RevenueCat's response shape is not
     * guaranteed: {@code entitlements} may be absent entirely, and a lifetime or non-renewing
     * purchase carries a null {@code expires_date}. The latter used to be dereferenced
     * unconditionally, so a lifetime subscriber's first premium request threw a
     * NullPointerException.
     */
    private EntitlementSnapshot snapshotOf(SubscriberLookup lookup) {
        if (!(lookup instanceof SubscriberLookup.Found found)) {
            // Unknown: RevenueCat has no such subscriber. An authoritative negative.
            return EntitlementSnapshot.notEntitled();
        }
        Map<String, RevenueCatSubscriberResponse.Entitlement> entitlements =
                found.response().subscriber().entitlements();
        if (entitlements == null) {
            return EntitlementSnapshot.notEntitled();
        }
        RevenueCatSubscriberResponse.Entitlement entitlement = entitlements.get(properties.entitlementId());
        if (entitlement == null) {
            return EntitlementSnapshot.notEntitled();
        }
        return new EntitlementSnapshot(
                true,
                entitlement.expiresDate() == null ? null : entitlement.expiresDate().toInstant(),
                entitlement.productIdentifier());
    }

    private EntitlementSnapshot toSnapshot(UserEntitlementEntity row) {
        return new EntitlementSnapshot(row.isEntitled(), row.getExpiresAt(), row.getProductIdentifier());
    }

    /** Whether a projected row is recent enough to answer without calling RevenueCat. */
    private boolean isFresh(UserEntitlementEntity row, Instant now) {
        Instant refreshedAt = row.getRefreshedAt();
        return refreshedAt != null && refreshedAt.isAfter(now.minus(properties.projectionMaxAge()));
    }

    private EntitlementSnapshot cachedSnapshot(String keycloakUserId) {
        Cache cache = cache();
        return cache == null ? null : cache.get(keycloakUserId, EntitlementSnapshot.class);
    }

    private void putCache(String keycloakUserId, EntitlementSnapshot snapshot) {
        Cache cache = cache();
        if (cache != null) {
            cache.put(keycloakUserId, snapshot);
        }
    }

    private boolean isSuppressedByOutage(String keycloakUserId) {
        Cache outage = cacheManager.getCache(CacheConfig.ENTITLEMENT_OUTAGE_CACHE);
        return outage != null && outage.get(keycloakUserId) != null;
    }

    private void markOutage(String keycloakUserId) {
        Cache outage = cacheManager.getCache(CacheConfig.ENTITLEMENT_OUTAGE_CACHE);
        if (outage != null) {
            outage.put(keycloakUserId, Boolean.TRUE);
        }
    }

    private Cache cache() {
        return cacheManager.getCache(CacheConfig.ENTITLEMENT_CACHE);
    }
}
