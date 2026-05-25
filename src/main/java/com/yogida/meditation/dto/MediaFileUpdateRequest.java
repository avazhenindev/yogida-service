package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

/**
 * Multipart request for updating a media catalog entry.
 * If {@code file} is non-null and {@code objectKey} differs from the existing one,
 * a new S3 object is uploaded and the old one is deleted.
 * If {@code status} is {@code null}, the existing status is preserved.
 * If {@code picture} is provided, it replaces the existing Cloudflare R2 picture object.
 */
public record MediaFileUpdateRequest(
        @NotBlank String name,
        @NotBlank String bucketName,
        @NotBlank String objectKey,
        MultipartFile file,
        String description,
        Long categoryId,
        MediaStatus status,
        MultipartFile picture
) {}

