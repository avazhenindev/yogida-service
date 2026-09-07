package com.yogida.meditation.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for a breathing exercise.
 *
 * <p>{@code isPremium} says whether the exercise requires a subscription; {@code locked} says
 * whether THIS caller is being denied it. They differ for an entitled subscriber, who sees
 * {@code isPremium=true, locked=false}. When {@code locked} is true every phase audio URL in
 * this DTO is null — the metadata is still returned so the paywall can show what is on offer.
 */
public record BreathingDto(
        Long id,
        String name,
        String icon,
        String description,
        String color,
        int cycles,
        boolean isPremium,
        boolean locked,
        int displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<BreathingPhaseDto> phases
) {}
