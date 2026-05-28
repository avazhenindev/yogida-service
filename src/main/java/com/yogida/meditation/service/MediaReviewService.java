package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaRatingSummaryResponse;
import com.yogida.meditation.dto.MediaReviewResponse;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaReviewEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.MediaRatingRepository;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.repository.MediaReviewRepository;
import com.yogida.meditation.service.api.MediaReviewApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaReviewService implements MediaReviewApi {

    private final MediaReviewRepository mediaReviewRepository;
    private final MediaRepository mediaRepository;
    private final AppUserRepository appUserRepository;
    private final MediaRatingRepository mediaRatingRepository;

    @Override
    @Transactional
    public MediaReviewResponse upsertReview(Long mediaId, Long userId, String reviewText) {
        if (reviewText == null || reviewText.isBlank()) {
            throw new IllegalArgumentException("Review text must not be blank");
        }
        if (reviewText.length() > 2000) {
            throw new IllegalArgumentException("Review text must not exceed 2000 characters");
        }
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media", mediaId));
        AppUserEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("AppUser", userId));

        return mediaReviewRepository.findByUserAndMedia(user, media)
                .map(existing -> {
                    existing.setReviewText(reviewText);
                    existing.setUpdatedAt(LocalDateTime.now());
                    MediaReviewEntity saved = mediaReviewRepository.save(existing);
                    log.info("MediaReviewService > Updated review id={} for mediaId={} userId={}", saved.getId(), mediaId, userId);
                    return toResponse(saved);
                })
                .orElseGet(() -> {
                    MediaReviewEntity entity = new MediaReviewEntity();
                    entity.setUser(user);
                    entity.setMedia(media);
                    entity.setReviewText(reviewText);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    MediaReviewEntity saved = mediaReviewRepository.save(entity);
                    log.info("MediaReviewService > Created review id={} for mediaId={} userId={}", saved.getId(), mediaId, userId);
                    return toResponse(saved);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MediaReviewResponse> findReviewsByMediaId(Long mediaId, Pageable pageable) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media", mediaId));
        return mediaReviewRepository.findAllByMediaOrderByCreatedAtDesc(media, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MediaReviewResponse> findUserReview(Long mediaId, Long userId) {
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media", mediaId));
        AppUserEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("AppUser", userId));
        return mediaReviewRepository.findByUserAndMedia(user, media)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaRatingSummaryResponse getRatingSummary(Long mediaId) {
        if (!mediaRepository.existsById(mediaId)) {
            throw new EntityNotFoundException("Media", mediaId);
        }
        double avgRating = mediaRatingRepository.findAverageRatingByMediaId(mediaId).orElse(0.0);
        long count = mediaRatingRepository.countByMediaId(mediaId);
        return new MediaRatingSummaryResponse(mediaId, avgRating, count);
    }

    private MediaReviewResponse toResponse(MediaReviewEntity entity) {
        return new MediaReviewResponse(
                entity.getId(),
                entity.getMedia().getId(),
                entity.getUser().getUserId(),
                entity.getReviewText(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
