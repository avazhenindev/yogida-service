package com.yogida.meditation.service;

import com.yogida.meditation.dto.*;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaLogAction;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaFacadeService implements MediaFacadeApi {

    private static final String PICTURE_BUCKET_NAME = "pictures";
    private static final String PICTURE_KEY_PREFIX = "media/";

    private final MediaApi mediaApi;
    private final MediaLogApi mediaLogApi;
    private final AdminStorageApi adminStorageApi;
    private final R2StorageApi r2StorageApi;
    private final MediaRepository mediaRepository;

    @Value("${app.media.max-picture-size-bytes:512000}")
    private long maxPictureSizeBytes;

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

        String pictureUrl = uploadPicture(request.picture());

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
            newPictureUrl = uploadPicture(request.picture());
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
            deleteObjectByUrlAfterCommit(oldPictureUrl, "old picture object");
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

        deleteObjectByUrl(s3Url, "media object");
        deleteObjectByUrl(pictureUrl, "picture object");
    }

    @Override
    @Transactional
    public void bulkDelete(MediaBulkDeleteRequest request) {
        Map<String, List<String>> keysByBucket = new LinkedHashMap<>();

        for (Long id : request.ids()) {
            MediaEntity entity = resolveEntity(id);
            addObjectKey(keysByBucket, entity.getS3Url());
            addObjectKey(keysByBucket, entity.getPicture());
            mediaLogApi.log(entity, MediaLogAction.REMOVED, "Media bulk-deleted: " + entity.getName());
            mediaApi.delete(id);
        }

        keysByBucket.forEach((bucket, keys) -> {
            try {
                adminStorageApi.bulkDeleteObjects(bucket, keys);
            } catch (Exception e) {
                log.warn("Failed to bulk-delete S3 objects [bucket={}, keys={}]: {}",
                    bucket, keys, e.getMessage());
            }
        });
    }

    private MediaEntity resolveEntity(Long id) {
        return mediaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

    private String uploadPicture(MultipartFile picture) {
        if (picture == null || picture.isEmpty()) {
            return null;
        }

        if (picture.getSize() > maxPictureSizeBytes) {
            throw new IllegalArgumentException("Picture exceeds max size of " + maxPictureSizeBytes + " bytes");
        }

        return adminStorageApi.uploadObject(PICTURE_BUCKET_NAME, buildPictureObjectKey(picture), picture).s3Url();
    }

    private String buildPictureObjectKey(MultipartFile picture) {
        String originalFilename = picture.getOriginalFilename();
        String filename = hasText(originalFilename) ? originalFilename : "picture";
        filename = filename.substring(Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!hasText(filename)) {
            filename = "picture";
        }
        return PICTURE_KEY_PREFIX + UUID.randomUUID() + "-" + filename;
    }

    private void addObjectKey(Map<String, List<String>> keysByBucket, String objectUrl) {
        if (!hasText(objectUrl)) {
            return;
        }
        try {
            String[] parts = r2StorageApi.parseS3Url(objectUrl);
            keysByBucket.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
        } catch (Exception e) {
            log.warn("Failed to parse object URL for bulk deletion [url={}]: {}", objectUrl, e.getMessage());
        }
    }

    private void deleteObjectByUrl(String objectUrl, String objectLabel) {
        if (!hasText(objectUrl)) {
            return;
        }
        try {
            String[] parts = r2StorageApi.parseS3Url(objectUrl);
            adminStorageApi.deleteObject(parts[0], parts[1]);
        } catch (Exception e) {
            log.warn("Failed to delete {} [url={}]: {}", objectLabel, objectUrl, e.getMessage());
        }
    }

    private void deleteObjectByUrlAfterCommit(String objectUrl, String objectLabel) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteObjectByUrl(objectUrl, objectLabel);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteObjectByUrl(objectUrl, objectLabel);
            }
        });
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
