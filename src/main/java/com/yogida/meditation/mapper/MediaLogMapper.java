package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaLogDto;
import com.yogida.meditation.entity.MediaLogEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MediaLogMapper {

    @Mapping(source = "media.id", target = "mediaId")
    MediaLogDto toDto(MediaLogEntity entity);

    List<MediaLogDto> toDtoList(List<MediaLogEntity> entities);
}

