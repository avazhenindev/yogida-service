package com.yogida.meditation.service;

import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.enums.RevenueCatEventType;
import com.yogida.meditation.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

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
 *
 * <p>Most events describe one subscriber, seen under any of {@code app_user_id},
 * {@code original_app_user_id} or {@code aliases}. {@code TRANSFER} is the exception: it carries
 * none of those and instead names two different subscribers, in {@code transferred_from} and
 * {@code transferred_to}. Both of their entitlements changed, so both are refreshed.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService {

    private final AppUserRepository appUserRepository;
    private final EntitlementService entitlementService;
    private final EntitlementProjectionService projectionService;
    private final SseService sseService;

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
        if (event.id() != null && !projectionService.claimEvent(event.id(), event.type(), ledgerUserId(event))) {
            log.info("RevenueCatWebhookService > Event {} already processed; skipping redelivery", event.id());
            return;
        }

        List<String> affected = affectedUserIds(event);
        if (affected.isEmpty()) {
            log.warn("RevenueCatWebhookService > No known app user on event {} of type {}",
                    event.id(), event.type());
            return;
        }
        for (String userId : affected) {
            entitlementService.refreshUserEntitlement(userId, event.id());
            sseService.publishToUser(userId, event.type());
        }
    }

    /**
     * Every app user whose entitlement this event could have changed.
     *
     * <p>For a transfer that is both sides. Refreshing only the destination would leave the
     * source reading as premium until the projection's staleness window expired, because the
     * entitlements it lost are still in its last projection. Each id is re-read from RevenueCat
     * independently, so the truth for both lands without either being inferred locally.
     *
     * <p>For everything else the several ids are aliases of one subscriber, so the first that
     * resolves is the answer — refreshing per alias would be the same user several times over.
     */
    private List<String> affectedUserIds(RevenueCatWebhookRequest.Event event) {
        if (RevenueCatEventType.TRANSFER.name().equals(event.type())) {
            // An id that is not an app user is normal here rather than an error: a transfer out
            // of an anonymous subscriber carries RevenueCat's own $RCAnonymousID: id, which was
            // never a Keycloak user and never resolves.
            return Stream.concat(idsOf(event.transferredTo()), idsOf(event.transferredFrom()))
                    .distinct()
                    .filter(this::isKnownUser)
                    .toList();
        }
        return Stream.concat(
                        Stream.of(event.appUserId(), event.originalAppUserId()),
                        idsOf(event.aliases()))
                .filter(Objects::nonNull)
                .filter(this::isKnownUser)
                .findFirst()
                .map(List::of)
                .orElseGet(List::of);
    }

    /**
     * What the dedup ledger records alongside the event id. Only ever read by a human looking at
     * the table, so a transfer stores its destination rather than the null an absent
     * {@code app_user_id} used to leave behind.
     */
    private static String ledgerUserId(RevenueCatWebhookRequest.Event event) {
        if (RevenueCatEventType.TRANSFER.name().equals(event.type())) {
            return idsOf(event.transferredTo()).findFirst().orElse(null);
        }
        return event.appUserId();
    }

    private boolean isKnownUser(String keycloakUserId) {
        return appUserRepository.findByKeycloakUserId(keycloakUserId)
                .map(AppUserEntity::getKeycloakUserId)
                .isPresent();
    }

    private static Stream<String> idsOf(List<String> ids) {
        return ids == null ? Stream.empty() : ids.stream().filter(Objects::nonNull);
    }
}
