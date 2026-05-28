package com.yogida.meditation.repository;

import com.yogida.meditation.dto.MediaRatingSummary;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaRatingRepository extends JpaRepository<MediaRatingEntity, Long> {

    Optional<MediaRatingEntity> findByUserAndMedia(AppUserEntity user, MediaEntity media);

    /**
     * Returns average ratings grouped by media ID for the given set of IDs.
     * Use for efficient batch enrichment of media lists.
     */
    @Query("""
            SELECT new com.yogida.meditation.dto.MediaRatingSummary(mr.media.id, AVG(mr.rating))
            FROM MediaRatingEntity mr
            WHERE mr.media.id IN :mediaIds
            GROUP BY mr.media.id
            """)
    List<MediaRatingSummary> findAverageRatingsByMediaIds(@Param("mediaIds") Collection<Long> mediaIds);

    /**
     * Returns the average rating for a single media item, or empty if no ratings exist.
     */
    @Query("""
            SELECT AVG(mr.rating)
            FROM MediaRatingEntity mr
            WHERE mr.media.id = :mediaId
            """)
    Optional<Double> findAverageRatingByMediaId(@Param("mediaId") Long mediaId);

    /**
     * Returns the total count of ratings for a single media item.
     */
    @Query("""
            SELECT COUNT(mr)
            FROM MediaRatingEntity mr
            WHERE mr.media.id = :mediaId
            """)
    long countByMediaId(@Param("mediaId") Long mediaId);
}
