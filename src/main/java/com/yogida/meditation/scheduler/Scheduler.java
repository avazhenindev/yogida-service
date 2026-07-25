package com.yogida.meditation.scheduler;

import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.enums.MediaStatus;
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
 * Sets status to ERROR when an object is missing or unreachable.
 * Recovers status to ACTIVE when the object is found again.
 */
@Log4j2
@Component
@RequiredArgsConstructor
@EnableScheduling
public class Scheduler {

    private final R2StorageApi r2StorageApi;
    private final MediaService mediaService;

    @Scheduled(fixedDelayString = "${app.scheduler.health-check-delay-ms:300000}")
    public void verifyMediaHealth() {
        List<MediaEntity> allMedia = mediaService.findAllEntities();
        log.info("Scheduler > Starting S3 health check for {} media records", allMedia.size());

        for (MediaEntity media : allMedia) {
            try {
                S3ObjectEntity mediaObject = media.getMediaObject();
                String bucket = mediaObject.getBucketName();
                String key = mediaObject.getObjectUri();
                String mediaUrl = mediaObject.getFullUrl();

                boolean exists = r2StorageApi.objectExists(bucket, key);

                if (!exists) {
                    if (media.getStatus() != MediaStatus.ERROR) {
                        mediaService.updateStatus(media.getId(), MediaStatus.ERROR);
                        log.warn("Scheduler > Object not found — marked ERROR: id={} s3Url={}", media.getId(), mediaUrl);
                    }
                } else {
                    if (media.getStatus() == MediaStatus.ERROR) {
                        mediaService.updateStatus(media.getId(), MediaStatus.ACTIVE);
                        log.info("Scheduler > Object recovered — restored ACTIVE: id={}", media.getId());
                    }
                }

            } catch (S3Exception e) {
                log.error("Scheduler > S3 error checking media id={}: {}", media.getId(), e.getMessage(), e);
                if (media.getStatus() != MediaStatus.ERROR) {
                    mediaService.updateStatus(media.getId(), MediaStatus.ERROR);
                }
            } catch (IllegalArgumentException e) {
                log.error("Scheduler > Cannot resolve media object for media id={}: {}", media.getId(), e.getMessage());
            }
        }

        log.info("Scheduler > S3 health check complete");
    }
}
