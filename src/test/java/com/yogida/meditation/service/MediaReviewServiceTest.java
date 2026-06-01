package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaReviewResponse;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaReviewEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.AppUserRepository;
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

    @InjectMocks
    private MediaReviewService mediaReviewService;

    // --- save (first save — rating + text) ---

    @Test
    void save_createsNewReviewWithRatingAndText() {
        MediaEntity media = mediaEntity(1L);
        AppUserEntity user = appUser(2L, "alice@example.com");
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

        MediaReviewResponse response = mediaReviewService.save(1L, 2L, 4, "Great meditation!");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.rating()).isEqualTo(4);
        assertThat(response.reviewText()).isEqualTo("Great meditation!");
        assertThat(response.userName()).isEqualTo("alice");
        assertThat(response.userInitial()).isEqualTo("A");
        verify(mediaReviewRepository).save(any(MediaReviewEntity.class));
    }

    @Test
    void save_updatesRatingButPreservesExistingText() {
        MediaEntity media = mediaEntity(1L);
        AppUserEntity user = appUser(2L, "bob@example.com");
        MediaReviewEntity existing = reviewEntity(5L, user, media, 3, "Original text");
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(mediaReviewRepository.findByUserAndMedia(user, media)).thenReturn(Optional.of(existing));
        when(mediaReviewRepository.save(existing)).thenReturn(existing);

        MediaReviewResponse response = mediaReviewService.save(1L, 2L, 5, "New text ignored");

        assertThat(existing.getRating()).isEqualTo(5);
        assertThat(existing.getReviewText()).isEqualTo("Original text");
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.reviewText()).isEqualTo("Original text");
    }

    @Test
    void save_ratingOnlyRow() {
        MediaEntity media = mediaEntity(1L);
        AppUserEntity user = appUser(2L, "carol@example.com");
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(mediaReviewRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());
        when(mediaReviewRepository.save(any(MediaReviewEntity.class))).thenAnswer(inv -> {
            MediaReviewEntity e = inv.getArgument(0);
            e.setId(11L);
            e.setMedia(media);
            e.setUser(user);
            return e;
        });

        MediaReviewResponse response = mediaReviewService.save(1L, 2L, 5, null);

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.reviewText()).isNull();
    }

    @Test
    void save_throwsWhenRatingOutOfRange() {
        assertThatThrownBy(() -> mediaReviewService.save(1L, 2L, 6, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
    }

    @Test
    void save_throwsWhenMediaNotFound() {
        when(mediaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mediaReviewService.save(99L, 2L, 3, "text"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void save_throwsWhenUserNotFound() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(mediaEntity(1L)));
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mediaReviewService.save(1L, 99L, 3, "text"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- findReviewsByMediaId ---

    @Test
    void findReviewsByMediaId_returnsEmptyPage() {
        when(mediaRepository.existsById(1L)).thenReturn(true);
        when(mediaReviewRepository.findAllByMediaId(1L, PageRequest.of(0, 10)))
                .thenReturn(Page.empty());

        Page<MediaReviewResponse> page = mediaReviewService.findReviewsByMediaId(1L, PageRequest.of(0, 10));

        assertThat(page).isEmpty();
    }

    @Test
    void findReviewsByMediaId_returnsMappedReviews() {
        MediaEntity media = mediaEntity(1L);
        AppUserEntity user = appUser(2L, "dave@example.com");
        MediaReviewEntity review = reviewEntity(7L, user, media, 5, "Wonderful!");
        when(mediaRepository.existsById(1L)).thenReturn(true);
        when(mediaReviewRepository.findAllByMediaId(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(review)));

        Page<MediaReviewResponse> page = mediaReviewService.findReviewsByMediaId(1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).reviewText()).isEqualTo("Wonderful!");
        assertThat(page.getContent().get(0).userName()).isEqualTo("dave");
    }

    // --- Helpers ---

    private MediaEntity mediaEntity(Long id) {
        MediaEntity e = new MediaEntity();
        e.setId(id);
        return e;
    }

    private AppUserEntity appUser(Long id, String email) {
        AppUserEntity u = new AppUserEntity();
        u.setUserId(id);
        u.setEmail(email);
        return u;
    }

    private MediaReviewEntity reviewEntity(Long id, AppUserEntity user, MediaEntity media, Integer rating, String text) {
        MediaReviewEntity e = new MediaReviewEntity();
        e.setId(id);
        e.setUser(user);
        e.setMedia(media);
        e.setRating(rating);
        e.setReviewText(text);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }
}
