package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.TagEntity;
import com.yogida.meditation.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Mapper for converting MediaEntity to user-facing MediaDto with entitlement logic applied.
 * Sets isPremium based on whether the user has access to the media.
 * Withholds the mediaObject.url if the user is not entitled and the media is premium.
 */
@Component
@RequiredArgsConstructor
public class MediaUserMapper {

    private final MediaMapper mediaMapper;
    private final EntitlementService entitlementService;
    private final S3ObjectMapper s3ObjectMapper;
    private final MediaSubscriptionMapper mediaSubscriptionMapper;
    private final MediaLogMapper mediaLogMapper;
    private final MediaCategoryMapper mediaCategoryMapper;

    /**
     * Converts a MediaEntity to a user-facing MediaDto with entitlement applied.
     * - Sets isPremium based on media subscriptions (not database field)
     * - Withholds media URL if not entitled to premium media
     *
     * @param entity the media entity
     * @param user the app user (null for unauthenticated users)
     * @return the user-facing media DTO
     */
    public MediaDto toDtoForUser(MediaEntity entity, AppUserEntity user) {
        // First map using the standard mapper (without isPremium from entity)
        MediaDto dto = mediaMapper.toDto(entity);

        // Determine entitlement
        boolean isPremium = entitlementService.isPremium(entity);
        boolean isEntitled = user != null && entitlementService.isEntitled(user, entity);

        // Set isPremium based on user entitlement
        dto.setIsPremium(isPremium && !isEntitled);

        // Withhold media URL if not entitled
        if (isPremium && !isEntitled && dto.getMediaObject() != null) {
            dto.getMediaObject().setUrl(null);
        }

        return dto;
    }

    /**
     * Converts a list of MediaEntity objects to user-facing DTOs with entitlement applied.
     *
     * @param entities the media entities
     * @param user the app user (null for unauthenticated users)
     * @return list of user-facing media DTOs
     */
    public List<MediaDto> toDtoListForUser(List<MediaEntity> entities, AppUserEntity user) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
            .map(entity -> toDtoForUser(entity, user))
            .toList();
    }
}
