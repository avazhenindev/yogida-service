package com.yogida.meditation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a breathing exercise.
 */
public record BreathingDto(
        Long id,
        String name,
        String icon,
        String description,
        String color,
        int cycles,
        boolean isPremium,
        int displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BreathingPhaseDto> phases
) {}
