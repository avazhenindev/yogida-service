package com.yogida.meditation.dto;

/**
 * Lightweight aggregate used internally to carry average rating per media item.
 * Populated via JPQL constructor expression — not a REST response type.
 */
public record MediaRatingSummary(Long mediaId, Double averageRating) {}
