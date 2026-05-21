package com.yogida.meditation.scheduler;

import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaLogAction;
import com.yogida.meditation.enums.MediaStatus;
import com.yogida.meditation.service.MediaLogService;
import com.yogida.meditation.service.MediaService;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.List;

/**
 * Periodically verifies that every MediaEntity's S3 object is reachable.
 * Sets status to ERROR (and logs it) when an object is missing or unreachable.
 * Recovers status to ACTIVE when the object is found again.
 */
@Log4j2
@Component
@RequiredArgsConstructor
@EnableScheduling
public class Scheduler {

    private final R2StorageApi r2StorageApi;
    private final MediaService mediaService;
    private final MediaLogService mediaLogService;

    @Scheduled(fixedDelayString = "${app.scheduler.health-check-delay-ms:300000}")
    public void verifyMediaHealth() {
        List<MediaEntity> allMedia = mediaService.findAllEntities();
        log.info("Scheduler > Starting S3 health check for {} media records", allMedia.size());

        for (MediaEntity media : allMedia) {
            try {
                String[] parts = r2StorageApi.parseS3Url(media.getS3Url());
                String bucket = parts[0];
                String key = parts[1];

                boolean exists = r2StorageApi.objectExists(bucket, key);

                if (!exists) {
                    if (media.getStatus() != MediaStatus.ERROR) {
                        mediaService.updateStatus(media.getId(), MediaStatus.ERROR);
                        mediaLogService.log(media, MediaLogAction.ERROR,
                                "Object not found in S3: " + media.getS3Url());
                        log.warn("Scheduler > Object not found — marked ERROR: id={} s3Url={}", media.getId(), media.getS3Url());
                    }
                } else {
                    if (media.getStatus() == MediaStatus.ERROR) {
                        mediaService.updateStatus(media.getId(), MediaStatus.ACTIVE);
                        mediaLogService.log(media, MediaLogAction.UPDATED,
                                "Object recovered in S3: " + media.getS3Url());
                        log.info("Scheduler > Object recovered — restored ACTIVE: id={}", media.getId());
                    }
                }

            } catch (S3Exception e) {
                log.error("Scheduler > S3 error checking media id={}: {}", media.getId(), e.getMessage(), e);
                if (media.getStatus() != MediaStatus.ERROR) {
                    mediaService.updateStatus(media.getId(), MediaStatus.ERROR);
                    mediaLogService.log(media, MediaLogAction.ERROR,
                            "S3 error during health check: " + e.getMessage());
                }
            } catch (IllegalArgumentException e) {
                log.error("Scheduler > Cannot parse s3Url for media id={}: {}", media.getId(), e.getMessage());
            }
        }

        log.info("Scheduler > S3 health check complete");
    }


}
