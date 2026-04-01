package com.yogida.meditation.scheduler;

import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.service.R2StorageService;
import com.yogida.meditation.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
@EnableScheduling
public class Scheduler {

    private final R2StorageService storageService;
    private final MediaService mediaService;

    // Scheduler which runs each 5 minutes
    @Scheduled(fixedDelay = 300000)
    public void runEvery5Minutes() {
        log.info("Scheduler > Running every 5 minutes to fetch and update all buckets with objects");
        Map<String, List<String>> allBucketsWithObjects = storageService.getAllBucketsWithObjects();
        List<MediaEntity> entities = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : allBucketsWithObjects.entrySet()) {
            String bucketName = entry.getKey();
            entry.getValue().forEach(objectKey -> {
                MediaEntity mediaEntity = new MediaEntity();
                mediaEntity.setBucketName(bucketName);
                mediaEntity.setName(objectKey);
                mediaEntity.setCreatedAt(LocalDateTime.now());
                entities.add(mediaEntity);
            });
        }
        mediaService.updateStorageEntityObjects(entities);
    }
}
