package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaUpdateRequest;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaStatus;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.MediaMapper;
import com.yogida.meditation.repository.MediaRepository;
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
        entity.setS3Url(request.s3Url());
        entity.setDescription(request.description());
        entity.setCategory(request.category());
        entity.setStatus(request.status() != null ? request.status() : MediaStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        return mediaMapper.toDto(mediaRepository.save(entity));
    }

    @Override
    @Transactional
    public MediaDto update(Long id, MediaUpdateRequest request) {
        MediaEntity entity = findEntityById(id);
        entity.setName(request.name());
        entity.setS3Url(request.s3Url());
        entity.setDescription(request.description());
        entity.setCategory(request.category());
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
        return mediaRepository.findAll();
    }

    private MediaEntity findEntityById(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }
}
