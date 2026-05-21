package com.yogida.meditation.dto;

import java.time.Instant;

/**
 * Lightweight bucket descriptor returned by admin S3 endpoints.
 */
public record BucketDto(String name, Instant creationDate) {}

