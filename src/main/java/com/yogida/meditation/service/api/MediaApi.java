package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaUpdateRequest;
import com.yogida.meditation.enums.MediaStatus;

import java.util.List;
import java.util.Optional;

public interface MediaApi {

    List<MediaDto> findAll();

    List<MediaDto> findAllActive();

    Optional<MediaDto> findById(Long id);

    MediaDto create(MediaUpdateRequest request);

    MediaDto update(Long id, MediaUpdateRequest request);

    void delete(Long id);

    /**
     * Updates only the status field; used by the scheduler health-check.
     */
    void updateStatus(Long id, MediaStatus status);
}
