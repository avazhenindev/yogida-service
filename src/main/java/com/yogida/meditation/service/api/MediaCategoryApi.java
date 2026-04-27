package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaCategoryCreateRequest;
import com.yogida.meditation.dto.MediaCategoryDto;
import com.yogida.meditation.dto.MediaCategoryUpdateRequest;

import java.util.List;

public interface MediaCategoryApi {

    List<MediaCategoryDto> findAll();

    MediaCategoryDto findById(Long id);

    MediaCategoryDto create(MediaCategoryCreateRequest request);

    MediaCategoryDto update(Long id, MediaCategoryUpdateRequest request);

    void delete(Long id);
}

