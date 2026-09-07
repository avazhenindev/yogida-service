package com.yogida.meditation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Durable projection of a user's RevenueCat entitlement — one row per user, keyed by the
 * Keycloak subject that doubles as the RevenueCat app user id.
 *
 * <p>This is deliberately <em>not</em> a subscription system. Changelog 030 dropped the local
 * {@code user_subscription} tables on purpose and RevenueCat remains the single source of
 * truth. What this table buys is survival across the two things an in-memory Caffeine cache
 * cannot survive: a restart, and a second instance. Without it a missed webhook leaves
 * entitlement wrong for a full cache TTL — a new subscriber locked out, or a churned one
 * still premium — and nothing ever notices.
 *
 * <p>Only authoritative answers are written here. A RevenueCat outage must never overwrite a
 * good row with a false one.
 */
@Getter
@Setter
@Entity
@Table(name = "user_entitlement")
public class UserEntitlementEntity {

    /** The Keycloak subject, which is also the RevenueCat app user id. */
    @Id
    @Column(name = "keycloak_user_id", length = 100)
    private String keycloakUserId;

    /** Whether RevenueCat reported the premium entitlement when this row was last refreshed. */
    @Column(name = "entitled", nullable = false)
    private boolean entitled;

    /** When the entitlement lapses. Null means it does not expire (lifetime / non-renewing). */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** The product behind the entitlement. Diagnostics only; nothing branches on it. */
    @Column(name = "product_identifier", length = 255)
    private String productIdentifier;

    /** When this row was last refreshed from RevenueCat. Drives staleness. */
    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt;

    /** The webhook event that last refreshed this row, or null when a read miss refreshed it. */
    @Column(name = "source_event_id", length = 255)
    private String sourceEventId;
}
