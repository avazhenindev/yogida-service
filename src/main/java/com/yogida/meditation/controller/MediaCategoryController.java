package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.MediaCategoryControllerApi;
import com.yogida.meditation.dto.MediaCategoryCreateRequest;
import com.yogida.meditation.dto.MediaCategoryDto;
import com.yogida.meditation.dto.MediaCategoryUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.yogida.meditation.service.MediaCategoryService;

@RestController
@RequiredArgsConstructor
public class MediaCategoryController implements MediaCategoryControllerApi {

    private final MediaCategoryService mediaCategoryService;

    @Override
    public ResponseEntity<List<MediaCategoryDto>> getAll() {
        return ResponseEntity.ok(mediaCategoryService.findAll());
    }

    @Override
    public ResponseEntity<MediaCategoryDto> getById(Long id) {
        return ResponseEntity.ok(mediaCategoryService.findById(id));
    }

    @Override
    public ResponseEntity<MediaCategoryDto> create(MediaCategoryCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mediaCategoryService.create(request));
    }

    @Override
    public ResponseEntity<MediaCategoryDto> update(Long id, MediaCategoryUpdateRequest request) {
        return ResponseEntity.ok(mediaCategoryService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        mediaCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

