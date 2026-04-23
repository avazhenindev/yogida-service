package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.SubscriptionDto;

import java.util.List;

public interface SubscriptionApi {

    List<SubscriptionDto> findAll();

    SubscriptionDto findById(Long id);

    SubscriptionDto create(SubscriptionDto dto);

    SubscriptionDto update(Long id, SubscriptionDto dto);

    void delete(Long id);
}
