package com.yogida.meditation.service;

import com.yogida.meditation.service.api.AdminStorageApi;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaPictureStorageService {

    private static final String PICTURE_BUCKET_NAME = "pictures";
    private static final String PICTURE_KEY_PREFIX = "media/";

    private final AdminStorageApi adminStorageApi;
    private final R2StorageApi r2StorageApi;

    @Value("${app.media.max-picture-size-bytes:512000}")
    private long maxPictureSizeBytes;

    @Value("${cloudflare.r2.public-picture-base-url:}")
    private String publicPictureBaseUrl;

    public String uploadPicture(MultipartFile picture) {
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
        return buildPublicPictureUrl(pictureObjectKey);
    }

    public Optional<String[]> parsePictureObjectUrl(String objectUrl) {
        if (!hasText(objectUrl)) {
            return Optional.empty();
        }

        if (hasText(publicPictureBaseUrl)) {
            String baseUrl = normalizeBaseUrl(publicPictureBaseUrl);
            String prefix = baseUrl + "/";
            if (objectUrl.startsWith(prefix)) {
                String objectKey = stripUrlSuffix(objectUrl.substring(prefix.length()));
                if (hasText(objectKey)) {
                    return Optional.of(new String[]{PICTURE_BUCKET_NAME, URLDecoder.decode(objectKey, StandardCharsets.UTF_8)});
                }
            }
        }

        return Optional.of(r2StorageApi.parseS3Url(objectUrl));
    }

    public void deletePictureObjectByUrl(String objectUrl) {
        if (!hasText(objectUrl)) {
            return;
        }

        try {
            parsePictureObjectUrl(objectUrl)
                    .ifPresent(parts -> adminStorageApi.deleteObject(parts[0], parts[1]));
        } catch (Exception e) {
            log.warn("Failed to delete picture object [url={}]: {}", objectUrl, e.getMessage());
        }
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

    private String buildPublicPictureUrl(String objectKey) {
        return normalizeBaseUrl(publicPictureBaseUrl) + "/" + objectKey;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String stripUrlSuffix(String value) {
        int queryIndex = value.indexOf('?');
        int fragmentIndex = value.indexOf('#');
        int endIndex = value.length();
        if (queryIndex >= 0) {
            endIndex = queryIndex;
        }
        if (fragmentIndex >= 0) {
            endIndex = Math.min(endIndex, fragmentIndex);
        }
        return value.substring(0, endIndex);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}


