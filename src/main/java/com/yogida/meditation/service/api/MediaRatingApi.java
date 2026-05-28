package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaRatingSummary;

import java.util.Collection;
import java.util.List;

public interface MediaRatingApi {

    /**
     * Creates or updates the rating for the given user/media pair.
     *
     * @param mediaId target media
     * @param userId  user submitting the rating
     * @param rating  value between 1 and 5 inclusive
     */
    void upsertRating(Long mediaId, Long userId, int rating);

    /**
     * Returns average ratings for a batch of media IDs.
     * Media IDs with no ratings are omitted from the result.
     */
    List<MediaRatingSummary> findAverageRatingsByMediaIds(Collection<Long> mediaIds);

    /**
     * Returns the average rating for a single media item, or {@code 0.0} when unrated.
     */
    double findAverageRatingByMediaId(Long mediaId);
}
