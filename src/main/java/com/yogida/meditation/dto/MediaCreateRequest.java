package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Multipart request for creating a new media catalog entry and uploading its S3 object in one step.
 * If {@code status} is {@code null}, defaults to {@link MediaStatus#ACTIVE}.
 * If {@code picture} is provided, it is uploaded to the Cloudflare R2 picture bucket
 * and stored as a public URL when a public picture base URL is configured.
 * If {@code durationSeconds} is omitted, the backend extracts it automatically from the uploaded file.
 */
public record MediaCreateRequest(
        @NotBlank String name,
        @NotBlank String bucketName,
        @NotBlank String objectKey,
        @NotNull MultipartFile file,
        String description,
        Long categoryId,
        MediaStatus status,
        MultipartFile picture,
        Integer durationSeconds,
        List<Long> tagIds
) {}

