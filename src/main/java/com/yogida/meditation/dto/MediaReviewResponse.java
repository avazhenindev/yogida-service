package com.yogida.meditation.dto;

import java.time.LocalDateTime;

/**
 * Response for a single written review entry.
 */
public record MediaReviewResponse(
        Long id,
        Long mediaId,
        Long userId,
        String reviewText,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
