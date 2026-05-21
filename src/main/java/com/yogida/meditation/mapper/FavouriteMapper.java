package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.FavouriteDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.FavouriteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface FavouriteMapper {

    @Mapping(source = "user.userId", target = "userId")
    FavouriteDto toDto(FavouriteEntity entity);

    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    FavouriteEntity toEntity(FavouriteDto dto);

    List<FavouriteDto> toDtoList(List<FavouriteEntity> entities);

    /** Merges non-null DTO fields into the existing entity. Skips server-managed timestamps. */
    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(FavouriteDto dto, @MappingTarget FavouriteEntity entity);

    @Named("userIdToUser")
    default AppUserEntity userIdToUser(Long userId) {
        if (userId == null) return null;
        AppUserEntity user = new AppUserEntity();
        user.setUserId(userId);
        return user;
    }
}
