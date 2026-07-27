package com.yogida.meditation.dto;

import java.util.List;

/**
 * Response DTO for a single phase within a breathing exercise.
 * {@code audioFiles} contains the public URL and S3 object ID for each
 * associated audio file.
 */
public record BreathingPhaseDto(
        Long id,
        String name,
        String label,
        int duration,
        String color,
        int displayOrder,
        List<BreathingPhaseAudioDto> audioFiles
) {}
