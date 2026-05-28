package com.yogida.meditation.dto;

import java.time.LocalDateTime;

/**
 * Response containing a user's rating for a media item.
 */
public record MediaRatingResponse(
        Long mediaId,
        Long userId,
        Integer rating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
