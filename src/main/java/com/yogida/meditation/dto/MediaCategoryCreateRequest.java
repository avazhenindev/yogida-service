package com.yogida.meditation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a new media category.
 */
public record MediaCategoryCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description
) {}

