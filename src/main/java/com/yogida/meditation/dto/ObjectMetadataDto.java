package com.yogida.meditation.dto;

import java.time.Instant;

/**
 * Metadata for a single S3 object.
 */
public record ObjectMetadataDto(
        String key,
        long sizeBytes,
        Instant lastModified,
        String eTag,
        String s3Url
) {}

