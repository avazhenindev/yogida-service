package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.ProfileDto;

import java.util.List;

public interface ProfileApi {

    List<ProfileDto> findAll();

    ProfileDto findById(Long id);

    ProfileDto create(ProfileDto dto);

    ProfileDto update(Long id, ProfileDto dto);

    void delete(Long id);
}

