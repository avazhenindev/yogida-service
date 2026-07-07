package com.yogida.meditation.service;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.dto.RevenueCatSubscriberResponse;
import com.yogida.meditation.entity.UserSubscriptionEntity;
import com.yogida.meditation.enums.SubscriptionStatus;
import com.yogida.meditation.repository.UserSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Safety-net reconciliation of stale local entitlement projections against the
 * RevenueCat Subscriber API. Handles missed or out-of-order webhooks.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RevenueCatReconciliationService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final RevenueCatSubscriberClient subscriberClient;
    private final RevenueCatProperties properties;

    /**
     * Reconciles ACTIVE auto-renewing rows whose end date has passed:
     * extends them when RevenueCat still reports an active entitlement,
     * expires them otherwise.
     */
    @Transactional
    public void reconcileStaleSubscriptions() {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            log.debug("RevenueCatReconciliationService > Skipped: no API key configured");
            return;
        }
        List<UserSubscriptionEntity> stale = userSubscriptionRepository
                .findByStatusAndAutoRenewTrueAndRcAppUserIdNotNullAndEndDateBefore(
                        SubscriptionStatus.ACTIVE, LocalDate.now());
        if (stale.isEmpty()) {
            return;
        }
        log.info("RevenueCatReconciliationService > Reconciling {} stale subscription(s)", stale.size());
        stale.forEach(this::reconcile);
    }

    private void reconcile(UserSubscriptionEntity entity) {
        Optional<OffsetDateTime> expiresAt = subscriberClient.getSubscriber(entity.getRcAppUserId())
                .map(RevenueCatSubscriberResponse::subscriber)
                .map(RevenueCatSubscriberResponse.Subscriber::entitlements)
                .map(entitlements -> entitlements.get(properties.entitlementId()))
                .map(RevenueCatSubscriberResponse.Entitlement::expiresDate);

        if (expiresAt.isPresent() && expiresAt.get().isAfter(OffsetDateTime.now())) {
            entity.setEndDate(expiresAt.get().toLocalDate());
            log.info("RevenueCatReconciliationService > Extended user subscription {} until {}",
                    entity.getUserSubscriptionId(), entity.getEndDate());
        } else {
            entity.setStatus(SubscriptionStatus.EXPIRED);
            entity.setAutoRenew(false);
            log.info("RevenueCatReconciliationService > Expired user subscription {}",
                    entity.getUserSubscriptionId());
        }
        entity.setRcLastEventAt(Instant.now());
        userSubscriptionRepository.save(entity);
    }
}
