package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaCreateRequest;
import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaUpdateRequest;
import com.yogida.meditation.dto.ObjectMetadataDto;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaLogAction;
import com.yogida.meditation.enums.MediaStatus;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.AdminStorageApi;
import com.yogida.meditation.service.api.MediaApi;
import com.yogida.meditation.service.api.MediaLogApi;
import com.yogida.meditation.service.api.R2StorageApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaFacadeServiceTest {

    private static final String PUBLIC_PICTURE_BASE_URL = "https://images.example.com";

    @Mock
    private MediaApi mediaApi;

    @Mock
    private MediaLogApi mediaLogApi;

    @Mock
    private AdminStorageApi adminStorageApi;

    @Mock
    private R2StorageApi r2StorageApi;

    @Mock
    private MediaPictureStorageService mediaPictureStorageService;

    @Mock
    private MediaRepository mediaRepository;

    @InjectMocks
    private MediaFacadeService mediaFacadeService;

    @Captor
    private ArgumentCaptor<MediaUpdateRequest> mediaUpdateRequestCaptor;

    @Test
    void createStoresConfiguredPublicPictureUrl() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "focus.mp3",
                "audio/mpeg",
                "audio".getBytes()
        );
        MockMultipartFile pictureFile = new MockMultipartFile(
                "picture",
                "cover image.png",
                "image/png",
                "picture".getBytes()
        );
        MediaCreateRequest request = new MediaCreateRequest(
                "Focus",
                "audio",
                "focus.mp3",
                audioFile,
                "desc",
                7L,
                MediaStatus.ACTIVE,
                pictureFile
        );

        when(adminStorageApi.uploadObject(eq("audio"), eq("focus.mp3"), eq(audioFile)))
                .thenReturn(new ObjectMetadataDto("focus.mp3", 5L, Instant.now(), "etag-audio", "https://acct.r2.cloudflarestorage.com/audio/focus.mp3"));
        when(mediaPictureStorageService.uploadPicture(eq(pictureFile)))
                .thenReturn(PUBLIC_PICTURE_BASE_URL + "/media/generated-cover.png");
        when(mediaApi.create(mediaUpdateRequestCaptor.capture()))
                .thenReturn(new MediaDto(15L, "Focus", "audio", "https://acct.r2.cloudflarestorage.com/audio/focus.mp3", MediaStatus.ACTIVE, "desc", null, null, null, null, null, null));
        when(mediaRepository.findById(15L)).thenReturn(Optional.of(mediaEntity(15L, "Focus", "https://acct.r2.cloudflarestorage.com/audio/focus.mp3", null)));

        mediaFacadeService.create(request);

        assertThat(mediaUpdateRequestCaptor.getValue().picture())
                .isEqualTo(PUBLIC_PICTURE_BASE_URL + "/media/generated-cover.png");
        verify(mediaLogApi).log(any(MediaEntity.class), eq(MediaLogAction.ADDED), eq("Media created: Focus"));
    }

    @Test
    void createPropagatesPictureUploadFailure() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "focus.mp3",
                "audio/mpeg",
                "audio".getBytes()
        );
        MockMultipartFile pictureFile = new MockMultipartFile(
                "picture",
                "cover image.png",
                "image/png",
                "picture".getBytes()
        );
        MediaCreateRequest request = new MediaCreateRequest(
                "Focus",
                "audio",
                "focus.mp3",
                audioFile,
                "desc",
                7L,
                MediaStatus.ACTIVE,
                pictureFile
        );

        when(adminStorageApi.uploadObject(eq("audio"), eq("focus.mp3"), eq(audioFile)))
                .thenReturn(new ObjectMetadataDto("focus.mp3", 5L, Instant.now(), "etag-audio", "https://acct.r2.cloudflarestorage.com/audio/focus.mp3"));
        when(mediaPictureStorageService.uploadPicture(eq(pictureFile)))
                .thenThrow(new IllegalStateException("Public picture base URL is not configured"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mediaFacadeService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Public picture base URL is not configured");
    }

    @Test
    void deleteParsesConfiguredPublicPictureUrlIntoPicturesBucket() {
        MediaEntity mediaEntity = mediaEntity(
                21L,
                "Sleep",
                "https://acct.r2.cloudflarestorage.com/audio/sleep.mp3",
                PUBLIC_PICTURE_BASE_URL + "/media/cover%20art.png"
        );
        when(mediaRepository.findById(21L)).thenReturn(Optional.of(mediaEntity));
        when(r2StorageApi.parseS3Url("https://acct.r2.cloudflarestorage.com/audio/sleep.mp3"))
                .thenReturn(new String[]{"audio", "sleep.mp3"});

        mediaFacadeService.delete(21L);

        verify(adminStorageApi).deleteObject("audio", "sleep.mp3");
        verify(mediaPictureStorageService).deletePictureObjectByUrl(PUBLIC_PICTURE_BASE_URL + "/media/cover%20art.png");
        verify(mediaLogApi).log(mediaEntity, MediaLogAction.REMOVED, "Media deleted: Sleep");
        verify(mediaApi).delete(21L);
    }

    private MediaEntity mediaEntity(Long id, String name, String s3Url, String pictureUrl) {
        MediaEntity entity = new MediaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setS3Url(s3Url);
        entity.setPicture(pictureUrl);
        entity.setBucketName("audio");
        entity.setStatus(MediaStatus.ACTIVE);
        return entity;
    }
}




