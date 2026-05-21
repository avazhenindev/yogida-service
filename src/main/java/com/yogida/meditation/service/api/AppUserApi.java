package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.AppUserDto;

import java.util.List;

public interface AppUserApi {

    List<AppUserDto> findAll();

    AppUserDto findById(Long id);

    AppUserDto create(AppUserDto dto);

    AppUserDto update(Long id, AppUserDto dto);

    void delete(Long id);

    AppUserDto findByEmail(String email);
}

