package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.entity.MediaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MediaSubscriptionMapper.class, MediaLogMapper.class})
public interface MediaMapper {

    @Mapping(source = "mediaSubscriptions", target = "mediaSubscriptions")
    @Mapping(source = "mediaLogs", target = "mediaLogs")
    MediaDto toDto(MediaEntity entity);

    @Mapping(target = "mediaSubscriptions", ignore = true)
    @Mapping(target = "mediaLogs", ignore = true)
    MediaEntity toEntity(MediaDto dto);

    List<MediaDto> toDtoList(List<MediaEntity> entities);
}
