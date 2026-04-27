package com.yogida.meditation.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multipart request for updating a media catalog entry.
 * If {@code file} is non-null and {@code objectKey} differs from the existing one,
 * a new S3 object is uploaded and the old one is deleted.
 */
public record MediaFileUpdateRequest(
        @NotBlank String name,
        @NotBlank String bucketName,
        @NotBlank String objectKey,
        MultipartFile file,
        String description,
        String category
) {}

