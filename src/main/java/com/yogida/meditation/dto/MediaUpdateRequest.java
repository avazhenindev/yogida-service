package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for updating a media catalog entry.
 * If {@code status} is {@code null}, the existing status is preserved.
 * Media and picture object metadata is referenced by S3 object id.
 */
public record MediaUpdateRequest(
        @NotBlank String name,
        @NotNull Long mediaObjectId,
        Long pictureObjectId,
        String description,
        Long categoryId,
        MediaStatus status,
        @NotNull Integer durationSeconds,
        List<Long> tagIds,
        boolean requiresPremiumSubscription
) {}

