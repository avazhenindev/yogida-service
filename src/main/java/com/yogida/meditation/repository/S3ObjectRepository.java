package com.yogida.meditation.repository;

import com.yogida.meditation.entity.S3ObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface S3ObjectRepository extends JpaRepository<S3ObjectEntity, Long> {

    /**
     * Counts everything still pointing at an S3 object, across all four referencing columns.
     *
     * <p>Native rather than JPQL because it spans four unrelated entities and the answer only has
     * to be a number. Every column that references {@code s3_object} must appear here — a missed
     * one means a row still in use is treated as an orphan and deleted.
     */
    @Query(value = """
            SELECT
              (SELECT count(*) FROM media WHERE media_object_id = :id)
            + (SELECT count(*) FROM media WHERE picture_object_id = :id)
            + (SELECT count(*) FROM breathing WHERE icon_object_id = :id)
            + (SELECT count(*) FROM breathing_phase_audio WHERE audio_object_id = :id)
            """, nativeQuery = true)
    long countReferents(@Param("id") Long id);

    /**
     * Every {@code s3_object} row that nothing references. Read by the reconciliation report;
     * deliberately not used to delete anything automatically.
     */
    @Query(value = """
            SELECT * FROM s3_object o
            WHERE NOT EXISTS (SELECT 1 FROM media m WHERE m.media_object_id = o.id)
              AND NOT EXISTS (SELECT 1 FROM media m WHERE m.picture_object_id = o.id)
              AND NOT EXISTS (SELECT 1 FROM breathing b WHERE b.icon_object_id = o.id)
              AND NOT EXISTS (SELECT 1 FROM breathing_phase_audio a WHERE a.audio_object_id = o.id)
            ORDER BY o.id
            """, nativeQuery = true)
    List<S3ObjectEntity> findOrphans();
}
