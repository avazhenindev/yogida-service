package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaSubscriptionDto;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaSubscriptionEntity;
import com.yogida.meditation.entity.SubscriptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MediaSubscriptionMapper {

    @Mapping(source = "media.id", target = "mediaId")
    @Mapping(source = "subscription.subscriptionId", target = "subscriptionId")
    MediaSubscriptionDto toDto(MediaSubscriptionEntity entity);

    @Mapping(source = "mediaId", target = "media", qualifiedByName = "mediaIdToMedia")
    @Mapping(source = "subscriptionId", target = "subscription", qualifiedByName = "subscriptionIdToSubscription")
    MediaSubscriptionEntity toEntity(MediaSubscriptionDto dto);

    List<MediaSubscriptionDto> toDtoList(List<MediaSubscriptionEntity> entities);

    @Named("mediaIdToMedia")
    default MediaEntity mediaIdToMedia(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaEntity media = new MediaEntity();
        media.setId(mediaId);
        return media;
    }

    @Named("subscriptionIdToSubscription")
    default SubscriptionEntity subscriptionIdToSubscription(Long subscriptionId) {
        if (subscriptionId == null) {
            return null;
        }
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setSubscriptionId(subscriptionId);
        return subscription;
    }
}

