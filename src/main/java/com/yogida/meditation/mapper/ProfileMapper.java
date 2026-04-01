package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.ProfileDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.ProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    @Mapping(source = "user.userId", target = "userId")
    ProfileDto toDto(ProfileEntity entity);

    @Mapping(source = "userId", target = "user", qualifiedByName = "userIdToUser")
    ProfileEntity toEntity(ProfileDto dto);

    List<ProfileDto> toDtoList(List<ProfileEntity> entities);

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


