package com.yogida.meditation.service;

import com.yogida.meditation.constants.BucketNames;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.service.api.AdminStorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Handles upload of breathing exercise icons and phase audio files to Cloudflare R2.
 * Both asset types are stored in the public R2 bucket and served via public URL (no presigning).
 */
@Service
@RequiredArgsConstructor
public class BreathingStorageService {

    private static final String BUCKET_NAME = BucketNames.PUBLIC;

    private final AdminStorageApi adminStorageApi;
    private final S3ObjectService s3ObjectService;

    @Value("${cloudflare.r2.public-picture-base-url:}")
    private String publicBaseUrl;

    /**
     * Uploads a breathing exercise icon to {@code public/breathing/icons/} and persists
     * the S3 object record.
     *
     * @param iconFile the icon image file (must not be null or empty)
     * @return the persisted {@link S3ObjectEntity} with a resolvable public URL
     */
    public S3ObjectEntity uploadIcon(MultipartFile iconFile) {
        requireConfigured();
        if (iconFile == null || iconFile.isEmpty()) {
            throw new IllegalArgumentException("Icon file must not be empty");
        }
        String key = BucketNames.BREATHING_ICONS_PREFIX + UUID.randomUUID() + "-" + sanitizeFilename(iconFile);
        adminStorageApi.uploadObject(BUCKET_NAME, key, iconFile);
        return s3ObjectService.createObject(BUCKET_NAME, normalizeBaseUrl(publicBaseUrl), key);
    }

    /**
     * Uploads a single phase audio file to {@code public/breathing/audio/} and persists
     * the S3 object record.
     *
     * @param audioFile the audio file (must not be null or empty)
     * @return the persisted {@link S3ObjectEntity}
     */
    public S3ObjectEntity uploadAudio(MultipartFile audioFile) {
        requireConfigured();
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Audio file must not be empty");
        }
        String key = BucketNames.BREATHING_AUDIO_PREFIX + UUID.randomUUID() + "-" + sanitizeFilename(audioFile);
        adminStorageApi.uploadObject(BUCKET_NAME, key, audioFile);
        return s3ObjectService.createObject(BUCKET_NAME, normalizeBaseUrl(publicBaseUrl), key);
    }

    private void requireConfigured() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new IllegalStateException("Public picture base URL is not configured");
        }
    }

    private String sanitizeFilename(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || name.isBlank()) {
            return "file";
        }
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isBlank() ? "file" : name;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
