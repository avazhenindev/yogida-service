package com.yogida.meditation.dto;

/**
 * Response DTO for a single audio object attached to a breathing phase.
 */
public record BreathingPhaseAudioDto(
        Long id,
        String url
) {}
