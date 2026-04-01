package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaSubscriptionDto;

import java.util.List;

public interface MediaSubscriptionApi {

    List<MediaSubscriptionDto> findAll();

    MediaSubscriptionDto findById(Long id);

    MediaSubscriptionDto create(MediaSubscriptionDto dto);

    MediaSubscriptionDto update(Long id, MediaSubscriptionDto dto);

    void delete(Long id);
}

