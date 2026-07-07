package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaRatingSummary;
import com.yogida.meditation.dto.MediaRatingSummaryResponse;
import com.yogida.meditation.dto.MediaReviewResponse;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaReviewEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.AppUserRepository;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaReviewService implements MediaReviewApi {

    private final MediaReviewRepository mediaReviewRepository;
    private final MediaRepository mediaRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public MediaReviewResponse save(Long mediaId, Long userId, Integer rating, String reviewText) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Rating must be between 1 and 5, got: " + rating);
        }
        MediaEntity media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new EntityNotFoundException("Media", mediaId));
        AppUserEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("AppUser", userId));

        MediaReviewEntity entity = mediaReviewRepository.findByUserAndMedia(user, media)
                .orElseGet(() -> {
                    MediaReviewEntity e = new MediaReviewEntity();
                    e.setUser(user);
                    e.setMedia(media);
                    e.setCreatedAt(LocalDateTime.now());
                    return e;
                });

        boolean isNew = entity.getId() == null;

        if (rating != null) {
            entity.setRating(rating);
        }
        // reviewText is immutable after creation — only set when the entity has no text yet
        if (entity.getReviewText() == null && reviewText != null && !reviewText.isBlank()) {
            entity.setReviewText(reviewText);
        }
        entity.setUpdatedAt(LocalDateTime.now());
        MediaReviewEntity saved = mediaReviewRepository.save(entity);
        log.info("MediaReviewService > {} review id={} for mediaId={} userId={}",
                isNew ? "Created" : "Updated", saved.getId(), mediaId, userId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MediaReviewResponse> findReviewsByMediaId(Long mediaId, Pageable pageable) {
        if (!mediaRepository.existsById(mediaId)) {
            throw new EntityNotFoundException("Media", mediaId);
        }
        return mediaReviewRepository.findAllByMediaId(mediaId, pageable)
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
        double avgRating = mediaReviewRepository.findAverageRatingByMediaId(mediaId).orElse(0.0);
        long count = mediaReviewRepository.countRatingsByMediaId(mediaId);

        Map<Integer, Long> breakdown = mediaReviewRepository
                .countByMediaIdGroupByRating(mediaId)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).longValue()
                ));
        for (int i = 1; i <= 5; i++) {
            breakdown.putIfAbsent(i, 0L);
        }
        return new MediaRatingSummaryResponse(mediaId, avgRating, count, breakdown);
    }

    @Override
    @Transactional(readOnly = true)
    public double findAverageRatingByMediaId(Long mediaId) {
        return mediaReviewRepository.findAverageRatingByMediaId(mediaId).orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaRatingSummary> findAverageRatingsByMediaIds(Collection<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return Collections.emptyList();
        }
        return mediaReviewRepository.findAverageRatingsByMediaIds(mediaIds);
    }

    private MediaReviewResponse toResponse(MediaReviewEntity entity) {
        String email = entity.getUser().getEmail();
        String userName = (email != null && email.contains("@"))
                ? email.substring(0, email.indexOf('@'))
                : (email != null ? email : "user");
        String userInitial = userName.isEmpty() ? "?" : String.valueOf(userName.charAt(0)).toUpperCase();

        return new MediaReviewResponse(
                entity.getId(),
                entity.getMedia().getId(),
                entity.getUser().getUserId(),
                userName,
                userInitial,
                entity.getRating(),
                entity.getReviewText(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
