package com.yogida.meditation.service;

import com.yogida.meditation.dto.S3ObjectDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Service for secure media streaming with entitlement verification.
 * Verifies that the authenticated user has access to the media before returning a streaming URL.
 */
@Service
@RequiredArgsConstructor
public class SecureStreamService {

    private final MediaRepository mediaRepository;
    private final CurrentUserService currentUserService;
    private final EntitlementService entitlementService;
    private final R2StorageApi r2StorageApi;

    /**
     * Generates a presigned streaming URL for a media object with entitlement verification.
     * Returns 403 Forbidden if the user is not entitled to access the media.
     *
     * @param s3ObjectDto the S3 object details (bucketName, objectUri required)
     * @return map containing the presigned URL under "url" key
     * @throws ResponseStatusException with 403 Forbidden if user is not entitled
     */
    @Transactional(readOnly = true)
    public Map<String, String> generateSecureStreamingUrl(S3ObjectDto s3ObjectDto) {
        AppUserEntity currentUser = currentUserService.getCurrentUserOrThrow();

        // Find the media that owns this S3 object
        MediaEntity media = findMediaByObjectUri(s3ObjectDto.getObjectUri())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));

        // Verify entitlement
        if (!entitlementService.isEntitled(currentUser, media)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not entitled to access this media");
        }

        // Generate and return presigned URL
        String url = r2StorageApi.generateStreamingUrl(
            s3ObjectDto.getBucketName(),
            s3ObjectDto.getObjectUri()
        );
        return Map.of("url", url);
    }

    /**
     * Find a media entity by its S3 object URI.
     * This is a helper method that can be extracted to a repository method if reused.
     *
     * @param objectUri the S3 object URI
     * @return Optional containing the media entity if found
     */
    @Transactional(readOnly = true)
    private java.util.Optional<MediaEntity> findMediaByObjectUri(String objectUri) {
        // Get all media and filter by media object URI
        // In a real scenario, this could be optimized with a direct repository query
        return mediaRepository.findAll().stream()
            .filter(m -> m.getMediaObject() != null && 
                        objectUri.equals(m.getMediaObject().getObjectUri()))
            .findFirst();
    }
}
