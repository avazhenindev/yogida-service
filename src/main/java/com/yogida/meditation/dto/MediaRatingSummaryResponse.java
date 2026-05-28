package com.yogida.meditation.dto;

/**
 * Aggregate rating summary for a media item returned by {@code GET /media/{mediaId}/rating-summary}.
 */
public record MediaRatingSummaryResponse(
        Long mediaId,
        Double averageRating,
        Long ratingCount
) {}
