package com.yogida.meditation.service;

import com.yogida.meditation.dto.RevenueCatSubscriberResponse;

/**
 * Outcome of a RevenueCat Subscriber API lookup.
 *
 * <p>The distinction this type exists to make: {@link Found} and {@link Unknown} are
 * <em>authoritative</em> answers from RevenueCat and may be cached and projected, while
 * {@link Unavailable} means we simply do not know. Collapsing the two — as a bare
 * {@code Optional.empty()} does — lets a transient outage or a rotated API key be recorded
 * as "not entitled" and served from cache for the whole TTL, locking out paying users.
 *
 * <p>Note on RevenueCat semantics: {@code GET /v1/subscribers/{id}} normally returns 200 and
 * lazily creates the subscriber, so a user who has never purchased arrives as {@link Found}
 * with an empty entitlements map. That is a legitimate, cacheable negative. The {@link Unknown}
 * branch is the defensive path for an app user id RevenueCat rejects outright.
 */
public sealed interface SubscriberLookup {

    /** RevenueCat returned 200 with a subscriber body. */
    record Found(RevenueCatSubscriberResponse response) implements SubscriberLookup {}

    /** RevenueCat returned 404 — no such subscriber. An authoritative "not entitled". */
    record Unknown() implements SubscriberLookup {}

    /** The call could not be completed (5xx, 429, 401/403, timeout, empty body). Not authoritative. */
    record Unavailable(String reason) implements SubscriberLookup {}
}
