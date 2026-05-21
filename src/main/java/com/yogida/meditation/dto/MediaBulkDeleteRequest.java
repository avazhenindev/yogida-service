package com.yogida.meditation.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request body for bulk-deleting media records and their associated S3 objects.
 */
public record MediaBulkDeleteRequest(
        @NotEmpty List<Long> ids
) {}

