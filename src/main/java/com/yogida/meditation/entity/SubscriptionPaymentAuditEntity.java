package com.yogida.meditation.entity;

import com.yogida.meditation.enums.PaymentAuditStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Compact audit record of a processed RevenueCat webhook event.
 * The unique rc_event_id is the idempotency guard for webhook processing.
 */
@Data
@Entity
@Table(name = "subscription_payment_audit")
public class SubscriptionPaymentAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_payment_audit_id")
    private Long subscriptionPaymentAuditId;

    @Column(name = "rc_event_id", nullable = false, unique = true)
    private String rcEventId;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "rc_app_user_id")
    private String rcAppUserId;

    @Column(name = "rc_product_id")
    private String rcProductId;

    @Column(name = "rc_store", length = 30)
    private String rcStore;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private PaymentAuditStatus processingStatus;

    @Column(name = "detail", length = 1000)
    private String detail;

    @Column(name = "raw_payload", columnDefinition = "text")
    private String rawPayload;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();
}
