package com.yogida.meditation.dto;

/**
 * DTO representing a media category dictionary entry.
 */
public record MediaCategoryDto(
        Long id,
        String name,
        String description
) {}

