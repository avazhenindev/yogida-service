package com.yogida.meditation.dto;

import java.util.Map;

/**
 * Aggregate rating summary for a media item.
 * {@code starBreakdown} maps star values 1–5 to their respective counts.
 * All keys 1–5 are always present (missing stars have a count of 0).
 */
public record MediaRatingSummaryResponse(
        Long mediaId,
        Double averageRating,
        Long ratingCount,
        Map<Integer, Long> starBreakdown
) {}
