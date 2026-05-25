package com.yogida.meditation.service;

import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.service.api.AdminStorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaPictureStorageService {

    private static final String PICTURE_BUCKET_NAME = "pictures";
    private static final String PICTURE_KEY_PREFIX = "media/";

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

        String pictureObjectKey = buildPictureObjectKey(picture);
        adminStorageApi.uploadObject(PICTURE_BUCKET_NAME, pictureObjectKey, picture);
        return s3ObjectService.createObject(PICTURE_BUCKET_NAME, normalizeBaseUrl(publicPictureBaseUrl), pictureObjectKey);
    }

    private String buildPictureObjectKey(MultipartFile picture) {
        String originalFilename = picture.getOriginalFilename();
        String filename = hasText(originalFilename) ? originalFilename : "picture";
        filename = filename.substring(Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\')) + 1);
        filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!hasText(filename)) {
            filename = "picture";
        }
        return PICTURE_KEY_PREFIX + UUID.randomUUID() + "-" + filename;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }


    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}


