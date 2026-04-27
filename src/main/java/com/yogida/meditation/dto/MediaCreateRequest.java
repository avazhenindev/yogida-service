package com.yogida.meditation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multipart request for creating a new media catalog entry and uploading its S3 object in one step.
 */
public record MediaCreateRequest(
        @NotBlank String name,
        @NotBlank String bucketName,
        @NotBlank String objectKey,
        @NotNull MultipartFile file,
        String description,
        String category
) {}

