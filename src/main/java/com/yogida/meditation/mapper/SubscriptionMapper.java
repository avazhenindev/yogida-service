package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.SubscriptionDto;
import com.yogida.meditation.entity.SubscriptionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface SubscriptionMapper {

    SubscriptionDto toDto(SubscriptionEntity entity);

    SubscriptionEntity toEntity(SubscriptionDto dto);

    List<SubscriptionDto> toDtoList(List<SubscriptionEntity> entities);

    /** Merges non-null fields from {@code dto} into the existing {@code entity}. */
    void updateEntity(SubscriptionDto dto, @MappingTarget SubscriptionEntity entity);
}
