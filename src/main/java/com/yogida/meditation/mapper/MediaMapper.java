package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.TagEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {MediaSubscriptionMapper.class, MediaLogMapper.class, MediaCategoryMapper.class, S3ObjectMapper.class})
public interface MediaMapper {

    @Mapping(source = "mediaSubscriptions", target = "mediaSubscriptions")
    @Mapping(source = "mediaLogs", target = "mediaLogs")
    @Mapping(source = "category", target = "category")
    @Mapping(source = "mediaObject", target = "mediaObject")
    @Mapping(source = "pictureObject", target = "pictureObject")
    @Mapping(target = "bucketName", expression = "java(entity.getMediaObject() != null ? entity.getMediaObject().getBucketName() : entity.getBucketName())")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "isPremium", source = "isPremium")
    @Mapping(source = "tags", target = "tags", qualifiedByName = "tagEntitiesToNames")
    MediaDto toDto(MediaEntity entity);

    @Mapping(target = "mediaSubscriptions", ignore = true)
    @Mapping(target = "mediaLogs", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "mediaObject", ignore = true)
    @Mapping(target = "pictureObject", ignore = true)
    @Mapping(target = "tags", ignore = true)
    MediaEntity toEntity(MediaDto dto);

    List<MediaDto> toDtoList(List<MediaEntity> entities);

    @Named("tagEntitiesToNames")
    default List<String> tagEntitiesToNames(Set<TagEntity> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream().map(TagEntity::getName).sorted().toList();
    }
}
