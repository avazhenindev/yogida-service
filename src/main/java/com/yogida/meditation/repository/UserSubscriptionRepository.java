package com.yogida.meditation.repository;

import com.yogida.meditation.entity.UserSubscriptionEntity;
import com.yogida.meditation.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, Long> {

    Optional<UserSubscriptionEntity> findFirstByUserUserIdAndSubscriptionSubscriptionId(Long userId, Long subscriptionId);

    /** Active auto-renewing rows past their end date — candidates for RevenueCat reconciliation. */
    List<UserSubscriptionEntity> findByStatusAndAutoRenewTrueAndRcAppUserIdNotNullAndEndDateBefore(
            SubscriptionStatus status, LocalDate endDateBefore);
}

