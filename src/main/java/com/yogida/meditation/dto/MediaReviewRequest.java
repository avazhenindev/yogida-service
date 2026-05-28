package com.yogida.meditation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating or updating a written review.
 */
public record MediaReviewRequest(
        @NotNull Long userId,
        @NotBlank @Size(max = 2000) String reviewText
) {}
