package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Catalog entity representing a reusable subscription plan definition.
 */
@Data
@Entity
@Table(name = "subscription")
public class SubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "period_days", nullable = false)
    private Integer periodDays;

    @Column(name = "details", length = 1000)
    private String details;
}
