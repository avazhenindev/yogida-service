package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaReviewResponse;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaReviewEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.MediaRatingRepository;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.repository.MediaReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaReviewServiceTest {

    @Mock
    private MediaReviewRepository mediaReviewRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private MediaRatingRepository mediaRatingRepository;

    @InjectMocks
    private MediaReviewService mediaReviewService;

    // --- upsertReview ---

    @Test
    void upsertReview_createsNewReviewWhenNoneExists() {
        MediaEntity media = mediaEntity(1L);
        AppUserEntity user = appUser(2L);
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(mediaReviewRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());
        when(mediaReviewRepository.save(any(MediaReviewEntity.class))).thenAnswer(inv -> {
            MediaReviewEntity e = inv.getArgument(0);
            e.setId(10L);
            e.setMedia(media);
            e.setUser(user);
            return e;
        });

        MediaReviewResponse response = mediaReviewService.upsertReview(1L, 2L, "Great meditation!");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.reviewText()).isEqualTo("Great meditation!");
        verify(mediaReviewRepository).save(any(MediaReviewEntity.class));
    }

    @Test
    void upsertReview_updatesExistingReview() {
        MediaEntity media = mediaEntity(1L);
        AppUserEntity user = appUser(2L);
        MediaReviewEntity existing = reviewEntity(5L, user, media, "Old text");
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(mediaReviewRepository.findByUserAndMedia(user, media)).thenReturn(Optional.of(existing));
        when(mediaReviewRepository.save(existing)).thenReturn(existing);

        MediaReviewResponse response = mediaReviewService.upsertReview(1L, 2L, "Updated text");

        assertThat(existing.getReviewText()).isEqualTo("Updated text");
        assertThat(response.reviewText()).isEqualTo("Updated text");
    }

    @Test
    void upsertReview_throwsOnBlankText() {
        assertThatThrownBy(() -> mediaReviewService.upsertReview(1L, 2L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void upsertReview_throwsOnNullText() {
        assertThatThrownBy(() -> mediaReviewService.upsertReview(1L, 2L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upsertReview_throwsOnTextExceedingMaxLength() {
        String longText = "a".repeat(2001);
        assertThatThrownBy(() -> mediaReviewService.upsertReview(1L, 2L, longText))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2000");
    }

    @Test
    void upsertReview_throwsWhenMediaNotFound() {
        when(mediaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mediaReviewService.upsertReview(99L, 2L, "text"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void upsertReview_throwsWhenUserNotFound() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(mediaEntity(1L)));
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mediaReviewService.upsertReview(1L, 99L, "text"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- findReviewsByMediaId ---

    @Test
    void findReviewsByMediaId_returnsEmptyPage() {
        MediaEntity media = mediaEntity(1L);
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(mediaReviewRepository.findAllByMediaOrderByCreatedAtDesc(media, PageRequest.of(0, 20)))
                .thenReturn(Page.empty());

        Page<MediaReviewResponse> page = mediaReviewService.findReviewsByMediaId(1L, PageRequest.of(0, 20));

        assertThat(page).isEmpty();
    }

    @Test
    void findReviewsByMediaId_returnsMappedReviews() {
        MediaEntity media = mediaEntity(1L);
        AppUserEntity user = appUser(2L);
        MediaReviewEntity review = reviewEntity(7L, user, media, "Wonderful!");
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(mediaReviewRepository.findAllByMediaOrderByCreatedAtDesc(media, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(review)));

        Page<MediaReviewResponse> page = mediaReviewService.findReviewsByMediaId(1L, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).reviewText()).isEqualTo("Wonderful!");
    }

    // --- Helpers ---

    private MediaEntity mediaEntity(Long id) {
        MediaEntity e = new MediaEntity();
        e.setId(id);
        return e;
    }

    private AppUserEntity appUser(Long id) {
        AppUserEntity u = new AppUserEntity();
        u.setUserId(id);
        return u;
    }

    private MediaReviewEntity reviewEntity(Long id, AppUserEntity user, MediaEntity media, String text) {
        MediaReviewEntity e = new MediaReviewEntity();
        e.setId(id);
        e.setUser(user);
        e.setMedia(media);
        e.setReviewText(text);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }
}
