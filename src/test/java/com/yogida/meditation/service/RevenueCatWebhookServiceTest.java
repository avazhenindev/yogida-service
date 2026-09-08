package com.yogida.meditation.service;

import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueCatWebhookServiceTest {

    private static final String KEYCLOAK_ID = "kc-user-1";
    private static final String EVENT_ID = "evt-1";

    @Mock private AppUserRepository appUserRepository;
    @Mock private EntitlementService entitlementService;
    @Mock private EntitlementProjectionService projectionService;
    @Mock private SseService sseApi;

    private RevenueCatWebhookService service;

    @BeforeEach
    void setUp() {
        service = new RevenueCatWebhookService(
                appUserRepository, entitlementService, projectionService, sseApi);
    }

    @Test
    void initialPurchase_refreshesEntitlementAndSignalsClients() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("INITIAL_PURCHASE"));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        verify(sseApi).publishToUser(KEYCLOAK_ID, "INITIAL_PURCHASE");
    }

    @Test
    void renewal_refreshesEntitlementAndSignalsClients() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("RENEWAL"));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        verify(sseApi).publishToUser(KEYCLOAK_ID, "RENEWAL");
    }

    @Test
    void expiration_refreshesEntitlementAndSignalsClients() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("EXPIRATION"));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        verify(sseApi).publishToUser(KEYCLOAK_ID, "EXPIRATION");
    }

    /**
     * RevenueCat retries deliveries. A redelivery must not refresh or notify a second time —
     * the endpoint's published contract has always claimed this and never implemented it.
     */
    @Test
    void redeliveredEvent_isSkipped() {
        when(projectionService.claimEvent(EVENT_ID, "RENEWAL", KEYCLOAK_ID)).thenReturn(false);

        service.processEvent(request("RENEWAL"));

        verifyNoInteractions(appUserRepository, entitlementService, sseApi);
    }

    /** An event with no id cannot be deduplicated, so it is processed rather than dropped. */
    @Test
    void eventWithoutId_isProcessedWithoutClaiming() {
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));
        RevenueCatWebhookRequest.Event event = new RevenueCatWebhookRequest.Event(
                null, "RENEWAL", KEYCLOAK_ID, KEYCLOAK_ID, "prod_month",
                "APP_STORE", "PRODUCTION", List.of("premium"), null, null);

        service.processEvent(new RevenueCatWebhookRequest(event, "1.0"));

        verify(projectionService, never()).claimEvent(any(), any(), any());
        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, null);
    }

    @Test
    void testEvent_isIgnored() {
        service.processEvent(request("TEST"));

        verifyNoInteractions(appUserRepository, entitlementService, projectionService, sseApi);
    }

    @Test
    void experimentEnrollment_isIgnored() {
        service.processEvent(request("EXPERIMENT_ENROLLMENT"));

        verifyNoInteractions(appUserRepository, entitlementService, projectionService, sseApi);
    }

    @Test
    void unknownEventType_isIgnoredGracefully() {
        service.processEvent(request("FUTURE_UNKNOWN_TYPE"));

        verifyNoInteractions(appUserRepository, entitlementService, projectionService, sseApi);
    }

    @Test
    void noUserFound_skipsRefreshAndSse() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.empty());

        service.processEvent(request("CANCELLATION"));

        verifyNoInteractions(entitlementService, sseApi);
    }

    @Test
    void nullRequest_isSkipped() {
        service.processEvent(null);

        verifyNoInteractions(appUserRepository, entitlementService, projectionService, sseApi);
    }

    @Test
    void nullEventType_isSkipped() {
        RevenueCatWebhookRequest.Event event = new RevenueCatWebhookRequest.Event(
                EVENT_ID, null, KEYCLOAK_ID, KEYCLOAK_ID, null, null, null, null, null, null);

        service.processEvent(new RevenueCatWebhookRequest(event, "1.0"));

        verifyNoInteractions(appUserRepository, entitlementService, projectionService, sseApi);
    }

    private void givenClaimSucceeds() {
        when(projectionService.claimEvent(anyString(), anyString(), anyString())).thenReturn(true);
    }

    private RevenueCatWebhookRequest request(String type) {
        RevenueCatWebhookRequest.Event event = new RevenueCatWebhookRequest.Event(
                EVENT_ID, type, KEYCLOAK_ID, KEYCLOAK_ID, "prod_month",
                "APP_STORE", "PRODUCTION", List.of("premium"), null, null);
        return new RevenueCatWebhookRequest(event, "1.0");
    }

    private AppUserEntity user() {
        AppUserEntity u = new AppUserEntity();
        u.setKeycloakUserId(KEYCLOAK_ID);
        return u;
    }
}
