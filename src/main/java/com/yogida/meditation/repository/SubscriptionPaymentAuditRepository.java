package com.yogida.meditation.repository;

import com.yogida.meditation.entity.SubscriptionPaymentAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPaymentAuditRepository extends JpaRepository<SubscriptionPaymentAuditEntity, Long> {

    boolean existsByRcEventId(String rcEventId);
}
