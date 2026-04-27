package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.MediaCategoryControllerApi;
import com.yogida.meditation.dto.MediaCategoryCreateRequest;
import com.yogida.meditation.dto.MediaCategoryDto;
import com.yogida.meditation.dto.MediaCategoryUpdateRequest;
import com.yogida.meditation.service.api.MediaCategoryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MediaCategoryController implements MediaCategoryControllerApi {

    private final MediaCategoryApi mediaCategoryApi;

    @Override
    public ResponseEntity<List<MediaCategoryDto>> getAll() {
        return ResponseEntity.ok(mediaCategoryApi.findAll());
    }

    @Override
    public ResponseEntity<MediaCategoryDto> getById(Long id) {
        return ResponseEntity.ok(mediaCategoryApi.findById(id));
    }

    @Override
    public ResponseEntity<MediaCategoryDto> create(MediaCategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaCategoryApi.create(request));
    }

    @Override
    public ResponseEntity<MediaCategoryDto> update(Long id, MediaCategoryUpdateRequest request) {
        return ResponseEntity.ok(mediaCategoryApi.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        mediaCategoryApi.delete(id);
        return ResponseEntity.noContent().build();
    }
}

