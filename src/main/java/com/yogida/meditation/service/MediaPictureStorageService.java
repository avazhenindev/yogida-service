package com.yogida.meditation.service;

import com.yogida.meditation.constants.BucketNames;
import com.yogida.meditation.entity.S3ObjectEntity;
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

    private final S3ObjectService s3ObjectService;

    @Value("${app.media.max-picture-size-bytes:512000}")
    private long maxPictureSizeBytes;

    @Value("${cloudflare.r2.public-picture-base-url:}")
    private String publicPictureBaseUrl;

    /**
     * Rejects an oversized picture.
     *
     * <p>Public because callers need it BEFORE any upload happens: the facade uploads the media
     * audio first, so leaving the only check in {@link #uploadPicture} meant an oversized picture
     * was caught after that object had already been written to R2 and had to be rolled back.
     *
     * <p>The check previously existed twice — here and in AdminMediaController, each with its own
     * @Value binding of the same property and its own wording. This is the controller's message,
     * since that is the one this path has been returning.
     */
    public void validateSize(MultipartFile picture) {
        if (picture != null && picture.getSize() > maxPictureSizeBytes) {
            throw new IllegalArgumentException(
                String.format("Picture size exceeds maximum allowed size of %d bytes", maxPictureSizeBytes)
            );
        }
    }

    public S3ObjectEntity uploadPicture(MultipartFile picture) {
        if (picture == null || picture.isEmpty()) {
            return null;
        }

        validateSize(picture);

        if (!hasText(publicPictureBaseUrl)) {
            throw new IllegalStateException("Public picture base URL is not configured");
        }

        String pictureObjectKey = PICTURE_KEY_PREFIX + UUID.randomUUID() + "-"
                + StorageKeys.sanitise(picture.getOriginalFilename(), "picture");
        s3ObjectService.uploadStaged(PICTURE_BUCKET_NAME, pictureObjectKey, picture);
        return s3ObjectService.createObject(PICTURE_BUCKET_NAME, StorageConfigs.normalizeBaseUrl(publicPictureBaseUrl), pictureObjectKey);
    }



    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}


