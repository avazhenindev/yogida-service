package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaStatus;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for updating a media catalog entry.
 * If {@code status} is {@code null}, the existing status is preserved.
 */
public record MediaUpdateRequest(
        @NotBlank String name,
        @NotBlank String s3Url,
        String description,
        String category,
        MediaStatus status
) {}

