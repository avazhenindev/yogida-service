package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaCreateRequest;
import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaUpdateRequest;
import com.yogida.meditation.dto.ObjectMetadataDto;
import com.yogida.meditation.dto.S3ObjectDto;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.enums.MediaLogAction;
import com.yogida.meditation.enums.MediaStatus;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.AdminStorageApi;
import com.yogida.meditation.service.api.MediaApi;
import com.yogida.meditation.service.api.MediaLogApi;
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
    private MediaPictureStorageService mediaPictureStorageService;

    @Mock
    private S3ObjectService s3ObjectService;

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
        S3ObjectEntity mediaObject = s3Object(31L, "audio", "https://acct.r2.cloudflarestorage.com/audio", "focus.mp3");
        S3ObjectEntity pictureObject = s3Object(32L, "pictures", PUBLIC_PICTURE_BASE_URL, "media/generated-cover.png");
        when(s3ObjectService.createMediaObject("audio", "focus.mp3")).thenReturn(mediaObject);
        when(mediaPictureStorageService.uploadPicture(eq(pictureFile))).thenReturn(pictureObject);
        when(mediaApi.create(mediaUpdateRequestCaptor.capture()))
                .thenReturn(new MediaDto(15L, "Focus", "audio", s3ObjectDto(mediaObject), MediaStatus.ACTIVE, "desc", null, null, null, null, null, null));
        when(mediaRepository.findById(15L)).thenReturn(Optional.of(mediaEntity(15L, "Focus", mediaObject, null)));

        mediaFacadeService.create(request);

        assertThat(mediaUpdateRequestCaptor.getValue().mediaObjectId()).isEqualTo(31L);
        assertThat(mediaUpdateRequestCaptor.getValue().pictureObjectId()).isEqualTo(32L);
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
        when(s3ObjectService.createMediaObject("audio", "focus.mp3"))
                .thenReturn(s3Object(31L, "audio", "https://acct.r2.cloudflarestorage.com/audio", "focus.mp3"));
        when(mediaPictureStorageService.uploadPicture(eq(pictureFile)))
                .thenThrow(new IllegalStateException("Public picture base URL is not configured"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mediaFacadeService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Public picture base URL is not configured");
    }

    @Test
    void deleteRemovesLinkedMediaAndPictureObjects() {
        S3ObjectEntity mediaObject = s3Object(31L, "audio", "https://acct.r2.cloudflarestorage.com/audio", "sleep.mp3");
        S3ObjectEntity pictureObject = s3Object(32L, "pictures", PUBLIC_PICTURE_BASE_URL, "media/cover art.png");
        MediaEntity mediaEntity = mediaEntity(
                21L,
                "Sleep",
                mediaObject,
                pictureObject
        );
        when(mediaRepository.findById(21L)).thenReturn(Optional.of(mediaEntity));

        mediaFacadeService.delete(21L);

        verify(s3ObjectService).deleteObjectAfterCommit(mediaObject);
        verify(s3ObjectService).deleteObjectAfterCommit(pictureObject);
        verify(mediaLogApi).log(mediaEntity, MediaLogAction.REMOVED, "Media deleted: Sleep");
        verify(mediaApi).delete(21L);
    }

    private MediaEntity mediaEntity(Long id, String name, S3ObjectEntity mediaObject, S3ObjectEntity pictureObject) {
        MediaEntity entity = new MediaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setMediaObject(mediaObject);
        entity.setPictureObject(pictureObject);
        entity.setBucketName(mediaObject.getBucketName());
        entity.setStatus(MediaStatus.ACTIVE);
        return entity;
    }

    private S3ObjectEntity s3Object(Long id, String bucketName, String baseUrl, String objectUri) {
        S3ObjectEntity object = new S3ObjectEntity();
        object.setId(id);
        object.setBucketName(bucketName);
        object.setBaseUrl(baseUrl);
        object.setObjectUri(objectUri);
        return object;
    }

    private S3ObjectDto s3ObjectDto(S3ObjectEntity object) {
        return new S3ObjectDto(object.getId(), object.getBucketName(), object.getBaseUrl(), object.getObjectUri(), object.getFullUrl());
    }
}




