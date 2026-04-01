package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.FavouriteDto;

import java.util.List;

public interface FavouriteApi {

    List<FavouriteDto> findAll();

    FavouriteDto findById(Long id);

    FavouriteDto create(FavouriteDto dto);

    FavouriteDto update(Long id, FavouriteDto dto);

    void delete(Long id);
}

