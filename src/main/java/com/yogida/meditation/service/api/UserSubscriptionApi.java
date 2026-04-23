package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.UserSubscriptionDto;

import java.util.List;

public interface UserSubscriptionApi {

    List<UserSubscriptionDto> findAll();

    UserSubscriptionDto findById(Long id);

    UserSubscriptionDto create(UserSubscriptionDto dto);

    UserSubscriptionDto update(Long id, UserSubscriptionDto dto);

    void delete(Long id);
}

