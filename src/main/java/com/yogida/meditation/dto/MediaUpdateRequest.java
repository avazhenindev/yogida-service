package com.yogida.meditation.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for creating or updating a media catalog entry.
 */
public record MediaUpdateRequest(
        @NotBlank String name,
        @NotBlank String s3Url,
        String description,
        String category
) {}

