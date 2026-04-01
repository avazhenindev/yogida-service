package com.yogida.meditation.controller;


import com.yogida.meditation.controller.api.MeditationControllerApi;
import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.service.MediaService;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MeditationController implements MeditationControllerApi {

    private final R2StorageApi r2StorageApi;
    private final MediaService mediaService;

    @Override
    public Map<String, String> getStreamLink(String bucketName, String mediaName) {
        String url = r2StorageApi.generateStreamingUrl(bucketName, mediaName);
        return Map.of("url", url);
    }

    @Override
    public ResponseEntity<Map<String, List<MediaDto>>> getAllStorageObject() {
        return ResponseEntity.ok(
            mediaService.findAll().stream().collect(
                Collectors.groupingBy(
                    media -> media.getBucketName(),
                    Collectors.mapping(media -> media, Collectors.toList())
                ))
        );
    }

}
