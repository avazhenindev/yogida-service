package com.yogida.meditation.repository;

import com.yogida.meditation.entity.UserSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, Long> {
}

