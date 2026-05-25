package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaDto {
    private Long id;
    private String name;
    private String bucketName;
    private S3ObjectDto mediaObject;
    private MediaStatus status;
    private String description;
    private S3ObjectDto pictureObject;
    private MediaCategoryDto category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MediaSubscriptionDto> mediaSubscriptions;
    private List<MediaLogDto> mediaLogs;
}
