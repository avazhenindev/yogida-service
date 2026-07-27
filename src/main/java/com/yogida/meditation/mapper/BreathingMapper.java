package com.yogida.meditation.mapper;

import com.yogida.meditation.dto.BreathingDto;
import com.yogida.meditation.dto.BreathingPhaseAudioDto;
import com.yogida.meditation.dto.BreathingPhaseDto;
import com.yogida.meditation.entity.BreathingEntity;
import com.yogida.meditation.entity.BreathingPhaseAudioEntity;
import com.yogida.meditation.entity.BreathingPhaseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

/**
 * Maps {@link BreathingEntity} and its phase/audio children to response DTOs.
 * All mappings must be called inside an active transaction to avoid
 * {@code LazyInitializationException} on lazily-loaded collections.
 */
@Mapper(componentModel = "spring")
public interface BreathingMapper {

    @Mapping(target = "icon", source = "iconObject", qualifiedByName = "iconObjectToUrl")
    @Mapping(target = "phases", source = "phases")
    BreathingDto toDto(BreathingEntity entity);

    List<BreathingDto> toDtoList(List<BreathingEntity> entities);

    @Mapping(target = "duration", source = "durationSeconds")
    @Mapping(target = "audioFiles", source = "audioFiles")
    BreathingPhaseDto toPhaseDto(BreathingPhaseEntity phase);

    @Mapping(target = "id", source = "audioObject.id")
    @Mapping(target = "url", source = "audioObject.fullUrl")
    BreathingPhaseAudioDto toAudioDto(BreathingPhaseAudioEntity audio);

    @Named("iconObjectToUrl")
    default String iconObjectToUrl(com.yogida.meditation.entity.S3ObjectEntity iconObject) {
        return iconObject != null ? iconObject.getFullUrl() : null;
    }
}
