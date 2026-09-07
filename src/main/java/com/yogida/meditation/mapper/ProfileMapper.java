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

    /**
     * Merges non-null DTO fields into the existing entity.
     *
     * <p>Neither the primary key nor the owning user is mergeable: an update must not be able
     * to move a profile onto a different account, and {@code profileId} is not the client's to
     * set. The row to update is identified by the path variable alone.
     */
    @Mapping(target = "profileId", ignore = true)
    @Mapping(target = "user", ignore = true)
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
