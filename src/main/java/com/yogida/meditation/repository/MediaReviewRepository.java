package com.yogida.meditation.repository;

import com.yogida.meditation.dto.MediaRatingSummary;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaReviewRepository extends JpaRepository<MediaReviewEntity, Long> {

    /** Finds the single review/rating row for a (user, media) pair. */
    Optional<MediaReviewEntity> findByUserAndMedia(AppUserEntity user, MediaEntity media);

    /** Paginated list of all rows for a media item (including rating-only rows), sortable by Pageable. */
    @Query(
        value = """
            SELECT r FROM MediaReviewEntity r
            WHERE r.media.id = :mediaId
            """,
        countQuery = """
            SELECT COUNT(r) FROM MediaReviewEntity r
            WHERE r.media.id = :mediaId
            """
    )
    Page<MediaReviewEntity> findAllByMediaId(
            @Param("mediaId") Long mediaId, Pageable pageable);

    /** Average rating across all rows that have a non-null rating for the media item. */
    @Query("""
            SELECT AVG(r.rating)
            FROM MediaReviewEntity r
            WHERE r.media.id = :mediaId
              AND r.rating IS NOT NULL
            """)
    Optional<Double> findAverageRatingByMediaId(@Param("mediaId") Long mediaId);

    /** Count of rows with a non-null rating for the media item. */
    @Query("""
            SELECT COUNT(r)
            FROM MediaReviewEntity r
            WHERE r.media.id = :mediaId
              AND r.rating IS NOT NULL
            """)
    long countRatingsByMediaId(@Param("mediaId") Long mediaId);

    /** Per-star rating counts for the histogram. Returns [starValue, count] pairs. */
    @Query("""
            SELECT r.rating, COUNT(r)
            FROM MediaReviewEntity r
            WHERE r.media.id = :mediaId
              AND r.rating IS NOT NULL
            GROUP BY r.rating
            """)
    List<Object[]> countByMediaIdGroupByRating(@Param("mediaId") Long mediaId);

    /** Batch average ratings grouped by media ID — used for media list enrichment. */
    @Query("""
            SELECT new com.yogida.meditation.dto.MediaRatingSummary(r.media.id, AVG(r.rating))
            FROM MediaReviewEntity r
            WHERE r.media.id IN :mediaIds
              AND r.rating IS NOT NULL
            GROUP BY r.media.id
            """)
    List<MediaRatingSummary> findAverageRatingsByMediaIds(@Param("mediaIds") Collection<Long> mediaIds);
}
