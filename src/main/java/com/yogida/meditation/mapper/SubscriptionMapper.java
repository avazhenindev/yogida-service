package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.SubscriptionDto;
import com.yogida.meditation.entity.SubscriptionEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    SubscriptionDto toDto(SubscriptionEntity entity);

    SubscriptionEntity toEntity(SubscriptionDto dto);

    List<SubscriptionDto> toDtoList(List<SubscriptionEntity> entities);
}
