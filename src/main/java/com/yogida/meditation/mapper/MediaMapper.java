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

@Mapper(componentModel = "spring", uses = {MediaCategoryMapper.class, S3ObjectMapper.class})
public interface MediaMapper {

    @Mapping(source = "category", target = "category")
    @Mapping(source = "mediaObject", target = "mediaObject")
    @Mapping(source = "pictureObject", target = "pictureObject")
    // media.media_object_id is NOT NULL, so the old fallback to a duplicate bucket_name column
    // on media itself was unreachable. The S3 object is the single place a bucket is recorded.
    @Mapping(target = "bucketName", source = "mediaObject.bucketName")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "isPremium", ignore = true)
    @Mapping(target = "isFavourite", ignore = true)
    @Mapping(target = "favouriteId", ignore = true)
    @Mapping(source = "tags", target = "tags", qualifiedByName = "tagEntitiesToNames")
    MediaDto toDto(MediaEntity entity);

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
