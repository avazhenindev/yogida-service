package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.MediaSubscriptionControllerApi;
import com.yogida.meditation.dto.MediaSubscriptionDto;
import com.yogida.meditation.service.MediaSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MediaSubscriptionController implements MediaSubscriptionControllerApi {

    private final MediaSubscriptionService mediaSubscriptionService;

    @Override
    public ResponseEntity<List<MediaSubscriptionDto>> getAll() {
        return ResponseEntity.ok(mediaSubscriptionService.findAll());
    }

    @Override
    public ResponseEntity<MediaSubscriptionDto> getById(Long id) {
        return ResponseEntity.ok(mediaSubscriptionService.findById(id));
    }

    @Override
    public ResponseEntity<MediaSubscriptionDto> create(MediaSubscriptionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaSubscriptionService.create(dto));
    }

    @Override
    public ResponseEntity<MediaSubscriptionDto> update(Long id, MediaSubscriptionDto dto) {
        return ResponseEntity.ok(mediaSubscriptionService.update(id, dto));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        mediaSubscriptionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

