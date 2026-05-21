package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.MediaCategoryCreateRequest;
import com.yogida.meditation.dto.MediaCategoryDto;
import com.yogida.meditation.dto.MediaCategoryUpdateRequest;
import com.yogida.meditation.entity.MediaCategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MediaCategoryMapper {

    MediaCategoryDto toDto(MediaCategoryEntity entity);

    MediaCategoryEntity toEntity(MediaCategoryCreateRequest request);

    List<MediaCategoryDto> toDtoList(List<MediaCategoryEntity> entities);

    /** Merges non-null fields from {@code request} into the existing {@code entity}. */
    void updateEntity(MediaCategoryUpdateRequest request, @MappingTarget MediaCategoryEntity entity);
}

