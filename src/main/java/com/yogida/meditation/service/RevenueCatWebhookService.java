package com.yogida.meditation.service;

import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.enums.RevenueCatEventType;
import com.yogida.meditation.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Processes RevenueCat webhook events.
 *
 * <p>For an entitlement-affecting event the user's entitlement is re-read from the RevenueCat
 * Subscriber API and written to the durable projection, then connected SSE clients are signalled
 * so they re-query their own customer info. No subscription state is invented locally —
 * RevenueCat remains the source of truth and the projection only mirrors it.
 *
 * <p>Deliveries are deduplicated by RevenueCat's event id, because RevenueCat retries and the
 * endpoint's published contract has always claimed idempotency.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService {

    private final AppUserRepository appUserRepository;
    private final EntitlementService entitlementService;
    private final EntitlementProjectionService projectionService;
    private final SseService sseApi;

    public void processEvent(RevenueCatWebhookRequest request) {
        RevenueCatWebhookRequest.Event event = request == null ? null : request.event();
        if (event == null || event.type() == null) {
            log.warn("RevenueCatWebhookService > Skipping webhook without event type");
            return;
        }
        log.info("RevenueCatWebhookService > Processing event {} of type {}", event.id(), event.type());

        if (!RevenueCatEventType.isEntitlementAffecting(event.type())) {
            log.debug("RevenueCatWebhookService > Ignoring non-entitlement event type: {}", event.type());
            return;
        }

        // RevenueCat retries; without this a single purchase can be handled several times.
        // An event with no id cannot be deduplicated, so it is processed rather than dropped.
        if (event.id() != null && !projectionService.claimEvent(event.id(), event.type(), event.appUserId())) {
            log.info("RevenueCatWebhookService > Event {} already processed; skipping redelivery", event.id());
            return;
        }

        resolveKeycloakUserId(event).ifPresentOrElse(
            userId -> {
                entitlementService.refreshUserEntitlement(userId, event.id());
                sseApi.publishToUser(userId, event.type());
            },
            () -> log.warn("RevenueCatWebhookService > No user found for RC app_user_id on event {}", event.id())
        );
    }

    private Optional<String> resolveKeycloakUserId(RevenueCatWebhookRequest.Event event) {
        return Optional.ofNullable(event.appUserId())
            .flatMap(appUserRepository::findByKeycloakUserId)
            .map(AppUserEntity::getKeycloakUserId)
            .or(() -> Optional.ofNullable(event.originalAppUserId())
                .flatMap(appUserRepository::findByKeycloakUserId)
                .map(AppUserEntity::getKeycloakUserId));
    }
}
