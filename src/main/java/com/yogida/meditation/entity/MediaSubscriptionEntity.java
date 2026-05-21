package com.yogida.meditation.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/*
 * Junction entity to map media with subscription ManyToMany relationship.
 * This allows us to track which media items are associated with which subscriptions.
 * */
@Data
@Entity
@Table(name = "media_subscription", uniqueConstraints = {
    @UniqueConstraint(name = "uk_media_subscription", columnNames = {"media_id", "subscription_id"})
})
public class MediaSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_subscription_id")
    private Long mediaSubscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false, foreignKey = @ForeignKey(name = "fk_media_subscription_media"))
    private MediaEntity media;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false, foreignKey = @ForeignKey(name = "fk_media_subscription_subscription"))
    private SubscriptionEntity subscription;

    private LocalDateTime createdAt;
}

