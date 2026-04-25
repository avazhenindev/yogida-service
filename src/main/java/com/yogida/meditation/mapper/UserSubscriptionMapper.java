package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.UserSubscriptionDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.SubscriptionEntity;
import com.yogida.meditation.entity.UserSubscriptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", uses = SubscriptionMapper.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserSubscriptionMapper {

    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "subscription.subscriptionId", target = "subscriptionId")
    @Mapping(source = "subscription", target = "subscription")
    UserSubscriptionDto toDto(UserSubscriptionEntity entity);

    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    @Mapping(source = "subscriptionId", target = "subscription", qualifiedByName = "subscriptionIdToSubscription")
    UserSubscriptionEntity toEntity(UserSubscriptionDto dto);

    List<UserSubscriptionDto> toDtoList(List<UserSubscriptionEntity> entities);

    /** Merges non-null DTO fields into the existing entity. */
    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    @Mapping(source = "subscriptionId", target = "subscription", qualifiedByName = "subscriptionIdToSubscription")
    void updateEntity(UserSubscriptionDto dto, @MappingTarget UserSubscriptionEntity entity);

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
