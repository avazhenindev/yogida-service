package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.AdminMediaControllerApi;
import com.yogida.meditation.dto.*;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.service.api.MediaFacadeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminMediaController implements AdminMediaControllerApi {

    private final MediaFacadeApi mediaFacadeApi;

    @Value("${app.media.max-picture-size-bytes:512000}")
    private long maxPictureSizeBytes;

    @Override
    public ResponseEntity<List<MediaDto>> getAll() {
        return ResponseEntity.ok(mediaFacadeApi.findAll());
    }

    @Override
    public ResponseEntity<MediaDto> getById(Long id) {
        return mediaFacadeApi.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

    @Override
    public ResponseEntity<MediaDto> create(MediaCreateRequest request) {
        validatePictureSize(request.picture());
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaFacadeApi.create(request));
    }

    @Override
    public ResponseEntity<MediaDto> update(Long id, MediaFileUpdateRequest request) {
        validatePictureSize(request.picture());
        return ResponseEntity.ok(mediaFacadeApi.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        mediaFacadeApi.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validatePictureSize(MultipartFile picture) {
        if (picture != null && picture.getSize() > maxPictureSizeBytes) {
            throw new IllegalArgumentException(
                String.format("Picture size exceeds maximum allowed size of %d bytes", maxPictureSizeBytes)
            );
        }
    }
}
