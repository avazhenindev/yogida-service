package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaStatus;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for updating a media catalog entry.
 * If {@code status} is {@code null}, the existing status is preserved.
 * Picture is stored as a public Cloudflare R2 picture URL when configured.
 */
public record MediaUpdateRequest(
        @NotBlank String name,
        @NotBlank String s3Url,
        @NotBlank String bucketName,
        String description,
        Long categoryId,
        MediaStatus status,
        String picture
) {}

