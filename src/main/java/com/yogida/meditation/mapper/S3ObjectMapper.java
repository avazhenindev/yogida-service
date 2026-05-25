package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.S3ObjectDto;
import com.yogida.meditation.entity.S3ObjectEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface S3ObjectMapper {

    @Mapping(target = "url", expression = "java(entity == null ? null : entity.getFullUrl())")
    S3ObjectDto toDto(S3ObjectEntity entity);
}

