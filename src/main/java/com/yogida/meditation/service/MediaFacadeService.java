package com.yogida.meditation.service;

import com.yogida.meditation.dto.*;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.service.storage.StorageKeys;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.*;
import lombok.RequiredArgsConstructor;import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaFacadeService {

    private final MediaService mediaService;
    private final MediaPictureStorageService mediaPictureStorageService;
    private final S3ObjectService s3ObjectService;
    private final MediaRepository mediaRepository;
    private final MediaReviewService mediaReviewService;
    private final MediaDurationService mediaDurationService;

    @Transactional(readOnly = true)
    public List<MediaDto> findAll() {
        List<MediaDto> dtos = mediaService.findAll();
        enrichWithAverageRating(dtos);
        return dtos;
    }

    @Transactional(readOnly = true)
    public Optional<MediaDto> findById(Long id) {
        return mediaService.findById(id).map(dto -> {
            dto.setAverageRating(mediaReviewService.findAverageRatingByMediaId(id));
            return dto;
        });
    }

    @Transactional
    public MediaDto create(MediaCreateRequest request) {
        // Server-generated: see StorageKeys. The client used to supply this and collisions
        // silently overwrote another item's audio.
        String objectKey = StorageKeys.mediaKey(request.file() == null ? null : request.file().getOriginalFilename());
        // If anything after this point fails — validation, ffprobe, a constraint — the object
        // would otherwise stay in R2 with no row pointing at it, and permanently hold its key.
        s3ObjectService.uploadStaged(request.bucketName(), objectKey, request.file());
        S3ObjectEntity mediaObject = s3ObjectService.createMediaObject(request.bucketName(), objectKey);

        S3ObjectEntity pictureObject = mediaPictureStorageService.uploadPicture(request.picture());

        int durationSeconds = request.durationSeconds() != null && request.durationSeconds() > 0
                ? request.durationSeconds()
                : mediaDurationService.extractDurationSeconds(request.file());

        MediaUpdateRequest mediaRequest = new MediaUpdateRequest(
            request.name(), mediaObject.getId(), pictureObject == null ? null : pictureObject.getId(),
            request.description(), request.categoryId(), request.status(),
            durationSeconds, request.tagIds(), request.requiresPremiumSubscription());

        MediaDto dto = mediaService.create(mediaRequest);
        dto.setAverageRating(0.0);
        return dto;
    }

    @Transactional
    public MediaDto update(Long id, MediaFileUpdateRequest request) {
        MediaEntity existingEntity = resolveEntity(id);
        S3ObjectEntity oldMediaObject = existingEntity.getMediaObject();
        S3ObjectEntity oldPictureObject = existingEntity.getPictureObject();

        S3ObjectEntity newMediaObject = oldMediaObject;
        if (request.file() != null && !request.file().isEmpty()) {
            // Unconditional. This used to be guarded by "only if the submitted key differs from
            // the stored one", so re-uploading a file under the same name kept the OLD audio and
            // reported success. With server-generated keys there is no same-name case left.
            String objectKey = StorageKeys.mediaKey(request.file().getOriginalFilename());
            s3ObjectService.uploadStaged(request.bucketName(), objectKey, request.file());
            newMediaObject = s3ObjectService.createMediaObject(request.bucketName(), objectKey);
        }

        S3ObjectEntity newPictureObject = oldPictureObject;
        boolean shouldDeleteOldPicture = false;
        if (request.picture() != null && !request.picture().isEmpty()) {
            newPictureObject = mediaPictureStorageService.uploadPicture(request.picture());
            shouldDeleteOldPicture = oldPictureObject != null
                && !oldPictureObject.getId().equals(newPictureObject.getId());
        }

        Integer durationSeconds;
        if (request.durationSeconds() != null && request.durationSeconds() > 0) {
            durationSeconds = request.durationSeconds();
        } else if (request.file() != null && !request.file().isEmpty()) {
            durationSeconds = mediaDurationService.extractDurationSeconds(request.file());
        } else {
            durationSeconds = existingEntity.getDurationSeconds();
        }

        MediaUpdateRequest mediaRequest = new MediaUpdateRequest(
            request.name(), newMediaObject.getId(), newPictureObject == null ? null : newPictureObject.getId(),
            request.description(), request.categoryId(), request.status(),
            durationSeconds, request.tagIds(), request.requiresPremiumSubscription());
        MediaDto dto = mediaService.update(id, mediaRequest);

        if (!newMediaObject.getId().equals(oldMediaObject.getId())) {
            s3ObjectService.deleteObjectAfterCommit(oldMediaObject);
        }

        if (shouldDeleteOldPicture) {
            s3ObjectService.deleteObjectAfterCommit(oldPictureObject);
        }

        dto.setAverageRating(mediaReviewService.findAverageRatingByMediaId(id));
        return dto;
    }

    @Transactional
    public void delete(Long id) {
        MediaEntity entity = resolveEntity(id);
        S3ObjectEntity mediaObject = entity.getMediaObject();
        S3ObjectEntity pictureObject = entity.getPictureObject();

        mediaService.delete(id);

        s3ObjectService.deleteObjectAfterCommit(mediaObject);
        s3ObjectService.deleteObjectAfterCommit(pictureObject);
    }

    private void enrichWithAverageRating(List<MediaDto> dtos) {
        if (dtos.isEmpty()) {
            return;
        }
        List<Long> ids = dtos.stream().map(MediaDto::getId).toList();
        Map<Long, Double> ratingByMediaId = mediaReviewService.findAverageRatingsByMediaIds(ids).stream()
                .collect(Collectors.toMap(MediaRatingSummary::mediaId, MediaRatingSummary::averageRating));
        dtos.forEach(dto -> dto.setAverageRating(ratingByMediaId.getOrDefault(dto.getId(), 0.0)));
    }

    private MediaEntity resolveEntity(Long id) {
        return mediaRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

}
