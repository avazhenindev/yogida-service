package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.MediaControllerApi;
import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.service.api.MediaApi;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MediaController implements MediaControllerApi {

    private final MediaApi mediaApi;
    private final R2StorageApi r2StorageApi;

    @Override
    public ResponseEntity<List<MediaDto>> getAll() {
        return ResponseEntity.ok(mediaApi.findAllActive());
    }

    @Override
    public ResponseEntity<MediaDto> getById(Long id) {
        return mediaApi.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

    @Override
    public ResponseEntity<Map<String, String>> getStreamUrl(Long id) {
        MediaDto media = mediaApi.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Media", id));
        String url = r2StorageApi.generateStreamingUrlFromS3Url(media.getS3Url());
        return ResponseEntity.ok(Map.of("url", url));
    }
}

