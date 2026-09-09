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
) {

    /**
     * A copy with {@code locked} replaced.
     *
     * <p>Here rather than in the facade that needs it: rebuilding all twelve components by hand at
     * the call site means every component added to this record has to be threaded through those
     * copies too, and the compiler only complains if the count changes — not if two arguments of
     * the same type are swapped.
     */
    public BreathingDto withLocked(boolean locked) {
        return new BreathingDto(id, name, icon, description, color, cycles, isPremium, locked,
                displayOrder, createdAt, updatedAt, phases);
    }

    /** A copy with {@code phases} replaced — used to swap in presigned audio URLs. */
    public BreathingDto withPhases(List<BreathingPhaseDto> phases) {
        return new BreathingDto(id, name, icon, description, color, cycles, isPremium, locked,
                displayOrder, createdAt, updatedAt, phases);
    }
}
