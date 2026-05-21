package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.ProfileDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.ProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProfileMapper {

    @Mapping(source = "user.userId", target = "userId")
    ProfileDto toDto(ProfileEntity entity);

    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    ProfileEntity toEntity(ProfileDto dto);

    List<ProfileDto> toDtoList(List<ProfileEntity> entities);

    /** Merges non-null DTO fields into the existing entity. Skips server-managed timestamps. */
    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(ProfileDto dto, @MappingTarget ProfileEntity entity);

    @Named("userIdToUser")
    default AppUserEntity userIdToUser(Long userId) {
        if (userId == null) return null;
        AppUserEntity user = new AppUserEntity();
        user.setUserId(userId);
        return user;
    }
}
