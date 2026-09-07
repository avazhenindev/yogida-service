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
import java.util.function.BooleanSupplier;
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
        MediaDto dto = mapWithoutFavourite(entity, premiumResolver(user));
        // Single-item path: one query for one row is the right shape.
        enrichWithFavourite(dto, user, entity.getId());
        return dto;
    }

    /**
     * Maps one entity and applies entitlement, without touching favourites.
     *
     * <p>Split out so the list path can apply favourites from a single batch query instead of
     * one query per item — the list used to call the single-item path and then immediately
     * overwrite both favourite fields from the batch map, so every one of those N queries was
     * issued and discarded.
     *
     * @param premiumUser resolved lazily; see {@link #premiumResolver(AppUserEntity)}
     */
    private MediaDto mapWithoutFavourite(MediaEntity entity, BooleanSupplier premiumUser) {
        MediaDto dto = mediaMapper.toDto(entity);

        // Suppress admin-only field from user-facing responses
        dto.setRequiresPremiumSubscription(null);

        boolean isPremium = entitlementService.isPremium(entity);
        // Free media short-circuits before the supplier is touched, which is what keeps a
        // catalogue containing no premium items at zero entitlement resolutions.
        boolean isEntitled = !isPremium || premiumUser.getAsBoolean();

        dto.setIsPremium(isPremium && !isEntitled);

        if (isPremium && !isEntitled && dto.getMediaObject() != null) {
            dto.getMediaObject().setUrl(null);
        }
        return dto;
    }

    /**
     * A once-per-request entitlement resolution, deferred until a premium item is actually seen.
     *
     * <p>Entitlement is a property of the user, not of the item, so resolving it per item repeats
     * identical work. Resolving it eagerly instead would be a different regression: a listing with
     * no premium content would start paying for a lookup it never previously made — and on a cache
     * miss that lookup is a RevenueCat round trip.
     */
    private BooleanSupplier premiumResolver(AppUserEntity user) {
        if (user == null) {
            return () -> false;
        }
        return new BooleanSupplier() {
            private Boolean resolved;

            @Override
            public boolean getAsBoolean() {
                if (resolved == null) {
                    resolved = entitlementService.isUserPremium(user.getKeycloakUserId());
                }
                return resolved;
            }
        };
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

        // One query for every favourite the user has, instead of one per media item.
        Map<Long, Long> favouriteMap = buildFavouriteMap(user);
        BooleanSupplier premiumUser = premiumResolver(user);

        return entities.stream()
            .map(entity -> {
                MediaDto dto = mapWithoutFavourite(entity, premiumUser);
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
