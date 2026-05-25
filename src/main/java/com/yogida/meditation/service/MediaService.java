package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaUpdateRequest;
import com.yogida.meditation.entity.MediaCategoryEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.enums.MediaStatus;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.MediaMapper;
import com.yogida.meditation.repository.MediaCategoryRepository;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.repository.S3ObjectRepository;
import com.yogida.meditation.service.api.MediaApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaService implements MediaApi {

    private final MediaRepository mediaRepository;
    private final MediaCategoryRepository mediaCategoryRepository;
    private final S3ObjectRepository s3ObjectRepository;
    private final MediaMapper mediaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MediaDto> findAll() {
        return mediaMapper.toDtoList(mediaRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaDto> findAllActive() {
        return mediaMapper.toDtoList(mediaRepository.findAllByStatus(MediaStatus.ACTIVE));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MediaDto> findById(Long id) {
        return mediaRepository.findById(id).map(mediaMapper::toDto);
    }

    @Override
    @Transactional
    public MediaDto create(MediaUpdateRequest request) {
        MediaEntity entity = new MediaEntity();
        entity.setName(request.name());
        S3ObjectEntity mediaObject = resolveS3Object(request.mediaObjectId());
        entity.setMediaObject(mediaObject);
        entity.setBucketName(mediaObject.getBucketName());
        entity.setDescription(request.description());
        entity.setPictureObject(resolveS3ObjectOrNull(request.pictureObjectId()));
        entity.setCategory(resolveCategory(request.categoryId()));
        entity.setStatus(request.status() != null ? request.status() : MediaStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        return mediaMapper.toDto(mediaRepository.save(entity));
    }

    @Override
    @Transactional
    public MediaDto update(Long id, MediaUpdateRequest request) {
        MediaEntity entity = findEntityById(id);
        entity.setName(request.name());
        S3ObjectEntity mediaObject = resolveS3Object(request.mediaObjectId());
        entity.setMediaObject(mediaObject);
        entity.setBucketName(mediaObject.getBucketName());
        entity.setDescription(request.description());
        entity.setPictureObject(resolveS3ObjectOrNull(request.pictureObjectId()));
        entity.setCategory(resolveCategory(request.categoryId()));
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return mediaMapper.toDto(mediaRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!mediaRepository.existsById(id)) {
            throw new EntityNotFoundException("Media", id);
        }
        mediaRepository.deleteById(id);
        log.info("MediaService > Deleted media id={}", id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, MediaStatus status) {
        MediaEntity entity = findEntityById(id);
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        mediaRepository.save(entity);
    }

    /** Package-visible helper used by the scheduler. */
    @Transactional(readOnly = true)
    public List<MediaEntity> findAllEntities() {
        return mediaRepository.findAllWithMediaObjectBy();
    }

    private MediaEntity findEntityById(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

    private MediaCategoryEntity resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return mediaCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("MediaCategory", categoryId));
    }

    private S3ObjectEntity resolveS3Object(Long s3ObjectId) {
        return s3ObjectRepository.findById(s3ObjectId)
                .orElseThrow(() -> new EntityNotFoundException("S3Object", s3ObjectId));
    }

    private S3ObjectEntity resolveS3ObjectOrNull(Long s3ObjectId) {
        if (s3ObjectId == null) {
            return null;
        }
        return resolveS3Object(s3ObjectId);
    }
}
