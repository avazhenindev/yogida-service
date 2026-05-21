package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaSubscriptionDto;
import com.yogida.meditation.entity.MediaSubscriptionEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.MediaSubscriptionMapper;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.repository.MediaSubscriptionRepository;
import com.yogida.meditation.repository.SubscriptionRepository;
import com.yogida.meditation.service.api.MediaSubscriptionApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaSubscriptionService implements MediaSubscriptionApi {

    private final MediaSubscriptionRepository mediaSubscriptionRepository;
    private final MediaRepository mediaRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MediaSubscriptionMapper mediaSubscriptionMapper;

    @Override
    public List<MediaSubscriptionDto> findAll() {
        return mediaSubscriptionRepository.findAll().stream()
                .map(mediaSubscriptionMapper::toDto).toList();
    }

    @Override
    public MediaSubscriptionDto findById(Long id) {
        return mediaSubscriptionRepository.findById(id)
                .map(mediaSubscriptionMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("MediaSubscription", id));
    }

    @Override
    public MediaSubscriptionDto create(MediaSubscriptionDto dto) {
        validateMediaExists(dto.getMediaId());
        validateSubscriptionExists(dto.getSubscriptionId());
        MediaSubscriptionEntity entity = mediaSubscriptionMapper.toEntity(dto);
        entity.setMediaSubscriptionId(null);
        entity.setCreatedAt(LocalDateTime.now());
        MediaSubscriptionEntity saved = mediaSubscriptionRepository.save(entity);
        log.info("MediaSubscriptionService > Created media-subscription with id: {}", saved.getMediaSubscriptionId());
        return mediaSubscriptionMapper.toDto(saved);
    }

    @Override
    public MediaSubscriptionDto update(Long id, MediaSubscriptionDto dto) {
        MediaSubscriptionEntity existing = mediaSubscriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MediaSubscription", id));
        if (dto.getMediaId() != null) {
            validateMediaExists(dto.getMediaId());
        }
        if (dto.getSubscriptionId() != null) {
            validateSubscriptionExists(dto.getSubscriptionId());
        }
        mediaSubscriptionMapper.updateEntity(dto, existing);
        MediaSubscriptionEntity saved = mediaSubscriptionRepository.save(existing);
        log.info("MediaSubscriptionService > Updated media-subscription with id: {}", saved.getMediaSubscriptionId());
        return mediaSubscriptionMapper.toDto(saved);
    }

    @Override
    public void delete(Long id) {
        if (!mediaSubscriptionRepository.existsById(id)) {
            throw new EntityNotFoundException("MediaSubscription", id);
        }
        mediaSubscriptionRepository.deleteById(id);
        log.info("MediaSubscriptionService > Deleted media-subscription with id: {}", id);
    }

    private void validateMediaExists(Long mediaId) {
        if (mediaId == null || !mediaRepository.existsById(mediaId)) {
            throw new EntityNotFoundException("Media", mediaId);
        }
    }

    private void validateSubscriptionExists(Long subscriptionId) {
        if (subscriptionId == null || !subscriptionRepository.existsById(subscriptionId)) {
            throw new EntityNotFoundException("Subscription", subscriptionId);
        }
    }
}

