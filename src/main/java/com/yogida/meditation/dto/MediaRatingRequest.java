package com.yogida.meditation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating or updating a media rating.
 */
public record MediaRatingRequest(
        @NotNull Long userId,
        @NotNull @Min(1) @Max(5) Integer rating
) {}
