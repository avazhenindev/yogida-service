package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.AdminMediaControllerApi;
import com.yogida.meditation.dto.*;
import com.yogida.meditation.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.yogida.meditation.service.MediaFacadeService;

@RestController
@RequiredArgsConstructor
public class AdminMediaController implements AdminMediaControllerApi {

    private final MediaFacadeService mediaFacadeService;


    @Override
    public ResponseEntity<List<MediaDto>> getAll() {
        return ResponseEntity.ok(mediaFacadeService.findAll());
    }

    @Override
    public ResponseEntity<MediaDto> getById(Long id) {
        return mediaFacadeService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

    @Override
    public ResponseEntity<MediaDto> create(MediaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaFacadeService.create(request));
    }

    @Override
    public ResponseEntity<MediaDto> update(Long id, MediaFileUpdateRequest request) {
        return ResponseEntity.ok(mediaFacadeService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        mediaFacadeService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
