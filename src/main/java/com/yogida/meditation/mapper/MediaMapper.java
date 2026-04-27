package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.entity.MediaEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MediaMapper {

    MediaDto toDto(MediaEntity entity);

    MediaEntity toEntity(MediaDto dto);

    List<MediaDto> toDtoList(List<MediaEntity> entities);
}
