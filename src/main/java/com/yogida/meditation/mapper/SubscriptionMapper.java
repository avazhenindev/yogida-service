package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.SubscriptionDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.SubscriptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    @Mapping(source = "user.userId", target = "userId")
    SubscriptionDto toDto(SubscriptionEntity entity);

    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    SubscriptionEntity toEntity(SubscriptionDto dto);

    List<SubscriptionDto> toDtoList(List<SubscriptionEntity> entities);

    @Named("userIdToUser")
    default AppUserEntity userIdToUser(Long userId) {
        if (userId == null) {
            return null;
        }
        AppUserEntity user = new AppUserEntity();
        user.setUserId(userId);
        return user;
    }
}


