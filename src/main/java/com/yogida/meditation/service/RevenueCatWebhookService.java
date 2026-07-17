package com.yogida.meditation.service;

import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.enums.RevenueCatEventType;
import com.yogida.meditation.enums.SseMessageType;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.service.api.RevenueCatWebhookApi;
import com.yogida.meditation.service.api.SseApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Processes RevenueCat webhook events by evicting the cached entitlement entry
 * for the affected user and pushing fresh customer info to any connected SSE clients.
 * No local subscription state is written to the database.
 * RevenueCat is the authoritative entitlement source.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService implements RevenueCatWebhookApi {

    private final AppUserRepository appUserRepository;
    private final EntitlementService entitlementService;
    private final RevenueCatSubscriberClient subscriberClient;
    private final SseApi sseApi;

    @Override
    public void processEvent(RevenueCatWebhookRequest request) {
        log.info("RevenueCatWebhookService > Received webhook event: {}", request);
        RevenueCatWebhookRequest.Event event = request == null ? null : request.event();
        if (event == null || event.type() == null) {
            log.warn("RevenueCatWebhookService > Skipping webhook without event type");
            return;
        }
        if (!RevenueCatEventType.isEntitlementAffecting(event.type())) {
            log.debug("RevenueCatWebhookService > Ignoring non-entitlement event type: {}", event.type());
            return;
        }
        resolveKeycloakUserId(event).ifPresentOrElse(
            userId -> {
                entitlementService.evictUserEntitlement(userId);
                publishEntitlementUpdate(userId, event);
            },
            () -> log.warn("RevenueCatWebhookService > No user found for RC app_user_id: {}",
                event.appUserId())
        );
    }

    /**
     * Fetches fresh customer info from RevenueCat and pushes it to any connected SSE clients.
     * If the RC API call fails (returns empty), the SSE push is skipped gracefully.
     */
    private void publishEntitlementUpdate(String userId, RevenueCatWebhookRequest.Event event) {
        log.info("RevenueCatWebhookService > Publishing entitlement update to SSE for user {}", userId);
        sseApi.publishToUser(userId, SseMessageType.TEST.name(), event);
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

