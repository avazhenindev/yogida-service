package com.yogida.meditation.dto;

import java.util.List;

/**
 * Paginated response for object listing within a bucket.
 */
public record ObjectListResponse(
        String bucketName,
        List<ObjectMetadataDto> objects,
        String nextContinuationToken,
        boolean truncated
) {}

