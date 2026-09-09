package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The media catalogue entry as returned to a caller.
 *
 * <p>Mutable, and deliberately so — this is one of six {@code @Data} DTOs among twenty-three
 * records, which reads as drift and is not. The records are request bodies and responses that are
 * complete when constructed. This one is <em>enriched per request</em> after mapping: the facade
 * fills in {@code averageRating} from a second query, and MediaUserAssembler overwrites
 * {@code isPremium}, {@code isFavourite}, {@code favouriteId} and the nested media object's URL
 * according to who is asking. Fourteen call sites, one of them mutating the nested S3ObjectDto.
 *
 * <p>Making it a record would replace those with fourteen copy-constructions of a seventeen-
 * component record, threaded through the assembler's per-item loops — harder to read than the
 * mutation it removed, and slower on the list path. The DTOs that are NOT enriched
 * (AppUserDto, SubscriptionDto) could be records today; converting only those would leave the
 * split in place while gaining nothing.
 *
 * <p>If this does become a record, do it together with S3ObjectDto and give both withers, the way
 * BreathingDto has them.
 */
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
    private Integer durationSeconds;
    private Double averageRating;
    private Boolean isPremium;
    /** Admin-facing flag: whether this media requires a premium subscription. Not set in user-facing responses. */
    private Boolean requiresPremiumSubscription;
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

