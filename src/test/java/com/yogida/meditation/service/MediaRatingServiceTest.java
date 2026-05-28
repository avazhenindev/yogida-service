package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaRatingSummary;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaRatingEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.AppUserRepository;
import com.yogida.meditation.repository.MediaRatingRepository;
import com.yogida.meditation.repository.MediaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaRatingServiceTest {

    @Mock
    private MediaRatingRepository mediaRatingRepository;

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private MediaRatingService mediaRatingService;

    // --- upsertRating ---

    @Test
    void upsertRating_createsNewRatingWhenNoneExists() {
        MediaEntity media = mediaEntity(1L);
        AppUserEntity user = appUser(2L);
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(mediaRatingRepository.findByUserAndMedia(user, media)).thenReturn(Optional.empty());
        when(mediaRatingRepository.save(any(MediaRatingEntity.class))).thenAnswer(inv -> {
            MediaRatingEntity e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        mediaRatingService.upsertRating(1L, 2L, 4);

        verify(mediaRatingRepository).save(any(MediaRatingEntity.class));
    }

    @Test
    void upsertRating_updatesExistingRating() {
        MediaEntity media = mediaEntity(1L);
        AppUserEntity user = appUser(2L);
        MediaRatingEntity existing = new MediaRatingEntity();
        existing.setRating(3);
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(mediaRatingRepository.findByUserAndMedia(user, media)).thenReturn(Optional.of(existing));

        mediaRatingService.upsertRating(1L, 2L, 5);

        assertThat(existing.getRating()).isEqualTo(5);
        verify(mediaRatingRepository).save(existing);
    }

    @Test
    void upsertRating_throwsWhenRatingBelowOne() {
        assertThatThrownBy(() -> mediaRatingService.upsertRating(1L, 2L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
    }

    @Test
    void upsertRating_throwsWhenRatingAboveFive() {
        assertThatThrownBy(() -> mediaRatingService.upsertRating(1L, 2L, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
    }

    @Test
    void upsertRating_throwsWhenMediaNotFound() {
        when(mediaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mediaRatingService.upsertRating(99L, 2L, 3))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void upsertRating_throwsWhenUserNotFound() {
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(mediaEntity(1L)));
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mediaRatingService.upsertRating(1L, 99L, 3))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // --- findAverageRatingsByMediaIds ---

    @Test
    void findAverageRatingsByMediaIds_returnsEmptyListForEmptyInput() {
        List<MediaRatingSummary> result = mediaRatingService.findAverageRatingsByMediaIds(List.of());
        assertThat(result).isEmpty();
        verifyNoInteractions(mediaRatingRepository);
    }

    @Test
    void findAverageRatingsByMediaIds_delegatesToRepository() {
        List<MediaRatingSummary> summary = List.of(new MediaRatingSummary(1L, 4.2));
        when(mediaRatingRepository.findAverageRatingsByMediaIds(List.of(1L))).thenReturn(summary);

        List<MediaRatingSummary> result = mediaRatingService.findAverageRatingsByMediaIds(List.of(1L));

        assertThat(result).isEqualTo(summary);
    }

    // --- findAverageRatingByMediaId ---

    @Test
    void findAverageRatingByMediaId_returnsZeroWhenNoRatings() {
        when(mediaRatingRepository.findAverageRatingByMediaId(1L)).thenReturn(Optional.empty());
        assertThat(mediaRatingService.findAverageRatingByMediaId(1L)).isEqualTo(0.0);
    }

    @Test
    void findAverageRatingByMediaId_returnsValueFromRepository() {
        when(mediaRatingRepository.findAverageRatingByMediaId(1L)).thenReturn(Optional.of(3.5));
        assertThat(mediaRatingService.findAverageRatingByMediaId(1L)).isEqualTo(3.5);
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
}
