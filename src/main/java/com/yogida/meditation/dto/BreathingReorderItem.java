package com.yogida.meditation.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Single item in a bulk reorder request for breathing exercises.
 */
public record BreathingReorderItem(
        @NotNull Long id,
        @NotNull Integer displayOrder
) {}
