package com.yogida.meditation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * JSON part of the breathing exercise update request.
 * Sent as {@code @RequestPart("meta")} alongside the optional {@code iconFile} multipart part.
 * All fields are optional; only non-null values are applied.
 * If {@code phases} is provided it fully replaces the current phase list (audio is lost on replaced phases).
 */
public record BreathingUpdateRequest(
        String name,
        String description,
        String color,
        @Min(1) Integer cycles,
        Boolean isPremium,
        Integer displayOrder,
        @Valid List<BreathingPhaseCreateRequest> phases
) {}
