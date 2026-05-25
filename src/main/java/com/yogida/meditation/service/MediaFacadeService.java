package com.yogida.meditation.service;

import com.yogida.meditation.dto.*;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaLogAction;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaFacadeService implements MediaFacadeApi {

    private final MediaApi mediaApi;
    private final MediaLogApi mediaLogApi;
    private final AdminStorageApi adminStorageApi;
    private final R2StorageApi r2StorageApi;
    private final MediaPictureStorageService mediaPictureStorageService;
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
        ObjectMetadataDto uploaded = adminStorageApi.uploadObject(
            request.bucketName(), request.objectKey(), request.file());

        String pictureUrl = mediaPictureStorageService.uploadPicture(request.picture());

        MediaUpdateRequest mediaRequest = new MediaUpdateRequest(
            request.name(), uploaded.s3Url(), request.bucketName(), request.description(), request.categoryId(), request.status(), pictureUrl);

        MediaDto dto = mediaApi.create(mediaRequest);
        MediaEntity entity = resolveEntity(dto.getId());
        mediaLogApi.log(entity, MediaLogAction.ADDED, "Media created: " + entity.getName());
        return dto;
    }

    @Override
    @Transactional
    public MediaDto update(Long id, MediaFileUpdateRequest request) {
        MediaDto existing = mediaApi.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Media", id));
        String oldS3Url = existing.getS3Url();
        String oldPictureUrl = existing.getPicture();

        String newS3Url = oldS3Url;
        if (request.file() != null) {
            String oldKey = r2StorageApi.parseS3Url(oldS3Url)[1];
            if (!request.objectKey().equals(oldKey)) {
                ObjectMetadataDto uploaded = adminStorageApi.uploadObject(
                    request.bucketName(), request.objectKey(), request.file());
                newS3Url = uploaded.s3Url();
            }
        }

        String newPictureUrl = oldPictureUrl;
        boolean shouldDeleteOldPicture = false;
        if (request.picture() != null && !request.picture().isEmpty()) {
            newPictureUrl = mediaPictureStorageService.uploadPicture(request.picture());
            shouldDeleteOldPicture = hasText(oldPictureUrl) && !oldPictureUrl.equals(newPictureUrl);
        }

        MediaUpdateRequest mediaRequest = new MediaUpdateRequest(
            request.name(), newS3Url, request.bucketName(), request.description(), request.categoryId(), request.status(), newPictureUrl);
        MediaDto dto = mediaApi.update(id, mediaRequest);

        if (!newS3Url.equals(oldS3Url)) {
            String[] oldParts = r2StorageApi.parseS3Url(oldS3Url);
            try {
                adminStorageApi.deleteObject(oldParts[0], oldParts[1]);
            } catch (Exception e) {
                log.warn("Failed to delete old S3 object [bucket={}, key={}]: {}",
                    oldParts[0], oldParts[1], e.getMessage());
            }
        }

        if (shouldDeleteOldPicture) {
            deletePictureObjectByUrlAfterCommit(oldPictureUrl);
        }

        MediaEntity entity = resolveEntity(id);
        mediaLogApi.log(entity, MediaLogAction.UPDATED, "Media updated: " + entity.getName());
        return dto;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MediaEntity entity = resolveEntity(id);
        String s3Url = entity.getS3Url();
        String pictureUrl = entity.getPicture();
        String name = entity.getName();

        mediaLogApi.log(entity, MediaLogAction.REMOVED, "Media deleted: " + name);
        mediaApi.delete(id);

        deleteObjectByUrl(s3Url);
        mediaPictureStorageService.deletePictureObjectByUrl(pictureUrl);
    }

    private MediaEntity resolveEntity(Long id) {
        return mediaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }


    private void deleteObjectByUrl(String objectUrl) {
        if (!hasText(objectUrl)) {
            return;
        }
        try {
            String[] parts = r2StorageApi.parseS3Url(objectUrl);
            adminStorageApi.deleteObject(parts[0], parts[1]);
        } catch (Exception e) {
            log.warn("Failed to delete media object [url={}]: {}", objectUrl, e.getMessage());
        }
    }

    private void deletePictureObjectByUrlAfterCommit(String objectUrl) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            mediaPictureStorageService.deletePictureObjectByUrl(objectUrl);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mediaPictureStorageService.deletePictureObjectByUrl(objectUrl);
            }
        });
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
