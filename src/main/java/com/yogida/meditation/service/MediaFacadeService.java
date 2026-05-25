package com.yogida.meditation.service;

import com.yogida.meditation.dto.*;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.enums.MediaLogAction;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaFacadeService implements MediaFacadeApi {

    private final MediaApi mediaApi;
    private final MediaLogApi mediaLogApi;
    private final AdminStorageApi adminStorageApi;
    private final MediaPictureStorageService mediaPictureStorageService;
    private final S3ObjectService s3ObjectService;
    private final MediaRepository mediaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MediaDto> findAll() {
        return mediaApi.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaDto> findAllActive() {
        return mediaApi.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MediaDto> findById(Long id) {
        return mediaApi.findById(id);
    }

    @Override
    @Transactional
    public MediaDto create(MediaCreateRequest request) {
        adminStorageApi.uploadObject(request.bucketName(), request.objectKey(), request.file());
        S3ObjectEntity mediaObject = s3ObjectService.createMediaObject(request.bucketName(), request.objectKey());

        S3ObjectEntity pictureObject = mediaPictureStorageService.uploadPicture(request.picture());

        MediaUpdateRequest mediaRequest = new MediaUpdateRequest(
            request.name(), mediaObject.getId(), pictureObject == null ? null : pictureObject.getId(), request.description(), request.categoryId(), request.status());

        MediaDto dto = mediaApi.create(mediaRequest);
        MediaEntity entity = resolveEntity(dto.getId());
        mediaLogApi.log(entity, MediaLogAction.ADDED, "Media created: " + entity.getName());
        return dto;
    }

    @Override
    @Transactional
    public MediaDto update(Long id, MediaFileUpdateRequest request) {
        MediaEntity existingEntity = resolveEntity(id);
        S3ObjectEntity oldMediaObject = existingEntity.getMediaObject();
        S3ObjectEntity oldPictureObject = existingEntity.getPictureObject();

        S3ObjectEntity newMediaObject = oldMediaObject;
        if (request.file() != null) {
            String oldKey = oldMediaObject.getObjectUri();
            if (!request.objectKey().equals(oldKey)) {
                adminStorageApi.uploadObject(request.bucketName(), request.objectKey(), request.file());
                newMediaObject = s3ObjectService.createMediaObject(request.bucketName(), request.objectKey());
            }
        }

        S3ObjectEntity newPictureObject = oldPictureObject;
        boolean shouldDeleteOldPicture = false;
        if (request.picture() != null && !request.picture().isEmpty()) {
            newPictureObject = mediaPictureStorageService.uploadPicture(request.picture());
            shouldDeleteOldPicture = oldPictureObject != null
                && !oldPictureObject.getId().equals(newPictureObject.getId());
        }

        MediaUpdateRequest mediaRequest = new MediaUpdateRequest(
            request.name(), newMediaObject.getId(), newPictureObject == null ? null : newPictureObject.getId(),
            request.description(), request.categoryId(), request.status());
        MediaDto dto = mediaApi.update(id, mediaRequest);

        if (!newMediaObject.getId().equals(oldMediaObject.getId())) {
            s3ObjectService.deleteObjectAfterCommit(oldMediaObject);
        }

        if (shouldDeleteOldPicture) {
            s3ObjectService.deleteObjectAfterCommit(oldPictureObject);
        }

        MediaEntity entity = resolveEntity(id);
        mediaLogApi.log(entity, MediaLogAction.UPDATED, "Media updated: " + entity.getName());
        return dto;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MediaEntity entity = resolveEntity(id);
        S3ObjectEntity mediaObject = entity.getMediaObject();
        S3ObjectEntity pictureObject = entity.getPictureObject();
        String name = entity.getName();

        mediaLogApi.log(entity, MediaLogAction.REMOVED, "Media deleted: " + name);
        mediaApi.delete(id);

        s3ObjectService.deleteObjectAfterCommit(mediaObject);
        s3ObjectService.deleteObjectAfterCommit(pictureObject);
    }

    private MediaEntity resolveEntity(Long id) {
        return mediaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

}
