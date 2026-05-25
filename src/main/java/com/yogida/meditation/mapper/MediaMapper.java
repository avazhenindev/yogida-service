package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.entity.MediaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MediaSubscriptionMapper.class, MediaLogMapper.class, MediaCategoryMapper.class, S3ObjectMapper.class})
public interface MediaMapper {

    @Mapping(source = "mediaSubscriptions", target = "mediaSubscriptions")
    @Mapping(source = "mediaLogs", target = "mediaLogs")
    @Mapping(source = "category", target = "category")
    @Mapping(source = "mediaObject", target = "mediaObject")
    @Mapping(source = "pictureObject", target = "pictureObject")
    @Mapping(target = "bucketName", expression = "java(entity.getMediaObject() != null ? entity.getMediaObject().getBucketName() : entity.getBucketName())")
    MediaDto toDto(MediaEntity entity);

    @Mapping(target = "mediaSubscriptions", ignore = true)
    @Mapping(target = "mediaLogs", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "mediaObject", ignore = true)
    @Mapping(target = "pictureObject", ignore = true)
    MediaEntity toEntity(MediaDto dto);

    List<MediaDto> toDtoList(List<MediaEntity> entities);
}
