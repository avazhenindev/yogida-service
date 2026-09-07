package com.yogida.meditation.dto;

/**
 * Response DTO for a single audio object attached to a breathing phase.
 *
 * @param id  the S3 object id
 * @param url a presigned, time-limited URL for the audio, or {@code null} when the caller is
 *            not entitled to this exercise. Phase audio lives in the private bucket, so this
 *            is the only way to reach it — there is no public URL to fall back on.
 */
public record BreathingPhaseAudioDto(
        Long id,
        String url
) {}
