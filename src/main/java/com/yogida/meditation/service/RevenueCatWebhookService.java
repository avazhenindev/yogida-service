package com.yogida.meditation.service;

import com.yogida.meditation.config.EntitlementEventProperties;
import com.yogida.meditation.dto.EntitlementEventMessage;
import com.yogida.meditation.dto.EntitlementEventMessage.TransferDirection;
import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.enums.RevenueCatEventType;
import com.yogida.meditation.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Processes RevenueCat webhook events.
 *
 * <p>Every event that resolves to a known app user is forwarded to that user's SSE clients as an
 * {@link EntitlementEventMessage}, whatever its type: what an event means to the user is the app's
 * decision, not this service's. For an entitlement-affecting type the user's entitlement is first
 * re-read from the RevenueCat Subscriber API and written to the durable projection, so a refetch the
 * frame prompts already sees the new state. No subscription state is invented locally — RevenueCat
 * remains the source of truth and the projection only mirrors it.
 *
 * <p>The refresh and the publish are isolated from each other, and from other recipients. Two
 * events for one user can arrive in the same second and race on the projection insert; that
 * failure used to escape as a 409 and take the push down with it. A failed refresh is now retried
 * once, and the event is forwarded either way.
 *
 * <p>Deliveries are deduplicated by RevenueCat's event id, because RevenueCat retries and the
 * endpoint's published contract has always claimed idempotency.
 *
 * <p>Most events describe one subscriber, seen under any of {@code app_user_id},
 * {@code original_app_user_id} or {@code aliases}. {@code TRANSFER} is the exception: it carries
 * none of those and instead names two different subscribers, in {@code transferred_from} and
 * {@code transferred_to}. Both of their entitlements changed, so both are refreshed, and each is
 * sent the event with the direction of the transfer from its own side.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class RevenueCatWebhookService {

    private final AppUserRepository appUserRepository;
    private final EntitlementService entitlementService;
    private final EntitlementProjectionService projectionService;
    private final SseService sseService;
    private final EntitlementEventProperties eventProperties;

    public void processEvent(RevenueCatWebhookRequest request) {
        RevenueCatWebhookRequest.Event event = request == null ? null : request.event();
        if (event == null || event.type() == null) {
            log.warn("RevenueCatWebhookService > Skipping webhook without event type");
            return;
        }
        boolean refresh = RevenueCatEventType.isEntitlementAffecting(event.type());
        log.info("RevenueCatWebhookService > Processing event {} of type {} (projection refresh: {})",
                event.id(), event.type(), refresh);

        // RevenueCat retries; without this a single purchase can be handled several times.
        // An event with no id cannot be deduplicated, so it is processed rather than dropped.
        if (event.id() != null && !projectionService.claimEvent(event.id(), event.type(), ledgerUserId(event))) {
            log.info("RevenueCatWebhookService > Event {} already processed; skipping redelivery", event.id());
            return;
        }

        List<Recipient> recipients = affectedUsers(event);
        if (recipients.isEmpty()) {
            // Only worth a warning when an entitlement may have changed for someone we cannot
            // find. A paywall or test event for an unknown id is routine.
            if (refresh) {
                log.warn("RevenueCatWebhookService > No known app user on event {} of type {}",
                        event.id(), event.type());
            } else {
                log.info("RevenueCatWebhookService > No known app user on event {} of type {}",
                        event.id(), event.type());
            }
            return;
        }

        boolean bannerAllowed = eventProperties.bannerAllowedFor(event.environment());
        for (Recipient recipient : recipients) {
            if (refresh) {
                refreshBeforeForwarding(recipient.userId(), event);
            }
            try {
                sseService.publishToUser(recipient.userId(),
                        EntitlementEventMessage.from(event, recipient.direction(), bannerAllowed));
            } catch (RuntimeException e) {
                // Class name only: an unexpected exception from the publish path can quote the frame.
                log.warn("RevenueCatWebhookService > Forwarding event {} ({}) to user {} failed: {}",
                        event.id(), event.type(), recipient.userId(), e.getClass().getSimpleName());
            }
        }
    }

    /**
     * Re-reads one recipient's entitlement ahead of the push. Never throws: the event is forwarded
     * whatever happens here.
     *
     * <p>A refresh that throws has almost always lost the race to insert the user's first
     * projection row. That failure surfaces at commit, past the upsert's own catch, so this
     * refresh never reached the cache — and the winner, a same-second event or a read-path miss,
     * may have cached state from before this event, which the frame's refetch would be served.
     * The row exists by then, so one retry updates it instead. If that fails too, the cached
     * entry is at least dropped.
     */
    private void refreshBeforeForwarding(String userId, RevenueCatWebhookRequest.Event event) {
        try {
            entitlementService.refreshUserEntitlement(userId, event.id());
            return;
        } catch (RuntimeException e) {
            log.warn("RevenueCatWebhookService > Entitlement refresh failed for user {} on event {} ({}); "
                    + "retrying once: {}", userId, event.id(), event.type(), e.getMessage());
        }
        try {
            entitlementService.refreshUserEntitlement(userId, event.id());
        } catch (RuntimeException e) {
            log.warn("RevenueCatWebhookService > Entitlement refresh failed again for user {} on event {} ({}); "
                    + "evicting the cache and forwarding anyway: {}", userId, event.id(), event.type(), e.getMessage());
            entitlementService.evictUserEntitlement(userId);
        }
    }

    /** A user an event is forwarded to and, for a TRANSFER only, which side of it they are on. */
    private record Recipient(String userId, TransferDirection direction) {
    }

    /**
     * Every app user this event is about.
     *
     * <p>For a transfer that is both sides. Refreshing only the destination would leave the
     * source reading as premium until the projection's staleness window expired, because the
     * entitlements it lost are still in its last projection. Each id is re-read from RevenueCat
     * independently, so the truth for both lands without either being inferred locally. An id
     * named on both sides is one recipient, and the destination side wins.
     *
     * <p>For everything else the several ids are aliases of one subscriber, so the first that
     * resolves is the answer — refreshing per alias would be the same user several times over.
     * Aliases that resolve to several distinct app users, a rare legacy case, are not handled:
     * only the first is sent the event.
     */
    private List<Recipient> affectedUsers(RevenueCatWebhookRequest.Event event) {
        if (RevenueCatEventType.TRANSFER.name().equals(event.type())) {
            Map<String, TransferDirection> sides = new LinkedHashMap<>();
            idsOf(event.transferredTo()).forEach(id -> sides.putIfAbsent(id, TransferDirection.IN));
            idsOf(event.transferredFrom()).forEach(id -> sides.putIfAbsent(id, TransferDirection.OUT));
            // An id that is not an app user is normal here rather than an error: a transfer out
            // of an anonymous subscriber carries RevenueCat's own $RCAnonymousID: id, which was
            // never a Keycloak user and never resolves.
            return sides.entrySet().stream()
                    .filter(side -> isKnownUser(side.getKey()))
                    .map(side -> new Recipient(side.getKey(), side.getValue()))
                    .toList();
        }
        return Stream.concat(
                        Stream.of(event.appUserId(), event.originalAppUserId()),
                        idsOf(event.aliases()))
                .filter(Objects::nonNull)
                .filter(this::isKnownUser)
                .findFirst()
                .map(userId -> List.of(new Recipient(userId, null)))
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
