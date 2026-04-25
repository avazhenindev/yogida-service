package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.AppUserDto;
import com.yogida.meditation.entity.AppUserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = UserSubscriptionMapper.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface AppUserMapper {

    @Mapping(source = "subscriptions", target = "subscriptions")
    AppUserDto toDto(AppUserEntity entity);

    AppUserEntity toEntity(AppUserDto dto);

    List<AppUserDto> toDtoList(List<AppUserEntity> entities);

    List<AppUserEntity> toEntityList(List<AppUserDto> dtos);

    /** Merges non-null DTO fields into the existing entity. Skips server-managed timestamps and subscriptions. */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    void updateEntity(AppUserDto dto, @MappingTarget AppUserEntity entity);
}
