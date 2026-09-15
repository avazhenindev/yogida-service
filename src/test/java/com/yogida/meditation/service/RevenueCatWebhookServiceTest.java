package com.yogida.meditation.service;

import com.yogida.meditation.config.EntitlementEventProperties;
import com.yogida.meditation.dto.EntitlementEventMessage;
import com.yogida.meditation.dto.EntitlementEventMessage.TransferDirection;
import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevenueCatWebhookServiceTest {

    private static final String KEYCLOAK_ID = "kc-user-1";
    private static final String EVENT_ID = "evt-1";
    private static final String OTHER_KEYCLOAK_ID = "kc-user-2";

    @Mock private AppUserRepository appUserRepository;
    @Mock private EntitlementService entitlementService;
    @Mock private EntitlementProjectionService projectionService;
    @Mock private SseService sseApi;

    private RevenueCatWebhookService service;

    @BeforeEach
    void setUp() {
        service = service(new EntitlementEventProperties(true, true));
    }

    @Test
    void initialPurchase_refreshesEntitlementAndSignalsClients() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("INITIAL_PURCHASE"));

        // Refresh first, so the refetch the frame prompts already sees the new locks.
        InOrder order = inOrder(entitlementService, sseApi);
        order.verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        order.verify(sseApi).publishToUser(eq(KEYCLOAK_ID), any());
        EntitlementEventMessage sent = publishedTo(KEYCLOAK_ID);
        assertThat(sent.type()).isEqualTo("INITIAL_PURCHASE");
        assertThat(sent.eventId()).isEqualTo(EVENT_ID);
        assertThat(sent.bannerAllowed()).isTrue();
        assertThat(sent.transferDirection()).isNull();
    }

    @Test
    void renewal_refreshesEntitlementAndSignalsClients() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("RENEWAL"));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("RENEWAL");
    }

    @Test
    void expiration_refreshesEntitlementAndSignalsClients() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("EXPIRATION"));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("EXPIRATION");
    }

    /**
     * A refund arrives as a CANCELLATION and projects "not entitled". Its reversal has to refresh
     * too, or the server keeps premium locked until the projection goes stale.
     */
    @Test
    void refundReversed_refreshesAndForwards() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("REFUND_REVERSED"));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("REFUND_REVERSED");
    }

    /**
     * Two events for one user in the same second race on the projection insert. The losing
     * refresh must not take the push down with it, nor leave the cache serving the frame's
     * refetch the state from before the event.
     */
    @Test
    void refreshThrows_stillForwards() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);

        service.processEvent(request("RENEWAL"));

        verify(entitlementService, times(2)).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        verify(entitlementService).evictUserEntitlement(KEYCLOAK_ID);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("RENEWAL");
    }

    /** The race's loser retries: the winner's row exists by then, so the second pass is an update. */
    @Test
    void refreshThrowsOnce_isRetriedBeforeForwarding() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .doNothing()
                .when(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);

        service.processEvent(request("INITIAL_PURCHASE"));

        InOrder order = inOrder(entitlementService, sseApi);
        order.verify(entitlementService, times(2)).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        order.verify(sseApi).publishToUser(eq(KEYCLOAK_ID), any());
        verify(entitlementService, never()).evictUserEntitlement(anyString());
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("INITIAL_PURCHASE");
    }

    @Test
    void cancellation_carriesCancelReason() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request(event("CANCELLATION")
                .cancelReason("BILLING_ERROR")
                .expirationAtMs(1_800_000_000_000L)));

        EntitlementEventMessage sent = publishedTo(KEYCLOAK_ID);
        assertThat(sent.cancelReason()).isEqualTo("BILLING_ERROR");
        assertThat(sent.expirationAtMs()).isEqualTo(1_800_000_000_000L);
    }

    @Test
    void productChange_carriesNewProductId() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request(event("PRODUCT_CHANGE").newProductId("prod_year")));

        EntitlementEventMessage sent = publishedTo(KEYCLOAK_ID);
        assertThat(sent.productId()).isEqualTo("prod_month");
        assertThat(sent.newProductId()).isEqualTo("prod_year");
    }

    @Test
    void sandbox_bannerNotAllowed_whenSandboxDisabled() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service(new EntitlementEventProperties(true, false))
                .processEvent(request(event("INITIAL_PURCHASE").environment("SANDBOX")));

        EntitlementEventMessage sent = publishedTo(KEYCLOAK_ID);
        assertThat(sent.bannerAllowed()).isFalse();
        assertThat(sent.environment()).isEqualTo("SANDBOX");
    }

    @Test
    void bannerDisabledGlobally() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service(new EntitlementEventProperties(false, true)).processEvent(request("INITIAL_PURCHASE"));

        assertThat(publishedTo(KEYCLOAK_ID).bannerAllowed()).isFalse();
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

        service.processEvent(request(event("RENEWAL").id(null)));

        verify(projectionService, never()).claimEvent(any(), any(), any());
        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, null);
    }

    // --- forwarded without a refresh ----------------------------------------
    //
    // Types outside the entitlement-affecting set, and types this service has never heard of,
    // still reach the app. They just do not re-read the projection first.

    @Test
    void testEvent_isForwardedWithoutRefresh() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("TEST"));

        verifyNoInteractions(entitlementService);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("TEST");
    }

    @Test
    void experimentEnrollment_isForwardedWithoutRefresh() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("EXPERIMENT_ENROLLMENT"));

        verifyNoInteractions(entitlementService);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("EXPERIMENT_ENROLLMENT");
    }

    @Test
    void unknownEventType_isForwardedWithoutRefresh() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request("FUTURE_UNKNOWN_TYPE"));

        verifyNoInteractions(entitlementService);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("FUTURE_UNKNOWN_TYPE");
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
        service.processEvent(request(event(null)));

        verifyNoInteractions(appUserRepository, entitlementService, projectionService, sseApi);
    }

    // --- transfers -------------------------------------------------------
    //
    // TRANSFER carries neither app_user_id nor original_app_user_id. Both sides changed: one
    // subscriber lost the entitlements and the other gained them.

    @Test
    void transfer_refreshesBothSidesOfTheTransfer() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));
        when(appUserRepository.findByKeycloakUserId(OTHER_KEYCLOAK_ID))
                .thenReturn(Optional.of(user(OTHER_KEYCLOAK_ID)));

        service.processEvent(transfer(List.of(OTHER_KEYCLOAK_ID), List.of(KEYCLOAK_ID)));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        verify(entitlementService).refreshUserEntitlement(OTHER_KEYCLOAK_ID, EVENT_ID);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("TRANSFER");
        assertThat(publishedTo(OTHER_KEYCLOAK_ID).type()).isEqualTo("TRANSFER");
    }

    @Test
    void transfer_setsDirectionPerRecipient() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));
        when(appUserRepository.findByKeycloakUserId(OTHER_KEYCLOAK_ID))
                .thenReturn(Optional.of(user(OTHER_KEYCLOAK_ID)));

        service.processEvent(transfer(List.of(OTHER_KEYCLOAK_ID), List.of(KEYCLOAK_ID)));

        assertThat(publishedTo(KEYCLOAK_ID).transferDirection()).isEqualTo(TransferDirection.IN);
        assertThat(publishedTo(OTHER_KEYCLOAK_ID).transferDirection()).isEqualTo(TransferDirection.OUT);
    }

    /** The destination is handled first; its failed refresh must not cost either side its push. */
    @Test
    void transfer_refreshFailureOnOneSide_stillForwardsBoth() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));
        when(appUserRepository.findByKeycloakUserId(OTHER_KEYCLOAK_ID))
                .thenReturn(Optional.of(user(OTHER_KEYCLOAK_ID)));
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);

        service.processEvent(transfer(List.of(OTHER_KEYCLOAK_ID), List.of(KEYCLOAK_ID)));

        verify(entitlementService).refreshUserEntitlement(OTHER_KEYCLOAK_ID, EVENT_ID);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("TRANSFER");
        assertThat(publishedTo(OTHER_KEYCLOAK_ID).type()).isEqualTo("TRANSFER");
    }

    /** The destination is handled first; its failed publish must not cost the source anything. */
    @Test
    void transfer_publishFailureOnOneSide_stillForwardsOther() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));
        when(appUserRepository.findByKeycloakUserId(OTHER_KEYCLOAK_ID))
                .thenReturn(Optional.of(user(OTHER_KEYCLOAK_ID)));
        doThrow(new IllegalStateException("boom"))
                .when(sseApi).publishToUser(eq(KEYCLOAK_ID), any());

        service.processEvent(transfer(List.of(OTHER_KEYCLOAK_ID), List.of(KEYCLOAK_ID)));

        verify(entitlementService).refreshUserEntitlement(OTHER_KEYCLOAK_ID, EVENT_ID);
        assertThat(publishedTo(OTHER_KEYCLOAK_ID).transferDirection()).isEqualTo(TransferDirection.OUT);
    }

    /** A transfer out of an anonymous subscriber names an id that was never an app user. */
    @Test
    void transfer_ignoresIdsThatAreNotAppUsers() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId("$RCAnonymousID:abc")).thenReturn(Optional.empty());
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(transfer(List.of("$RCAnonymousID:abc"), List.of(KEYCLOAK_ID)));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        verify(entitlementService, never()).refreshUserEntitlement("$RCAnonymousID:abc", EVENT_ID);
    }

    @Test
    void transfer_withNoKnownUsers_skipsRefreshAndSse() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(anyString())).thenReturn(Optional.empty());

        service.processEvent(transfer(List.of("$RCAnonymousID:abc"), List.of("$RCAnonymousID:def")));

        verifyNoInteractions(entitlementService, sseApi);
    }

    /** The same user on both sides is refreshed once, not twice, and is told they received. */
    @Test
    void transfer_betweenTheSameIdRefreshesOnce() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(transfer(List.of(KEYCLOAK_ID), List.of(KEYCLOAK_ID)));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        EntitlementEventMessage sent = publishedTo(KEYCLOAK_ID);
        assertThat(sent.type()).isEqualTo("TRANSFER");
        assertThat(sent.transferDirection()).isEqualTo(TransferDirection.IN);
    }

    /** RevenueCat's docs say to search aliases as well as original_app_user_id. */
    @Test
    void subscriberFoundOnlyByAlias_isResolved() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId("rc-only-id")).thenReturn(Optional.empty());
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request(event("RENEWAL")
                .appUserId("rc-only-id")
                .originalAppUserId("rc-only-id")
                .aliases(List.of(KEYCLOAK_ID))));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
    }

    /** One subscriber under several ids is still one refresh, not one per alias. */
    @Test
    void subscriberWithAliases_isRefreshedOnce() {
        givenClaimSucceeds();
        when(appUserRepository.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user()));

        service.processEvent(request(event("RENEWAL").aliases(List.of(KEYCLOAK_ID))));

        verify(entitlementService).refreshUserEntitlement(KEYCLOAK_ID, EVENT_ID);
        assertThat(publishedTo(KEYCLOAK_ID).type()).isEqualTo("RENEWAL");
    }

    private RevenueCatWebhookService service(EntitlementEventProperties eventProperties) {
        return new RevenueCatWebhookService(
                appUserRepository, entitlementService, projectionService, sseApi, eventProperties);
    }

    /** The one frame published to a user, failing unless exactly one was. */
    private EntitlementEventMessage publishedTo(String keycloakUserId) {
        ArgumentCaptor<EntitlementEventMessage> message = ArgumentCaptor.forClass(EntitlementEventMessage.class);
        verify(sseApi).publishToUser(eq(keycloakUserId), message.capture());
        return message.getValue();
    }

    private RevenueCatWebhookRequest transfer(List<String> from, List<String> to) {
        return request(RevenueCatWebhookRequest.Event.builder()
                .id(EVENT_ID)
                .type("TRANSFER")
                .transferredFrom(from)
                .transferredTo(to)
                .store("APP_STORE")
                .environment("PRODUCTION"));
    }

    private void givenClaimSucceeds() {
        when(projectionService.claimEvent(anyString(), anyString(), anyString())).thenReturn(true);
    }

    private RevenueCatWebhookRequest request(String type) {
        return request(event(type));
    }

    private RevenueCatWebhookRequest request(RevenueCatWebhookRequest.Event.EventBuilder event) {
        return new RevenueCatWebhookRequest(event.build(), "1.0");
    }

    /** A subscriber event about {@link #KEYCLOAK_ID}, for tests to adjust. */
    private RevenueCatWebhookRequest.Event.EventBuilder event(String type) {
        return RevenueCatWebhookRequest.Event.builder()
                .id(EVENT_ID)
                .type(type)
                .appUserId(KEYCLOAK_ID)
                .originalAppUserId(KEYCLOAK_ID)
                .productId("prod_month")
                .store("APP_STORE")
                .environment("PRODUCTION")
                .entitlementIds(List.of("premium"));
    }

    private AppUserEntity user() {
        return user(KEYCLOAK_ID);
    }

    private AppUserEntity user(String keycloakUserId) {
        AppUserEntity u = new AppUserEntity();
        u.setKeycloakUserId(keycloakUserId);
        return u;
    }
}
