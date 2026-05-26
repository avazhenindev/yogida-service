package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.MediaControllerApi;
import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.S3ObjectDto;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.service.api.MediaFacadeApi;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MediaController implements MediaControllerApi {

    private final MediaFacadeApi mediaFacadeApi;
    private final R2StorageApi r2StorageApi;

    @Override
    public ResponseEntity<List<MediaDto>> getAll() {
        return ResponseEntity.ok(mediaFacadeApi.findAllActive());
    }

    @Override
    public ResponseEntity<MediaDto> getById(Long id) {
        return mediaFacadeApi.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

    @Override
    public ResponseEntity<Map<String, String>> getStreamUrl(S3ObjectDto s3ObjectDto) {
        String url = r2StorageApi.generateStreamingUrl(
            s3ObjectDto.getBucketName(),
            s3ObjectDto.getObjectUri()
        );
        return ResponseEntity.ok(Map.of("url", url));
    }
}
