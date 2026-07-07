package com.yogida.meditation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a review/rating.
 *
 * <p>The target media item is identified by the URL path variable {@code {mediaId}}
 * (e.g. {@code PUT /media/42/review}) — it is intentionally absent from this record.</p>
 *
 * {@code rating} updates or sets the star rating (1–5).
 * {@code reviewText} is written once on creation; subsequent saves ignore it.
 */
public record MediaReviewSaveRequest(
        @NotNull Long userId,
        @Min(1) @Max(5) Integer rating,
        @Size(max = 2000) String reviewText
) {}
