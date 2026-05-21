package com.yogida.meditation.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request body for bulk S3 object deletion.
 */
public record BulkDeleteRequest(@NotEmpty List<String> keys) {}

