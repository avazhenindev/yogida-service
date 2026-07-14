package com.yogida.meditation.service;

import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.service.api.RevenueCatWebhookApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Processes RevenueCat webhook events by evicting the cached entitlement entry
 * for the affected user. No local subscription state is written to the database.
 * RevenueCat is the authoritative entitlement source; the next {@code isEntitled}
 * call will fetch fresh data from the RC Subscriber API.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService implements RevenueCatWebhookApi {

    /** Event types that signal a change in entitlement status. */
    private static final Set<String> EVICT_EVENT_TYPES = Set.of(
            "INITIAL_PURCHASE", "RENEWAL", "CANCELLATION", "EXPIRATION");

    private final AppUserRepository appUserRepository;
    private final EntitlementService entitlementService;

    @Override
    public void processEvent(RevenueCatWebhookRequest request) {
        RevenueCatWebhookRequest.Event event = request == null ? null : request.event();
        if (event == null || event.type() == null) {
            log.warn("RevenueCatWebhookService > Skipping webhook without event type");
            return;
        }
        if (!EVICT_EVENT_TYPES.contains(event.type())) {
            log.debug("RevenueCatWebhookService > Ignoring non-entitlement event type: {}", event.type());
            return;
        }
        resolveKeycloakUserId(event).ifPresentOrElse(
                userId -> {
                    entitlementService.evictUserEntitlement(userId);
                    log.info("RevenueCatWebhookService > Cache evicted for user {} on event {}",
                            userId, event.type());
                },
                () -> log.warn("RevenueCatWebhookService > No user found for RC app_user_id: {}",
                        event.appUserId())
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
