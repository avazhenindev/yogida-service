package com.yogida.meditation.entity;

import com.yogida.meditation.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Entity representing a user's active subscription instance.
 */
@Data
@Entity
@Table(name = "user_subscription")
public class UserSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_subscription_id")
    private Long userSubscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_subscription_user"))
    private AppUserEntity user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_subscription_subscription"))
    private SubscriptionEntity subscription;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew;
}

