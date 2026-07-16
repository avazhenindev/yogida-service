package com.yogida.meditation.service;

import com.yogida.meditation.dto.RevenueCatSubscriberResponse;
import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.enums.SseMessageType;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.service.api.SseApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueCatWebhookServiceTest {

    private static final String KEYCLOAK_ID = "kc-user-1";

    @Mock private AppUserRepository appUserRepository;
    @Mock private EntitlementService entitlementService;
    @Mock private RevenueCatSubscriberClient subscriberClient;
    @Mock private SseApi sseApi;

    private RevenueCatWebhookService service;

    @BeforeEach
    void setUp() {
        service = new RevenueCatWebhookService(appUserRepository, entitlementService, subscriberClient, sseApi);
    }

    @Test
    void initialPurchase_evictsCacheAndPushesSSE() {
        AppUserEntity user = user();
        RevenueCatSubscriberResponse customerInfo = mock(RevenueCatSubscriberResponse.class);
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(subscriberClient.getSubscriber(KEYCLOAK_ID)).thenReturn(Optional.of(customerInfo));

        service.processEvent(request("INITIAL_PURCHASE"));

        verify(entitlementService).evictUserEntitlement(KEYCLOAK_ID);
        verify(sseApi).publishToUser(KEYCLOAK_ID, SseMessageType.RC, customerInfo);
    }

    @Test
    void renewal_evictsCacheAndPushesSSE() {
        AppUserEntity user = user();
        RevenueCatSubscriberResponse customerInfo = mock(RevenueCatSubscriberResponse.class);
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(subscriberClient.getSubscriber(KEYCLOAK_ID)).thenReturn(Optional.of(customerInfo));

        service.processEvent(request("RENEWAL"));

        verify(entitlementService).evictUserEntitlement(KEYCLOAK_ID);
        verify(sseApi).publishToUser(KEYCLOAK_ID, SseMessageType.RC, customerInfo);
    }

    @Test
    void expiration_evictsCacheAndPushesSSE() {
        AppUserEntity user = user();
        RevenueCatSubscriberResponse customerInfo = mock(RevenueCatSubscriberResponse.class);
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(subscriberClient.getSubscriber(KEYCLOAK_ID)).thenReturn(Optional.of(customerInfo));

        service.processEvent(request("EXPIRATION"));

        verify(entitlementService).evictUserEntitlement(KEYCLOAK_ID);
        verify(sseApi).publishToUser(KEYCLOAK_ID, SseMessageType.RC, customerInfo);
    }

    @Test
    void testEvent_isIgnored() {
        service.processEvent(request("TEST"));

        verifyNoInteractions(appUserRepository, entitlementService, subscriberClient, sseApi);
    }

    @Test
    void experimentEnrollment_isIgnored() {
        service.processEvent(request("EXPERIMENT_ENROLLMENT"));

        verifyNoInteractions(appUserRepository, entitlementService, subscriberClient, sseApi);
    }

    @Test
    void unknownEventType_isIgnoredGracefully() {
        service.processEvent(request("FUTURE_UNKNOWN_TYPE"));

        verifyNoInteractions(appUserRepository, entitlementService, subscriberClient, sseApi);
    }

    @Test
    void noUserFound_skipsCacheEvictAndSSE() {
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.empty());

        service.processEvent(request("CANCELLATION"));

        verifyNoInteractions(entitlementService, sseApi);
    }

    @Test
    void subscriberClientReturnsEmpty_cacheStillEvicted_sseSkipped() {
        AppUserEntity user = user();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(subscriberClient.getSubscriber(KEYCLOAK_ID)).thenReturn(Optional.empty());

        service.processEvent(request("CANCELLATION"));

        verify(entitlementService).evictUserEntitlement(KEYCLOAK_ID);
        verifyNoInteractions(sseApi);
    }

    @Test
    void nullRequest_isSkipped() {
        service.processEvent(null);

        verifyNoInteractions(appUserRepository, entitlementService, subscriberClient, sseApi);
    }

    @Test
    void nullEventType_isSkipped() {
        RevenueCatWebhookRequest.Event event = new RevenueCatWebhookRequest.Event(
                "evt-1", null, KEYCLOAK_ID, KEYCLOAK_ID, null, null, null, null, null, null);
        service.processEvent(new RevenueCatWebhookRequest(event, "1.0"));

        verifyNoInteractions(appUserRepository, entitlementService, subscriberClient, sseApi);
    }

    private RevenueCatWebhookRequest request(String type) {
        RevenueCatWebhookRequest.Event event = new RevenueCatWebhookRequest.Event(
                "evt-1", type, KEYCLOAK_ID, KEYCLOAK_ID, "prod_month",
                "APP_STORE", "PRODUCTION", List.of("premium"), null, null);
        return new RevenueCatWebhookRequest(event, "1.0");
    }

    private AppUserEntity user() {
        AppUserEntity u = new AppUserEntity();
        u.setKeycloakUserId(KEYCLOAK_ID);
        return u;
    }
}
