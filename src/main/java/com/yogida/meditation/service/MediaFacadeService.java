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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaFacadeService implements MediaFacadeApi {

    private final MediaApi mediaApi;
    private final MediaLogApi mediaLogApi;
    private final AdminStorageApi adminStorageApi;
    private final R2StorageApi r2StorageApi;
    private final MediaRepository mediaRepository;

    @Override
    @Transactional
    public MediaDto create(MediaCreateRequest request) {
        ObjectMetadataDto uploaded = adminStorageApi.uploadObject(
            request.bucketName(), request.objectKey(), request.file());

        MediaUpdateRequest mediaRequest = new MediaUpdateRequest(
            request.name(), uploaded.s3Url(), request.bucketName(), request.description(), request.categoryId(), request.status());

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

        String newS3Url = oldS3Url;
        if (request.file() != null) {
            String oldKey = r2StorageApi.parseS3Url(oldS3Url)[1];
            if (!request.objectKey().equals(oldKey)) {
                ObjectMetadataDto uploaded = adminStorageApi.uploadObject(
                    request.bucketName(), request.objectKey(), request.file());
                newS3Url = uploaded.s3Url();
            }
        }

        MediaUpdateRequest mediaRequest = new MediaUpdateRequest(
            request.name(), newS3Url, request.bucketName(), request.description(), request.categoryId(), request.status());
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

        MediaEntity entity = resolveEntity(id);
        mediaLogApi.log(entity, MediaLogAction.UPDATED, "Media updated: " + entity.getName());
        return dto;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MediaEntity entity = resolveEntity(id);
        String s3Url = entity.getS3Url();
        String name = entity.getName();

        mediaLogApi.log(entity, MediaLogAction.REMOVED, "Media deleted: " + name);
        mediaApi.delete(id);

        String[] parts = r2StorageApi.parseS3Url(s3Url);
        try {
            adminStorageApi.deleteObject(parts[0], parts[1]);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object after media deletion [bucket={}, key={}]: {}",
                parts[0], parts[1], e.getMessage());
        }
    }

    @Override
    @Transactional
    public void bulkDelete(MediaBulkDeleteRequest request) {
        Map<String, List<String>> keysByBucket = new LinkedHashMap<>();

        for (Long id : request.ids()) {
            MediaEntity entity = resolveEntity(id);
            String[] parts = r2StorageApi.parseS3Url(entity.getS3Url());
            keysByBucket.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
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
}
