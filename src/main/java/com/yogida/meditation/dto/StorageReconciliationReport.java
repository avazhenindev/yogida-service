package com.yogida.meditation.dto;

import java.util.List;
import java.util.Map;

/**
 * Read-only comparison of the {@code s3_object} table against what R2 actually holds.
 *
 * @param orphanedRows      {@code s3_object} rows nothing references. These are the costly ones:
 *                          {@code unique(bucket, base_url, uri)} means each permanently blocks
 *                          its own key from being written again.
 * @param orphanedObjects   object keys present in a bucket with no {@code s3_object} row, by
 *                          bucket. These waste storage but break nothing.
 * @param missingObjects    keys an {@code s3_object} row points at that are absent from the
 *                          bucket, by bucket. A row here resolves to a 404 at playback.
 * @param bucketsInspected  buckets the report actually looked at
 * @param bucketsFailed     buckets that could not be listed, with the reason
 */
public record StorageReconciliationReport(
        List<String> orphanedRows,
        Map<String, List<String>> orphanedObjects,
        Map<String, List<String>> missingObjects,
        List<String> bucketsInspected,
        Map<String, String> bucketsFailed
) {}
