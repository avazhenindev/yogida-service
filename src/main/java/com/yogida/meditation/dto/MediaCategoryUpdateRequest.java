package com.yogida.meditation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for updating an existing media category.
 */
public record MediaCategoryUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 255) String description
) {}

