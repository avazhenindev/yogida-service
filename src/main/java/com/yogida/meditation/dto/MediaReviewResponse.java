package com.yogida.meditation.dto;

import java.time.LocalDateTime;

/**
 * Response for a single review/rating entry.
 * {@code rating} is null when the user wrote a review without a star rating.
 * {@code reviewText} is null when the user only submitted a star rating.
 */
public record MediaReviewResponse(
        Long id,
        Long mediaId,
        Long userId,
        String userName,
        String userInitial,
        Integer rating,
        String reviewText,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
