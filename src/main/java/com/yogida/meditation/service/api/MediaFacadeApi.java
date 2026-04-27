package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaUpdateRequest;

public interface MediaFacadeApi {

    MediaDto create(MediaUpdateRequest request);

    MediaDto update(Long id, MediaUpdateRequest request);

    void delete(Long id);
}

