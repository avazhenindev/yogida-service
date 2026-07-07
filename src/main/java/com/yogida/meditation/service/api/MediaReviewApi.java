package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaRatingSummary;
import com.yogida.meditation.dto.MediaRatingSummaryResponse;
import com.yogida.meditation.dto.MediaReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MediaReviewApi {

    /**
     * Creates or updates the review/rating row for the given user/media pair.
     * {@code reviewText} is written on first save only; subsequent calls preserve the existing text.
     */
    MediaReviewResponse save(Long mediaId, Long userId, Integer rating, String reviewText);

    /** Returns paginated reviews for a media item, sortable via Pageable. */
    Page<MediaReviewResponse> findReviewsByMediaId(Long mediaId, Pageable pageable);

    /** Returns the current user's review for a media item, or empty if none exists. */
    Optional<MediaReviewResponse> findUserReview(Long mediaId, Long userId);

    /** Returns aggregate rating summary including per-star histogram for a media item. */
    MediaRatingSummaryResponse getRatingSummary(Long mediaId);

    /** Returns the average rating for a single media item, or {@code 0.0} when unrated. */
    double findAverageRatingByMediaId(Long mediaId);

    /**
     * Returns average ratings grouped by media ID for the given set of IDs.
     * Media IDs with no ratings are omitted from the result.
     */
    List<MediaRatingSummary> findAverageRatingsByMediaIds(Collection<Long> mediaIds);
}
