package com.yogida.meditation.service;

import java.io.Serializable;
import java.time.Instant;

/**
 * A resolved entitlement state for one user, as last learned from RevenueCat.
 *
 * <p>This is what gets cached and projected rather than a bare {@code boolean}, and that is
 * the whole point: entitlement is not a fact, it is a fact <em>with an expiry</em>. Caching
 * the boolean means a user whose subscription lapses ten minutes after the cache write keeps
 * premium access for the remainder of the cache TTL — nearly a full day at the default —
 * unless a webhook happens to arrive and evict. Caching the expiry alongside it lets every
 * read re-evaluate against the clock, so a stale entry degrades to "not entitled" on its own.
 *
 * @param entitled          whether RevenueCat reported the premium entitlement at all
 * @param expiresAt         when that entitlement lapses; {@code null} means it never does
 *                          (lifetime and non-renewing purchases have a null expires_date)
 * @param productIdentifier the product behind the entitlement, for diagnostics only
 */
public record EntitlementSnapshot(
        boolean entitled,
        Instant expiresAt,
        String productIdentifier) implements Serializable {

    /** A negative snapshot, for users RevenueCat has no premium entitlement for. */
    public static EntitlementSnapshot notEntitled() {
        return new EntitlementSnapshot(false, null, null);
    }

    /**
     * Whether this snapshot still grants access at the given instant.
     * A null {@code expiresAt} on an entitled snapshot means it does not expire.
     */
    public boolean grantsAccessAt(Instant now) {
        return entitled && (expiresAt == null || expiresAt.isAfter(now));
    }
}
