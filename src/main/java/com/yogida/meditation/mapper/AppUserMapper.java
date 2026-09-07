package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.AppUserDto;
import com.yogida.meditation.entity.AppUserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AppUserMapper {

    AppUserDto toDto(AppUserEntity entity);

    AppUserEntity toEntity(AppUserDto dto);

    List<AppUserDto> toDtoList(List<AppUserEntity> entities);

    List<AppUserEntity> toEntityList(List<AppUserDto> dtos);

    /**
     * Merges non-null DTO fields into the existing entity.
     *
     * <p>Identity is not mergeable. With {@code NullValuePropertyMappingStrategy.IGNORE},
     * every non-null field in the request body was copied onto the managed entity — including
     * {@code keycloakUserId}, the claim the whole authentication chain hangs on. A caller
     * could point one account's row at another subject and inherit that account, its data and
     * its paid entitlement. {@code userId} is the primary key and equally not the client's to
     * set.
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(AppUserDto dto, @MappingTarget AppUserEntity entity);
}
