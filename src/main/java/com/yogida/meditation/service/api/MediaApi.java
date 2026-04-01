package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.entity.MediaEntity;

import java.util.List;

public interface MediaApi {

    List<MediaDto> findAll();

    void updateStorageEntityObjects(List<MediaEntity> objects);
}
