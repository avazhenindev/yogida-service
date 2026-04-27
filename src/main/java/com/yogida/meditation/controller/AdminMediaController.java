package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.AdminMediaControllerApi;
import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaLogDto;
import com.yogida.meditation.dto.MediaUpdateRequest;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.service.api.MediaApi;
import com.yogida.meditation.service.api.MediaLogApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminMediaController implements AdminMediaControllerApi {

    private final MediaApi mediaApi;
    private final MediaLogApi mediaLogApi;

    @Override
    public ResponseEntity<List<MediaDto>> getAll() {
        return ResponseEntity.ok(mediaApi.findAll());
    }

    @Override
    public ResponseEntity<MediaDto> getById(Long id) {
        return mediaApi.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

    @Override
    public ResponseEntity<MediaDto> create(MediaUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaApi.create(request));
    }

    @Override
    public ResponseEntity<MediaDto> update(Long id, MediaUpdateRequest request) {
        return ResponseEntity.ok(mediaApi.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        mediaApi.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<MediaLogDto>> getLogs(Long id) {
        return ResponseEntity.ok(mediaLogApi.findByMediaId(id));
    }
}

