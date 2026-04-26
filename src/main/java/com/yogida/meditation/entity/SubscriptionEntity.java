package com.yogida.meditation.entity;

import com.yogida.meditation.enums.Currency;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

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

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "period_days", nullable = false)
    private Integer periodDays;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 10)
    private Currency currency = Currency.USD;
}
