package com.yogida.meditation.repository;

import com.yogida.meditation.entity.RevenueCatWebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface RevenueCatWebhookEventRepository extends JpaRepository<RevenueCatWebhookEventEntity, String> {

    /**
     * Attempts to claim an event id, returning 1 when this caller won the claim and 0 when the
     * event was already recorded.
     *
     * <p>Written as a native insert on purpose. Going through {@code save()} would not work:
     * Spring Data decides {@code persist} vs {@code merge} from
     * {@code AbstractEntityInformation.isNew}, which for an assigned String id is
     * {@code id == null} — always false here — so every save becomes a {@code merge}. Merge
     * SELECTs first and then UPDATEs an existing row, raising no constraint violation, which
     * makes it check-then-act with no mutual exclusion whatsoever. {@code ON CONFLICT DO
     * NOTHING} pushes the decision into the database where it is actually atomic.
     */
    @Modifying
    @Query(value = """
            INSERT INTO revenuecat_webhook_event (rc_event_id, event_type, rc_app_user_id, received_at)
            VALUES (:eventId, :eventType, :appUserId, :receivedAt)
            ON CONFLICT (rc_event_id) DO NOTHING
            """, nativeQuery = true)
    int tryClaim(@Param("eventId") String eventId,
                 @Param("eventType") String eventType,
                 @Param("appUserId") String appUserId,
                 @Param("receivedAt") Instant receivedAt);

    /** Removes ledger rows older than the cut-off, so the table stays bounded. */
    @Modifying
    long deleteByReceivedAtBefore(Instant cutoff);
}
