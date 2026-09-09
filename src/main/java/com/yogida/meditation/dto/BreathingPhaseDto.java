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
) {

    /**
     * A copy with {@code audioFiles} replaced — used to swap in presigned audio URLs.
     *
     * <p>Same reason as {@link BreathingDto#withPhases}: the facade rebuilt this record from all
     * seven components to substitute one. Two adjacent components are {@code String}
     * ({@code name}, {@code label}) and two are {@code int} ({@code duration},
     * {@code displayOrder}), so a transposition compiles silently and ships swapped phase labels.
     */
    public BreathingPhaseDto withAudioFiles(List<BreathingPhaseAudioDto> audioFiles) {
        return new BreathingPhaseDto(id, name, label, duration, color, displayOrder, audioFiles);
    }
}
