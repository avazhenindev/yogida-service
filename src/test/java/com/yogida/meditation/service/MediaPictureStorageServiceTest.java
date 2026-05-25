package com.yogida.meditation.service;

import com.yogida.meditation.dto.ObjectMetadataDto;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.service.api.AdminStorageApi;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaPictureStorageServiceTest {

    private static final String PUBLIC_PICTURE_BASE_URL = "https://images.example.com";

    @Mock
    private AdminStorageApi adminStorageApi;

    @Mock
    private S3ObjectService s3ObjectService;

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

        when(s3ObjectService.createObject(eq("pictures"), eq(PUBLIC_PICTURE_BASE_URL), pictureObjectKeyCaptor.capture()))
                .thenAnswer(invocation -> {
                    S3ObjectEntity entity = new S3ObjectEntity();
                    entity.setId(44L);
                    entity.setBucketName("pictures");
                    entity.setBaseUrl(PUBLIC_PICTURE_BASE_URL);
                    entity.setObjectUri(invocation.getArgument(2, String.class));
                    return entity;
                });

        S3ObjectEntity savedPictureObject = mediaPictureStorageService.uploadPicture(pictureFile);

        String uploadedPictureKey = pictureObjectKeyCaptor.getValue();
        assertThat(uploadedPictureKey)
                .startsWith("media/")
                .endsWith("-cover_image.png");
        assertThat(savedPictureObject.getBucketName()).isEqualTo("pictures");
        assertThat(savedPictureObject.getFullUrl()).isEqualTo(PUBLIC_PICTURE_BASE_URL + "/" + uploadedPictureKey);
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

}

