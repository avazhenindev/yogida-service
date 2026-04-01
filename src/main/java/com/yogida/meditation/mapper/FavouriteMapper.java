package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.FavouriteDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.FavouriteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FavouriteMapper {

    @Mapping(source = "user.userId", target = "userId")
    FavouriteDto toDto(FavouriteEntity entity);

    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    FavouriteEntity toEntity(FavouriteDto dto);

    List<FavouriteDto> toDtoList(List<FavouriteEntity> entities);

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


