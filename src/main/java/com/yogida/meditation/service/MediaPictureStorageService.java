package com.yogida.meditation.service;

import com.yogida.meditation.constants.BucketNames;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.service.api.AdminStorageApi;
import com.yogida.meditation.service.storage.StorageConfigs;
import com.yogida.meditation.service.storage.StorageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaPictureStorageService {

    private static final String PICTURE_BUCKET_NAME = BucketNames.PUBLIC;
    // Was `"{}/".formatted(BucketNames.PICTURES)`. String.formatted takes %s, not SLF4J's {},
    // and Java's formatter silently ignores surplus arguments — so the prefix evaluated to the
    // literal two characters "{}/" and every picture ever uploaded landed at "{}/<uuid>-<name>".
    private static final String PICTURE_KEY_PREFIX = BucketNames.PICTURES + "/";

    private final AdminStorageApi adminStorageApi;
    private final S3ObjectService s3ObjectService;

    @Value("${app.media.max-picture-size-bytes:512000}")
    private long maxPictureSizeBytes;

    @Value("${cloudflare.r2.public-picture-base-url:}")
    private String publicPictureBaseUrl;

    public S3ObjectEntity uploadPicture(MultipartFile picture) {
        if (picture == null || picture.isEmpty()) {
            return null;
        }

        if (picture.getSize() > maxPictureSizeBytes) {
            throw new IllegalArgumentException("Picture exceeds max size of " + maxPictureSizeBytes + " bytes");
        }

        if (!hasText(publicPictureBaseUrl)) {
            throw new IllegalStateException("Public picture base URL is not configured");
        }

        String pictureObjectKey = PICTURE_KEY_PREFIX + UUID.randomUUID() + "-"
                + StorageKeys.sanitise(picture.getOriginalFilename(), "picture");
        adminStorageApi.uploadObject(PICTURE_BUCKET_NAME, pictureObjectKey, picture);
        s3ObjectService.deleteObjectOnRollback(PICTURE_BUCKET_NAME, pictureObjectKey);
        return s3ObjectService.createObject(PICTURE_BUCKET_NAME, StorageConfigs.normalizeBaseUrl(publicPictureBaseUrl), pictureObjectKey);
    }



    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}


