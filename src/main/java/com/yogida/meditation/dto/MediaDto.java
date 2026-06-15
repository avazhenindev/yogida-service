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
    private Integer durationSeconds;
    private Double averageRating;
    private Boolean isPremium;
    private List<String> tags;

    /**
     * Whether the current user has marked this media as a favourite.
     * Null for unauthenticated requests.
     */
    private Boolean isFavourite;

    /**
     * The ID of the favourite record if this media is favourited by the current user.
     * Null if not favourited or for unauthenticated requests.
     * Used by mobile to delete the favourite without fetching all favourites.
     */
    private Long favouriteId;
}

