package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaUpdateRequest;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaLogAction;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.MediaApi;
import com.yogida.meditation.service.api.MediaFacadeApi;
import com.yogida.meditation.service.api.MediaLogApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaFacadeService implements MediaFacadeApi {

    private final MediaApi mediaApi;
    private final MediaLogApi mediaLogApi;
    private final MediaRepository mediaRepository;

    @Override
    @Transactional
    public MediaDto create(MediaUpdateRequest request) {
        MediaDto dto = mediaApi.create(request);
        MediaEntity entity = resolveEntity(dto.getId());
        mediaLogApi.log(entity, MediaLogAction.ADDED, "Media created: " + entity.getName());
        return dto;
    }

    @Override
    @Transactional
    public MediaDto update(Long id, MediaUpdateRequest request) {
        MediaDto dto = mediaApi.update(id, request);
        MediaEntity entity = resolveEntity(id);
        mediaLogApi.log(entity, MediaLogAction.UPDATED, "Media updated: " + entity.getName());
        return dto;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MediaEntity entity = resolveEntity(id);
        mediaLogApi.log(entity, MediaLogAction.REMOVED, "Media deleted: " + entity.getName());
        mediaApi.delete(id);
    }

    private MediaEntity resolveEntity(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }
}


