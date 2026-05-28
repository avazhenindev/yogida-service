package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaRatingSummaryResponse;
import com.yogida.meditation.dto.MediaReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MediaReviewApi {

    /**
     * Creates or updates the written review for the given user/media pair.
     *
     * @param mediaId    target media
     * @param userId     user submitting the review
     * @param reviewText review body (max 2 000 characters)
     */
    MediaReviewResponse upsertReview(Long mediaId, Long userId, String reviewText);

    /**
     * Returns paginated reviews for a media item, ordered by creation date descending.
     */
    Page<MediaReviewResponse> findReviewsByMediaId(Long mediaId, Pageable pageable);

    /**
     * Returns the current user's review for a media item, or empty if none exists.
     */
    java.util.Optional<MediaReviewResponse> findUserReview(Long mediaId, Long userId);

    /**
     * Returns aggregate rating data for a media item.
     */
    MediaRatingSummaryResponse getRatingSummary(Long mediaId);
}
