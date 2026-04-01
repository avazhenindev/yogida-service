package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.AppUserDto;
import com.yogida.meditation.entity.AppUserEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AppUserMapper {

    AppUserDto toDto(AppUserEntity entity);

    AppUserEntity toEntity(AppUserDto dto);

    List<AppUserDto> toDtoList(List<AppUserEntity> entities);

    List<AppUserEntity> toEntityList(List<AppUserDto> dtos);
}

