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
    /**
     * Merges non-null DTO fields into the existing entity.
     *
     * <p>The owning user is not mergeable: an update must not reassign someone's favourite to
     * another account. {@code favouriteId} is the primary key and comes from the path.
     */
    @Mapping(target = "favouriteId", ignore = true)
    @Mapping(target = "user", ignore = true)
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
