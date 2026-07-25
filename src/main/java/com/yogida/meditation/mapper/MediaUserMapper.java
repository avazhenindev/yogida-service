package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.FavouriteEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.ContentType;
import com.yogida.meditation.repository.FavouriteRepository;
import com.yogida.meditation.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mapper for converting MediaEntity to user-facing MediaDto with entitlement logic and favourite data applied.
 * Sets isPremium based on whether the user has access to the media.
 * Withholds the mediaObject.url if the user is not entitled and the media is premium.
 * Populates isFavourite and favouriteId based on user's favourites.
 */
@Component
@RequiredArgsConstructor
public class MediaUserMapper {

    private final MediaMapper mediaMapper;
    private final EntitlementService entitlementService;
    private final FavouriteRepository favouriteRepository;
    private final S3ObjectMapper s3ObjectMapper;
    private final MediaCategoryMapper mediaCategoryMapper;

    /**
     * Converts a MediaEntity to a user-facing MediaDto with entitlement applied and favourite data populated.
     * - Sets isPremium based on media subscriptions (not database field)
     * - Withholds media URL if not entitled to premium media
     * - Populates isFavourite and favouriteId based on user's favourites
     *
     * @param entity the media entity
     * @param user the app user
     * @return the user-facing media DTO
     */
    public MediaDto toDtoForUser(MediaEntity entity, AppUserEntity user) {
        // First map using the standard mapper (without isPremium from entity)
        MediaDto dto = mediaMapper.toDto(entity);

        // Suppress admin-only field from user-facing responses
        dto.setRequiresPremiumSubscription(null);

        // Determine entitlement
        boolean isPremium = entitlementService.isPremium(entity);
        boolean isEntitled = user != null && entitlementService.isEntitled(user, entity);

        // Set isPremium based on user entitlement
        dto.setIsPremium(isPremium && !isEntitled);

        // Withhold media URL if not entitled
        if (isPremium && !isEntitled && dto.getMediaObject() != null) {
            dto.getMediaObject().setUrl(null);
        }

        // Enrich with favourite data
        enrichWithFavourite(dto, user, entity.getId());

        return dto;
    }

    /**
     * Converts a list of MediaEntity objects to user-facing DTOs with entitlement applied and favourite data populated.
     *
     * @param entities the media entities
     * @param user the app user
     * @return list of user-facing media DTOs
     */
    public List<MediaDto> toDtoListForUser(List<MediaEntity> entities, AppUserEntity user) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        // Build a map of media ID -> favourite ID for efficient lookup
        Map<Long, Long> favouriteMap = buildFavouriteMap(user);

        return entities.stream()
            .map(entity -> {
                MediaDto dto = toDtoForUser(entity, user);
                // Override favourite data with pre-fetched map for batch efficiency
                applyFavouriteFromMap(dto, favouriteMap);
                return dto;
            })
            .toList();
    }

    /**
     * Build a map of media ID -> favourite ID for a user.
     * This is used for efficient batch processing of multiple media items.
     */
    private Map<Long, Long> buildFavouriteMap(AppUserEntity user) {
        if (user == null) {
            return Collections.emptyMap();
        }

        return favouriteRepository.findByUserUserId(user.getUserId()).stream()
            .filter(f -> ContentType.MEDIA.value().equals(f.getContentType()))
            .collect(Collectors.toMap(FavouriteEntity::getContentId, FavouriteEntity::getFavouriteId));
    }

    /**
     * Enrich a single MediaDto with favourite information for the given user and media ID.
     */
    private void enrichWithFavourite(MediaDto mediaDto, AppUserEntity user, Long mediaId) {
        if (user == null) {
            mediaDto.setIsFavourite(false);
            mediaDto.setFavouriteId(null);
            return;
        }

        Optional<FavouriteEntity> favourite = favouriteRepository.findByUserUserIdAndContentTypeAndContentId(
            user.getUserId(),
            ContentType.MEDIA.value(),
            mediaId
        );

        if (favourite.isPresent()) {
            mediaDto.setIsFavourite(true);
            mediaDto.setFavouriteId(favourite.get().getFavouriteId());
        } else {
            mediaDto.setIsFavourite(false);
            mediaDto.setFavouriteId(null);
        }
    }

    /**
     * Apply favourite data from a pre-built map to a MediaDto.
     * Used in batch processing for efficiency.
     */
    private void applyFavouriteFromMap(MediaDto mediaDto, Map<Long, Long> favouriteMap) {
        Long mediaId = mediaDto.getId();
        if (favouriteMap.containsKey(mediaId)) {
            mediaDto.setIsFavourite(true);
            mediaDto.setFavouriteId(favouriteMap.get(mediaId));
        } else {
            mediaDto.setIsFavourite(false);
            mediaDto.setFavouriteId(null);
        }
    }
}
