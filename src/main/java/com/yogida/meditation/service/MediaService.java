package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.mapper.MediaMapper;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.service.api.MediaApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaService implements MediaApi {

    private final MediaRepository mediaRepository;
    private final MediaMapper mediaMapper;

    @Override
    public List<MediaDto> findAll() {
        return mediaRepository.findAll().stream().map(mediaMapper::toDto).toList();
    }

    public void updateStorageEntityObjects(List<MediaEntity> objects) {
        List<MediaEntity> newObjects = objects.stream()
            .filter(this::bucketAndObjectNotExists)
            .toList();
        newObjects.forEach(obj -> log.info("StorageService > New object found - bucket: {}, object: {}", obj.getBucketName(), obj.getName()));
        mediaRepository.saveAll(newObjects);
    }

    private boolean bucketAndObjectNotExists(MediaEntity mediaEntity) {
        return !mediaRepository.existsByBucketNameAndName(mediaEntity.getBucketName(), mediaEntity.getName());
    }

}
