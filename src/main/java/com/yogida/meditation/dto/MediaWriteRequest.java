package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * The catalogue fields shared by the create and update request bodies.
 *
 * <p>{@link MediaCreateRequest} and {@link MediaFileUpdateRequest} carry the same ten components
 * and differ only in which of them are required — {@code file} is {@code @NotNull} on create.
 * Merging the two records would change the OpenAPI surface, so they stay separate and share this
 * view instead, which exists purely so the facade can translate either one without writing the
 * translation twice.
 */
public interface MediaWriteRequest {
    String name();
    String bucketName();
    MultipartFile file();
    String description();
    Long categoryId();
    MediaStatus status();
    MultipartFile picture();
    Integer durationSeconds();
    List<Long> tagIds();
    boolean requiresPremiumSubscription();
}
