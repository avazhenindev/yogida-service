package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaRatingSummary;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaRatingEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.MediaRatingRepository;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.MediaRatingApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaRatingService implements MediaRatingApi {

    private final MediaRatingRepository mediaRatingRepository;
    private final MediaRepository mediaRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public void upsertRating(Long mediaId, Long userId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5, got: " + rating);
        }
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media", mediaId));
        AppUserEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("AppUser", userId));

        mediaRatingRepository.findByUserAndMedia(user, media)
                .ifPresentOrElse(
                        existing -> {
                            existing.setRating(rating);
                            existing.setUpdatedAt(LocalDateTime.now());
                            mediaRatingRepository.save(existing);
                            log.info("MediaRatingService > Updated rating id={} for mediaId={} userId={}", existing.getId(), mediaId, userId);
                        },
                        () -> {
                            MediaRatingEntity entity = new MediaRatingEntity();
                            entity.setUser(user);
                            entity.setMedia(media);
                            entity.setRating(rating);
                            entity.setCreatedAt(LocalDateTime.now());
                            entity.setUpdatedAt(LocalDateTime.now());
                            MediaRatingEntity saved = mediaRatingRepository.save(entity);
                            log.info("MediaRatingService > Created rating id={} for mediaId={} userId={}", saved.getId(), mediaId, userId);
                        }
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaRatingSummary> findAverageRatingsByMediaIds(Collection<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mediaRatingRepository.findAverageRatingsByMediaIds(mediaIds);
    }

    @Override
    @Transactional(readOnly = true)
    public double findAverageRatingByMediaId(Long mediaId) {
        return mediaRatingRepository.findAverageRatingByMediaId(mediaId).orElse(0.0);
    }
}
