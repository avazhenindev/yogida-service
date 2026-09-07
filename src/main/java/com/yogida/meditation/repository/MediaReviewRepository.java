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

    /**
     * Paginated list of all rows for a media item (including rating-only rows), sortable by Pageable.
     *
     * <p>The user is fetch-joined because the response mapper reads the reviewer's email, which
     * otherwise initialised one lazy proxy per row of the page. An inner JOIN FETCH is correct
     * rather than a LEFT one: the association is {@code optional = false}.
     *
     * <p>The explicit countQuery must stay — Spring Data cannot derive a count from a query
     * carrying a fetch join. Note this is a to-one fetch, so it does not trigger Hibernate's
     * in-memory pagination warning the way a collection fetch on a paged query would.
     */
    @Query(
        value = """
            SELECT r FROM MediaReviewEntity r
            JOIN FETCH r.user
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
