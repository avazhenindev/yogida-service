package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.ProfileControllerApi;
import com.yogida.meditation.dto.ProfileDto;
import com.yogida.meditation.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProfileController implements ProfileControllerApi {

    private final ProfileService profileService;

    @Override
    public ResponseEntity<List<ProfileDto>> getAll() {
        return ResponseEntity.ok(profileService.findAll());
    }

    @Override
    public ResponseEntity<ProfileDto> getById(Long id) {
        return ResponseEntity.ok(profileService.findById(id));
    }

    @Override
    public ResponseEntity<ProfileDto> create(ProfileDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.create(dto));
    }

    @Override
    public ResponseEntity<ProfileDto> update(Long id, ProfileDto dto) {
        return ResponseEntity.ok(profileService.update(id, dto));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        profileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

