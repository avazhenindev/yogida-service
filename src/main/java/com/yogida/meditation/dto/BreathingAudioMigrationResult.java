package com.yogida.meditation.dto;

import java.util.List;

/**
 * Outcome of the one-off migration that moves breathing phase audio into the private bucket.
 *
 * @param dryRun        true when nothing was actually changed
 * @param targetBucket  the private bucket objects were (or would be) moved into
 * @param moved         objects moved, or that would move on a real run
 * @param skipped       objects left alone, with the reason
 * @param failed        objects that could not be moved, with the reason
 */
public record BreathingAudioMigrationResult(
        boolean dryRun,
        String targetBucket,
        List<String> moved,
        List<String> skipped,
        List<String> failed
) {}
