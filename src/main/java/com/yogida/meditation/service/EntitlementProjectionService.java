package com.yogida.meditation.service;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.entity.UserEntitlementEntity;
import com.yogida.meditation.repository.RevenueCatWebhookEventRepository;
import com.yogida.meditation.repository.UserEntitlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Owns every write to the entitlement projection and the webhook idempotency ledger.
 *
 * <p>It exists as a separate bean for one reason, and it is not stylistic. The hot read paths
 * that resolve entitlement — the catalogue listing in {@code UserMediaFacadeService} and
 * {@code SecureStreamService#generateSecureStreamingUrl} — are annotated
 * {@code @Transactional(readOnly = true)}. Spring's {@code HibernateJpaDialect} puts the
 * session in {@code FlushMode.MANUAL} for a read-only transaction, and a participating inner
 * {@code @Transactional} cannot upgrade it. A repository {@code save} called from there is
 * registered and then silently dropped at commit: no row, no exception, no log. The projection
 * would look implemented and never contain anything.
 *
 * <p>{@link Propagation#REQUIRES_NEW} suspends the caller's transaction and takes its own
 * read-write one, so the write actually commits. It costs a second pooled connection for the
 * duration of the write, which is why it is scoped to just these two short methods and why
 * the read path only reaches them on a cache miss.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntitlementProjectionService {

    private final UserEntitlementRepository entitlementRepository;
    private final RevenueCatWebhookEventRepository webhookEventRepository;
    private final RevenueCatProperties revenueCatProperties;

    /**
     * Reads the projected entitlement for a user. Safe to call from inside a read-only
     * transaction; it joins whatever is there.
     */
    @Transactional(readOnly = true)
    public Optional<UserEntitlementEntity> find(String keycloakUserId) {
        return entitlementRepository.findById(keycloakUserId);
    }

    /**
     * Writes an authoritative snapshot for a user, in its own committed transaction.
     *
     * <p>Never called with a non-authoritative result: an outage must not overwrite a good row
     * with a false one. A failure here is logged and swallowed, because the projection is an
     * optimisation — losing the write costs a RevenueCat call next time, and must never turn a
     * successful entitlement check into a 500.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsert(String keycloakUserId, EntitlementSnapshot snapshot, String sourceEventId) {
        try {
            UserEntitlementEntity row = entitlementRepository.findById(keycloakUserId)
                    .orElseGet(() -> {
                        UserEntitlementEntity fresh = new UserEntitlementEntity();
                        fresh.setKeycloakUserId(keycloakUserId);
                        return fresh;
                    });
            row.setEntitled(snapshot.entitled());
            row.setExpiresAt(snapshot.expiresAt());
            row.setProductIdentifier(snapshot.productIdentifier());
            row.setRefreshedAt(Instant.now());
            row.setSourceEventId(sourceEventId);
            entitlementRepository.save(row);
            log.debug("EntitlementProjectionService > Projected entitlement for {}: entitled={} expiresAt={}",
                    keycloakUserId, snapshot.entitled(), snapshot.expiresAt());
        } catch (DataAccessException e) {
            log.warn("EntitlementProjectionService > Failed to project entitlement for {}: {}",
                    keycloakUserId, e.getMessage());
        }
    }

    /**
     * Claims a RevenueCat event id, returning true when this caller is the one that should
     * process it. A redelivery of the same event returns false.
     *
     * <p>Committed in its own transaction so the claim holds even if the processing that
     * follows fails. That is the deliberate trade: a failed refresh is not retried on
     * redelivery, but the read path refreshes the projection on its own staleness window
     * anyway, so the state converges. Double-processing a purchase is the worse outcome.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimEvent(String eventId, String eventType, String appUserId) {
        try {
            return webhookEventRepository.tryClaim(eventId, eventType, appUserId, Instant.now()) == 1;
        } catch (DataAccessException e) {
            // Fail open: a ledger problem must not drop a genuine entitlement change.
            log.warn("EntitlementProjectionService > Idempotency claim failed for event {}: {}",
                    eventId, e.getMessage());
            return true;
        }
    }

    /**
     * Drops ledger rows older than the retention window, daily at 03:20.
     *
     * <p>The ledger exists only to deduplicate retries, and RevenueCat gives up retrying long
     * before the default retention elapses. Without this the table grows without bound for the
     * life of the service, which is a slower version of the bug this class was added to fix.
     */
    @Scheduled(cron = "${app.revenuecat.webhook-ledger-purge-cron:0 20 3 * * *}")
    @Transactional
    public void purgeExpiredEvents() {
        Instant cutoff = Instant.now().minus(revenueCatProperties.webhookLedgerRetention());
        long removed = webhookEventRepository.deleteByReceivedAtBefore(cutoff);
        if (removed > 0) {
            log.info("EntitlementProjectionService > Purged {} webhook ledger row(s) older than {}",
                    removed, cutoff);
        }
    }
}
