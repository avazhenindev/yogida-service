package com.yogida.meditation.service;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.SubscriptionEntity;
import com.yogida.meditation.entity.SubscriptionPaymentAuditEntity;
import com.yogida.meditation.entity.UserSubscriptionEntity;
import com.yogida.meditation.enums.BillingMode;
import com.yogida.meditation.enums.PaymentAuditStatus;
import com.yogida.meditation.enums.RevenueCatEventType;
import com.yogida.meditation.enums.SubscriptionStatus;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.SubscriptionPaymentAuditRepository;
import com.yogida.meditation.repository.SubscriptionRepository;
import com.yogida.meditation.repository.UserSubscriptionRepository;
import com.yogida.meditation.service.api.RevenueCatWebhookApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Idempotent projection of RevenueCat webhook events onto local user_subscription rows.
 * RevenueCat is the payment source of truth; this service only maintains the local
 * entitlement projection used by EntitlementService/SecureStreamService.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService implements RevenueCatWebhookApi {

    private final AppUserRepository appUserRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPaymentAuditRepository auditRepository;
    private final RevenueCatProperties properties;

    @Override
    @Transactional
    public void processEvent(RevenueCatWebhookRequest request) {
        RevenueCatWebhookRequest.Event event = request == null ? null : request.event();
        if (event == null || event.id() == null || event.id().isBlank()) {
            log.warn("RevenueCatWebhookService > Skipping webhook without event id");
            return;
        }
        if (auditRepository.existsByRcEventId(event.id())) {
            log.info("RevenueCatWebhookService > Duplicate event ignored: {}", event.id());
            return;
        }

        RevenueCatEventType type = RevenueCatEventType.fromString(event.type());
        try {
            processByType(type, event, request);
        } catch (RuntimeException e) {
            log.error("RevenueCatWebhookService > Failed to process event {}: {}", event.id(), e.getMessage());
            audit(event, request, PaymentAuditStatus.FAILED, e.getMessage());
        }
    }

    private void processByType(RevenueCatEventType type, RevenueCatWebhookRequest.Event event,
                               RevenueCatWebhookRequest request) {
        if (!grantsConfiguredEntitlement(event)) {
            audit(event, request, PaymentAuditStatus.IGNORED,
                    "Event does not reference entitlement '" + properties.entitlementId() + "'");
            return;
        }
        switch (type) {
            case INITIAL_PURCHASE, RENEWAL, UNCANCELLATION, PRODUCT_CHANGE ->
                    grant(event, request, BillingMode.RECURRING);
            case NON_RENEWING_PURCHASE -> grant(event, request, BillingMode.ONE_TIME);
            case CANCELLATION -> revokeRenewal(event, request);
            case EXPIRATION -> expire(event, request);
            case BILLING_ISSUE, UNKNOWN -> audit(event, request, PaymentAuditStatus.IGNORED,
                    "Event type not projected: " + event.type());
        }
    }

    /** Purchase-type events without entitlement ids are still projected (defensive default). */
    private boolean grantsConfiguredEntitlement(RevenueCatWebhookRequest.Event event) {
        return event.entitlementIds() == null
                || event.entitlementIds().isEmpty()
                || event.entitlementIds().contains(properties.entitlementId());
    }

    private void grant(RevenueCatWebhookRequest.Event event, RevenueCatWebhookRequest request,
                       BillingMode billingMode) {
        Optional<AppUserEntity> user = resolveUser(event);
        if (user.isEmpty()) {
            audit(event, request, PaymentAuditStatus.FAILED,
                    "No app user found for RevenueCat app_user_id: " + event.appUserId());
            return;
        }
        SubscriptionEntity plan = subscriptionRepository.findByName(properties.premiumPlanName())
                .orElse(null);
        if (plan == null) {
            audit(event, request, PaymentAuditStatus.FAILED,
                    "Premium plan not found: " + properties.premiumPlanName());
            return;
        }

        UserSubscriptionEntity entity = userSubscriptionRepository
                .findFirstByUserUserIdAndSubscriptionSubscriptionId(user.get().getUserId(), plan.getSubscriptionId())
                .orElseGet(() -> {
                    UserSubscriptionEntity created = new UserSubscriptionEntity();
                    created.setUser(user.get());
                    created.setSubscription(plan);
                    return created;
                });

        LocalDate startDate = toLocalDate(event.purchasedAtMs()).orElse(LocalDate.now());
        LocalDate endDate = toLocalDate(event.expirationAtMs())
                .orElse(startDate.plusDays(plan.getPeriodDays()));

        entity.setStatus(SubscriptionStatus.ACTIVE);
        if (entity.getStartDate() == null) {
            entity.setStartDate(startDate);
        }
        entity.setEndDate(endDate);
        entity.setAutoRenew(billingMode == BillingMode.RECURRING);
        entity.setBillingMode(billingMode);
        entity.setRcAppUserId(event.appUserId());
        entity.setRcProductId(event.productId());
        entity.setRcStore(event.store());
        entity.setRcLastEventAt(Instant.now());
        userSubscriptionRepository.save(entity);

        log.info("RevenueCatWebhookService > Granted {} entitlement to user {} until {}",
                billingMode, user.get().getUserId(), endDate);
        audit(event, request, PaymentAuditStatus.PROCESSED, null);
    }

    /** Cancellation keeps access until the paid period ends; only renewal expectation changes. */
    private void revokeRenewal(RevenueCatWebhookRequest.Event event, RevenueCatWebhookRequest request) {
        updateExistingProjection(event, request, entity -> {
            entity.setAutoRenew(false);
            entity.setRcLastEventAt(Instant.now());
        }, "cancellation");
    }

    private void expire(RevenueCatWebhookRequest.Event event, RevenueCatWebhookRequest request) {
        updateExistingProjection(event, request, entity -> {
            entity.setStatus(SubscriptionStatus.EXPIRED);
            entity.setAutoRenew(false);
            toLocalDate(event.expirationAtMs()).ifPresent(entity::setEndDate);
            entity.setRcLastEventAt(Instant.now());
        }, "expiration");
    }

    private void updateExistingProjection(RevenueCatWebhookRequest.Event event, RevenueCatWebhookRequest request,
                                          java.util.function.Consumer<UserSubscriptionEntity> mutation, String action) {
        Optional<AppUserEntity> user = resolveUser(event);
        Optional<SubscriptionEntity> plan = subscriptionRepository.findByName(properties.premiumPlanName());
        Optional<UserSubscriptionEntity> existing = user.flatMap(u -> plan.flatMap(p ->
                userSubscriptionRepository.findFirstByUserUserIdAndSubscriptionSubscriptionId(
                        u.getUserId(), p.getSubscriptionId())));
        if (existing.isEmpty()) {
            audit(event, request, PaymentAuditStatus.IGNORED,
                    "No local projection found for " + action + " of app_user_id: " + event.appUserId());
            return;
        }
        mutation.accept(existing.get());
        userSubscriptionRepository.save(existing.get());
        log.info("RevenueCatWebhookService > Applied {} for user subscription {}",
                action, existing.get().getUserSubscriptionId());
        audit(event, request, PaymentAuditStatus.PROCESSED, null);
    }

    private Optional<AppUserEntity> resolveUser(RevenueCatWebhookRequest.Event event) {
        return Optional.ofNullable(event.appUserId())
                .flatMap(appUserRepository::findByKeycloakUserId)
                .or(() -> Optional.ofNullable(event.originalAppUserId())
                        .flatMap(appUserRepository::findByKeycloakUserId));
    }

    private void audit(RevenueCatWebhookRequest.Event event, RevenueCatWebhookRequest request,
                       PaymentAuditStatus status, String detail) {
        SubscriptionPaymentAuditEntity audit = new SubscriptionPaymentAuditEntity();
        audit.setRcEventId(event.id());
        audit.setEventType(event.type());
        audit.setRcAppUserId(event.appUserId());
        audit.setRcProductId(event.productId());
        audit.setRcStore(event.store());
        audit.setProcessingStatus(status);
        audit.setDetail(detail);
        audit.setRawPayload(String.valueOf(request));
        audit.setProcessedAt(Instant.now());
        auditRepository.save(audit);
    }

    private Optional<LocalDate> toLocalDate(Long epochMs) {
        return Optional.ofNullable(epochMs)
                .map(ms -> Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate());
    }
}
