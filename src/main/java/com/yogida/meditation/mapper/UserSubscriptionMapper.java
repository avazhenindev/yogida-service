package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.UserSubscriptionDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.SubscriptionEntity;
import com.yogida.meditation.entity.UserSubscriptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserSubscriptionMapper {

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "subscription.subscriptionId", target = "subscriptionId")
    UserSubscriptionDto toDto(UserSubscriptionEntity entity);

    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    @Mapping(source = "subscriptionId", target = "subscription", qualifiedByName = "subscriptionIdToSubscription")
    UserSubscriptionEntity toEntity(UserSubscriptionDto dto);

    List<UserSubscriptionDto> toDtoList(List<UserSubscriptionEntity> entities);

    @Named("userIdToUser")
    default AppUserEntity userIdToUser(Long userId) {
        if (userId == null) return null;
        AppUserEntity user = new AppUserEntity();
        user.setUserId(userId);
        return user;
    }

    @Named("subscriptionIdToSubscription")
    default SubscriptionEntity subscriptionIdToSubscription(Long subscriptionId) {
        if (subscriptionId == null) return null;
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setSubscriptionId(subscriptionId);
        return subscription;
    }
}

