package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.AdminMediaControllerApi;
import com.yogida.meditation.dto.*;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.service.api.MediaApi;
import com.yogida.meditation.service.api.MediaFacadeApi;
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
    private final MediaFacadeApi mediaFacadeApi;
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
    public ResponseEntity<MediaDto> create(MediaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaFacadeApi.create(request));
    }

    @Override
    public ResponseEntity<MediaDto> update(Long id, MediaFileUpdateRequest request) {
        return ResponseEntity.ok(mediaFacadeApi.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        mediaFacadeApi.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> bulkDelete(MediaBulkDeleteRequest request) {
        mediaFacadeApi.bulkDelete(request);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<MediaLogDto>> getLogs(Long id) {
        return ResponseEntity.ok(mediaLogApi.findByMediaId(id));
    }
}
