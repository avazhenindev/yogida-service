package com.yogida.meditation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Represents a single phase definition within a {@link BreathingCreateRequest} or
 * {@link BreathingUpdateRequest}.
 *
 * <ul>
 *   <li>{@code id} — present for existing phases in an update; null for new phases.</li>
 *   <li>{@code audioObjectIdsToRemove} — S3 object IDs of saved audio files to delete.</li>
 *   <li>{@code newAudioFileIndices} — positions into the flat {@code audioFiles} multipart list
 *       that belong to this phase.</li>
 * </ul>
 */
public record BreathingPhaseCreateRequest(
        Long id,
        @NotBlank String name,
        @NotBlank String label,
        @NotNull @Min(1) Integer durationSeconds,
        @NotBlank String color,
        Integer displayOrder,
        List<Long> audioObjectIdsToRemove,
        List<Integer> newAudioFileIndices
) {}
