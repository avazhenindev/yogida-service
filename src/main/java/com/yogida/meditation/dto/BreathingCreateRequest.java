package com.yogida.meditation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * JSON part of the breathing exercise create request.
 * Sent as {@code @RequestPart("meta")} alongside the {@code iconFile} multipart part.
 * {@code phases} defines the initial ordered phase structure; audio files are added
 * separately via {@code POST /admin/breathing/phases/{phaseId}/audio}.
 */
public record BreathingCreateRequest(
        @NotBlank String name,
        String description,
        @NotBlank String color,
        @NotNull @Min(1) Integer cycles,
        Boolean isPremium,
        Integer displayOrder,
        @NotNull @NotEmpty @Valid List<BreathingPhaseCreateRequest> phases
) {}
