package com.yogida.meditation.service;

import com.yogida.meditation.dto.ObjectMetadataDto;
import com.yogida.meditation.service.api.AdminStorageApi;
import com.yogida.meditation.service.api.R2StorageApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaPictureStorageServiceTest {

    private static final String PUBLIC_PICTURE_BASE_URL = "https://images.example.com";

    @Mock
    private AdminStorageApi adminStorageApi;

    @Mock
    private R2StorageApi r2StorageApi;

    @InjectMocks
    private MediaPictureStorageService mediaPictureStorageService;

    @Captor
    private ArgumentCaptor<String> pictureObjectKeyCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaPictureStorageService, "maxPictureSizeBytes", 512_000L);
        ReflectionTestUtils.setField(mediaPictureStorageService, "publicPictureBaseUrl", PUBLIC_PICTURE_BASE_URL);
    }

    @Test
    void uploadPictureStoresConfiguredPublicPictureUrl() {
        MockMultipartFile pictureFile = new MockMultipartFile(
                "picture",
                "cover image.png",
                "image/png",
                "picture".getBytes()
        );

        when(adminStorageApi.uploadObject(eq("pictures"), pictureObjectKeyCaptor.capture(), eq(pictureFile)))
                .thenReturn(new ObjectMetadataDto("ignored", 7L, Instant.now(), "etag-picture", "https://acct.r2.cloudflarestorage.com/pictures/legacy.png"));

        String savedPictureUrl = mediaPictureStorageService.uploadPicture(pictureFile);

        String uploadedPictureKey = pictureObjectKeyCaptor.getValue();
        assertThat(uploadedPictureKey)
                .startsWith("media/")
                .endsWith("-cover_image.png");
        assertThat(savedPictureUrl).isEqualTo(PUBLIC_PICTURE_BASE_URL + "/" + uploadedPictureKey);
    }

    @Test
    void uploadPictureFailsWhenPublicPictureBaseUrlMissing() {
        ReflectionTestUtils.setField(mediaPictureStorageService, "publicPictureBaseUrl", "");

        MockMultipartFile pictureFile = new MockMultipartFile(
                "picture",
                "cover image.png",
                "image/png",
                "picture".getBytes()
        );

        assertThatThrownBy(() -> mediaPictureStorageService.uploadPicture(pictureFile))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Public picture base URL is not configured");
    }

    @Test
    void parsePictureObjectUrlParsesPublicPictureUrl() {
        Optional<String[]> parts = mediaPictureStorageService.parsePictureObjectUrl(
                PUBLIC_PICTURE_BASE_URL + "/media/cover%20art.png?version=1#header"
        );

        assertThat(parts).isPresent();
        assertThat(parts.get()).containsExactly("pictures", "media/cover art.png");
    }

    @Test
    void parsePictureObjectUrlFallsBackToLegacyUrlParsing() {
        String legacyUrl = "https://acct.r2.cloudflarestorage.com/pictures/media/cover.png";
        when(r2StorageApi.parseS3Url(legacyUrl)).thenReturn(new String[]{"pictures", "media/cover.png"});

        Optional<String[]> parts = mediaPictureStorageService.parsePictureObjectUrl(legacyUrl);

        assertThat(parts).isPresent();
        assertThat(parts.get()).containsExactly("pictures", "media/cover.png");
        verify(r2StorageApi).parseS3Url(legacyUrl);
    }

    @Test
    void deletePictureObjectByUrlDeletesParsedObject() {
        mediaPictureStorageService.deletePictureObjectByUrl(PUBLIC_PICTURE_BASE_URL + "/media/deep-focus.png");

        verify(adminStorageApi).deleteObject("pictures", "media/deep-focus.png");
        verify(r2StorageApi, never()).parseS3Url(PUBLIC_PICTURE_BASE_URL + "/media/deep-focus.png");
    }
}

