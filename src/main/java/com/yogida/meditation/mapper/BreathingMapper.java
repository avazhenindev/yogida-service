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
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * Maps {@link BreathingEntity} and its phase/audio children to response DTOs.
 * All mappings must be called inside an active transaction to avoid
 * {@code LazyInitializationException} on lazily-loaded collections.
 */
/*
 * unmappedTargetPolicy is ERROR rather than the default WARN on purpose. The isPremium bug
 * documented below shipped precisely because an unmapped target property is only a warning:
 * the compiler said so on every build and it scrolled past. Anything this mapper forgets is
 * now a build failure.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BreathingMapper {

    /**
     * The premium flag has to be mapped explicitly. The entity's field is {@code isPremium},
     * which Lombok exposes as the bean property {@code premium}, while the DTO's record
     * component is named {@code isPremium} — so the names never matched and MapStruct left the
     * DTO's flag at its default. Every breathing exercise therefore reported itself as free,
     * whatever the database said. The compiler warned about it: "Unmapped target property".
     */
    @Mapping(target = "isPremium", source = "premium")
    @Mapping(target = "icon", source = "iconObject", qualifiedByName = "iconObjectToUrl")
    @Mapping(target = "phases", source = "phases")
    // This mapper serves the admin path, which is never locked out of its own content.
    // The user-facing path goes through BreathingUserFacadeService, which re-derives it.
    @Mapping(target = "locked", constant = "false")
    BreathingDto toDto(BreathingEntity entity);

    List<BreathingDto> toDtoList(List<BreathingEntity> entities);

    @Mapping(target = "duration", source = "durationSeconds")
    @Mapping(target = "audioFiles", source = "audioFiles")
    BreathingPhaseDto toPhaseDto(BreathingPhaseEntity phase);

    /**
     * Maps only the object id here. The URL is deliberately left null and filled in later by
     * whichever facade is serving the request: phase audio lives in the private bucket, so
     * {@code fullUrl} would be an unsigned URL that returns 403 — and mapping it would put a
     * URL-shaped value into a field that must stay empty for a caller who is not entitled.
     */
    @Mapping(target = "id", source = "audioObject.id")
    @Mapping(target = "url", ignore = true)
    BreathingPhaseAudioDto toAudioDto(BreathingPhaseAudioEntity audio);

    @Named("iconObjectToUrl")
    default String iconObjectToUrl(com.yogida.meditation.entity.S3ObjectEntity iconObject) {
        return iconObject != null ? iconObject.getFullUrl() : null;
    }
}
