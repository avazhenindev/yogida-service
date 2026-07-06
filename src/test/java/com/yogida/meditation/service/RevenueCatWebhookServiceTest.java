package com.yogida.meditation.service;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.SubscriptionEntity;
import com.yogida.meditation.entity.SubscriptionPaymentAuditEntity;
import com.yogida.meditation.entity.UserSubscriptionEntity;
import com.yogida.meditation.enums.BillingMode;
import com.yogida.meditation.enums.PaymentAuditStatus;
import com.yogida.meditation.enums.SubscriptionStatus;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.SubscriptionPaymentAuditRepository;
import com.yogida.meditation.repository.SubscriptionRepository;
import com.yogida.meditation.repository.UserSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueCatWebhookServiceTest {

    private static final String KEYCLOAK_ID = "kc-user-1";
    private static final String ENTITLEMENT = "premium";
    private static final String PLAN_NAME = "PREMIUM";

    @Mock
    private AppUserRepository appUserRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;
    @Mock
    private SubscriptionPaymentAuditRepository auditRepository;

    private RevenueCatWebhookService service;

    @BeforeEach
    void setUp() {
        RevenueCatProperties properties =
                new RevenueCatProperties(ENTITLEMENT, PLAN_NAME, "secret", null, null);
        service = new RevenueCatWebhookService(appUserRepository, subscriptionRepository,
                userSubscriptionRepository, auditRepository, properties);
    }

    @Test
    void initialPurchase_createsActiveRecurringProjection() {
        AppUserEntity user = user(7L);
        SubscriptionEntity plan = plan(3L);
        when(auditRepository.existsByRcEventId("evt-1")).thenReturn(false);
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByName(PLAN_NAME)).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findFirstByUserUserIdAndSubscriptionSubscriptionId(7L, 3L))
                .thenReturn(Optional.empty());

        long purchasedAt = Instant.parse("2026-07-01T10:00:00Z").toEpochMilli();
        long expiresAt = Instant.parse("2026-08-01T10:00:00Z").toEpochMilli();
        service.processEvent(request(event("evt-1", "INITIAL_PURCHASE", purchasedAt, expiresAt)));

        ArgumentCaptor<UserSubscriptionEntity> captor = ArgumentCaptor.forClass(UserSubscriptionEntity.class);
        verify(userSubscriptionRepository).save(captor.capture());
        UserSubscriptionEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(saved.getBillingMode()).isEqualTo(BillingMode.RECURRING);
        assertThat(saved.getAutoRenew()).isTrue();
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(saved.getRcAppUserId()).isEqualTo(KEYCLOAK_ID);
        assertAudit(PaymentAuditStatus.PROCESSED);
    }

    @Test
    void nonRenewingPurchase_createsOneTimeProjectionWithPlanPeriodFallback() {
        AppUserEntity user = user(7L);
        SubscriptionEntity plan = plan(3L);
        when(auditRepository.existsByRcEventId("evt-2")).thenReturn(false);
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByName(PLAN_NAME)).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findFirstByUserUserIdAndSubscriptionSubscriptionId(7L, 3L))
                .thenReturn(Optional.empty());

        long purchasedAt = Instant.parse("2026-07-01T10:00:00Z").toEpochMilli();
        service.processEvent(request(event("evt-2", "NON_RENEWING_PURCHASE", purchasedAt, null)));

        ArgumentCaptor<UserSubscriptionEntity> captor = ArgumentCaptor.forClass(UserSubscriptionEntity.class);
        verify(userSubscriptionRepository).save(captor.capture());
        UserSubscriptionEntity saved = captor.getValue();
        assertThat(saved.getBillingMode()).isEqualTo(BillingMode.ONE_TIME);
        assertThat(saved.getAutoRenew()).isFalse();
        assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 31));
    }

    @Test
    void duplicateEvent_isSkipped() {
        when(auditRepository.existsByRcEventId("evt-3")).thenReturn(true);

        service.processEvent(request(event("evt-3", "INITIAL_PURCHASE", null, null)));

        verify(userSubscriptionRepository, never()).save(any());
        verify(auditRepository, never()).save(any());
    }

    @Test
    void unknownUser_isAuditedAsFailed() {
        when(auditRepository.existsByRcEventId("evt-4")).thenReturn(false);
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.empty());

        service.processEvent(request(event("evt-4", "RENEWAL", null, null)));

        verify(userSubscriptionRepository, never()).save(any());
        assertAudit(PaymentAuditStatus.FAILED);
    }

    @Test
    void cancellation_disablesAutoRenewOnly() {
        AppUserEntity user = user(7L);
        SubscriptionEntity plan = plan(3L);
        UserSubscriptionEntity existing = existingSubscription(user, plan);
        when(auditRepository.existsByRcEventId("evt-5")).thenReturn(false);
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByName(PLAN_NAME)).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findFirstByUserUserIdAndSubscriptionSubscriptionId(7L, 3L))
                .thenReturn(Optional.of(existing));

        service.processEvent(request(event("evt-5", "CANCELLATION", null, null)));

        assertThat(existing.getAutoRenew()).isFalse();
        assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertAudit(PaymentAuditStatus.PROCESSED);
    }

    @Test
    void expiration_expiresProjection() {
        AppUserEntity user = user(7L);
        SubscriptionEntity plan = plan(3L);
        UserSubscriptionEntity existing = existingSubscription(user, plan);
        when(auditRepository.existsByRcEventId("evt-6")).thenReturn(false);
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByName(PLAN_NAME)).thenReturn(Optional.of(plan));
        when(userSubscriptionRepository.findFirstByUserUserIdAndSubscriptionSubscriptionId(7L, 3L))
                .thenReturn(Optional.of(existing));

        service.processEvent(request(event("evt-6", "EXPIRATION", null, null)));

        assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(existing.getAutoRenew()).isFalse();
        assertAudit(PaymentAuditStatus.PROCESSED);
    }

    @Test
    void unknownEventType_isAuditedAsIgnored() {
        when(auditRepository.existsByRcEventId("evt-7")).thenReturn(false);

        service.processEvent(request(event("evt-7", "TRANSFER", null, null)));

        verify(userSubscriptionRepository, never()).save(any());
        assertAudit(PaymentAuditStatus.IGNORED);
    }

    @Test
    void eventForOtherEntitlement_isAuditedAsIgnored() {
        when(auditRepository.existsByRcEventId("evt-8")).thenReturn(false);

        RevenueCatWebhookRequest.Event event = new RevenueCatWebhookRequest.Event(
                "evt-8", "INITIAL_PURCHASE", KEYCLOAK_ID, null, "prod_month", "APP_STORE",
                "PRODUCTION", List.of("other-entitlement"), null, null);
        service.processEvent(request(event));

        verify(userSubscriptionRepository, never()).save(any());
        assertAudit(PaymentAuditStatus.IGNORED);
    }

    private void assertAudit(PaymentAuditStatus expected) {
        ArgumentCaptor<SubscriptionPaymentAuditEntity> captor =
                ArgumentCaptor.forClass(SubscriptionPaymentAuditEntity.class);
        verify(auditRepository).save(captor.capture());
        assertThat(captor.getValue().getProcessingStatus()).isEqualTo(expected);
    }

    private static RevenueCatWebhookRequest request(RevenueCatWebhookRequest.Event event) {
        return new RevenueCatWebhookRequest(event, "1.0");
    }

    private static RevenueCatWebhookRequest.Event event(String id, String type, Long purchasedAtMs, Long expirationAtMs) {
        return new RevenueCatWebhookRequest.Event(id, type, KEYCLOAK_ID, null, "prod_month",
                "APP_STORE", "PRODUCTION", List.of(ENTITLEMENT), purchasedAtMs, expirationAtMs);
    }

    private static AppUserEntity user(Long id) {
        AppUserEntity user = new AppUserEntity();
        user.setUserId(id);
        user.setKeycloakUserId(KEYCLOAK_ID);
        return user;
    }

    private static SubscriptionEntity plan(Long id) {
        SubscriptionEntity plan = new SubscriptionEntity();
        plan.setSubscriptionId(id);
        plan.setName(PLAN_NAME);
        plan.setStatus(SubscriptionStatus.ACTIVE);
        plan.setPeriodDays(30);
        return plan;
    }

    private static UserSubscriptionEntity existingSubscription(AppUserEntity user, SubscriptionEntity plan) {
        UserSubscriptionEntity entity = new UserSubscriptionEntity();
        entity.setUserSubscriptionId(11L);
        entity.setUser(user);
        entity.setSubscription(plan);
        entity.setStatus(SubscriptionStatus.ACTIVE);
        entity.setStartDate(LocalDate.of(2026, 6, 1));
        entity.setEndDate(LocalDate.of(2026, 8, 1));
        entity.setAutoRenew(true);
        return entity;
    }
}
