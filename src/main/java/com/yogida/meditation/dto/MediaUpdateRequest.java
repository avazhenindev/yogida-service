package com.yogida.meditation.dto;

import com.yogida.meditation.enums.MediaStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for updating a media catalog entry.
 * If {@code status} is {@code null}, the existing status is preserved.
 * Media and picture object metadata is referenced by S3 object id.
 */
public record MediaUpdateRequest(
        @NotBlank String name,
        @NotNull Long mediaObjectId,
        Long pictureObjectId,
        String description,
        Long categoryId,
        MediaStatus status,
        @NotNull Integer durationSeconds,
        List<Long> tagIds,
        boolean requiresPremiumSubscription
) {

    /**
     * Builds the internal hand-off from a create/update body plus the ids the facade resolved.
     *
     * <p>Both call sites previously wrote this out as nine positional arguments. Nine positions of
     * which three are {@code Long} and two are nullable is a shape where a transposition compiles
     * cleanly and fails at runtime.
     */
    public static MediaUpdateRequest from(MediaWriteRequest source, Long mediaObjectId,
                                          Long pictureObjectId, Integer durationSeconds) {
        return new MediaUpdateRequest(
                source.name(),
                mediaObjectId,
                pictureObjectId,
                source.description(),
                source.categoryId(),
                source.status(),
                durationSeconds,
                source.tagIds(),
                source.requiresPremiumSubscription());
    }
}

