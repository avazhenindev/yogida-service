package com.yogida.meditation.service;

import com.yogida.meditation.constants.BucketNames;
import com.yogida.meditation.service.storage.StorageConfigs;
import com.yogida.meditation.service.storage.StorageKeys;
import com.yogida.meditation.entity.S3ObjectEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Handles upload of breathing exercise icons and phase audio files to Cloudflare R2.
 *
 * <p>The two asset types go to different buckets, and that split is the whole point. Icons are
 * decoration: they are rendered in the exercise list for locked exercises too, so they stay in
 * the public bucket and are served by plain URL. Phase audio is the paid artifact and goes to
 * the private bucket, reachable only through a presigned URL issued after an entitlement check.
 *
 * <p>Both used to live in the public bucket, which made gating impossible however the
 * {@code premium} flag was set — anyone holding the URL could fetch the audio, and the URL was
 * handed to every authenticated caller in the exercise listing.
 */
@Service
@RequiredArgsConstructor
public class BreathingStorageService {

    private static final String PUBLIC_BUCKET = BucketNames.PUBLIC;

    private final S3ObjectService s3ObjectService;

    @Value("${cloudflare.r2.public-picture-base-url:}")
    private String publicBaseUrl;

    /** The private bucket holding paid audio. A bucket name, never an endpoint URL. */
    @Value("${cloudflare.r2.bucket:}")
    private String privateBucket;

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
        String key = BucketNames.BREATHING_ICONS_PREFIX + UUID.randomUUID() + "-" + StorageKeys.sanitise(iconFile.getOriginalFilename(), "file");
        s3ObjectService.uploadStaged(PUBLIC_BUCKET, key, iconFile);
        return s3ObjectService.createObject(PUBLIC_BUCKET, StorageConfigs.normalizeBaseUrl(publicBaseUrl), key);
    }

    /**
     * Uploads a single phase audio file to the PRIVATE bucket and persists the S3 object record.
     *
     * <p>The stored base URL is the S3 API endpoint rather than a public domain, because nothing
     * should ever serve this object by plain URL. It is reached only through
     * {@code BreathingUserFacadeService}, which presigns it after checking entitlement.
     *
     * @param audioFile the audio file (must not be null or empty)
     * @return the persisted {@link S3ObjectEntity}
     */
    public S3ObjectEntity uploadAudio(MultipartFile audioFile) {
        String bucket = StorageConfigs.requireBucketName(privateBucket, "cloudflare.r2.bucket", "private audio bucket");
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Audio file must not be empty");
        }
        String key = BucketNames.BREATHING_AUDIO_PREFIX + UUID.randomUUID() + "-" + StorageKeys.sanitise(audioFile.getOriginalFilename(), "file");
        s3ObjectService.uploadStaged(bucket, key, audioFile);
        return s3ObjectService.createObject(bucket, "", key);
    }

    /**
     * Returns the configured private bucket name, rejecting an endpoint URL.
     *
     * <p>The URL check is not paranoia: the local stack shipped with
     * {@code CLOUDFLARE_R2_BUCKET} set to {@code https://<account>.r2.cloudflarestorage.com},
     * which passes a null-or-blank check and then fails at PutObject against a bucket literally
     * named that. Failing here names the actual problem.
     *
     * <p>Thrown as a 500 rather than {@code IllegalStateException}, which
     * {@code GlobalExceptionHandler} maps to a 401 "Authenticated user is not provisioned" —
     * a misconfigured bucket would otherwise read to an administrator as a sign-in problem.
     */
    private void requireConfigured() {
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "cloudflare.r2.public-picture-base-url is not configured");
        }
    }

}
