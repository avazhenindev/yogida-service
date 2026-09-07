package com.yogida.meditation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Ledger of RevenueCat webhook events already processed, keyed by the event id RevenueCat
 * assigns. RevenueCat retries deliveries, so without this a single purchase can be handled
 * several times.
 *
 * <p>Rows are claimed with an {@code INSERT ... ON CONFLICT DO NOTHING} rather than through
 * JPA {@code save}: an entity with an assigned (non-generated) id goes through
 * {@code EntityManager.merge}, which SELECTs first and UPDATEs when the row exists, so it
 * raises no constraint violation and provides no mutual exclusion at all. See
 * {@code WebhookIdempotencyService}.
 */
@Getter
@Setter
@Entity
@Table(name = "revenuecat_webhook_event")
public class RevenueCatWebhookEventEntity {

    /** The {@code event.id} from the RevenueCat payload. */
    @Id
    @Column(name = "rc_event_id", length = 255)
    private String rcEventId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "rc_app_user_id", length = 255)
    private String rcAppUserId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
