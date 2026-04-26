package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.AppUserControllerApi;
import com.yogida.meditation.dto.AppUserDto;
import com.yogida.meditation.service.AppUserService;
import com.yogida.meditation.service.api.UserFacadeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AppUserController implements AppUserControllerApi {

    private final AppUserService appUserService;
    private final UserFacadeApi userFacadeApi;

    @Override
    public ResponseEntity<List<AppUserDto>> getAll() {
        return ResponseEntity.ok(appUserService.findAll());
    }

    @Override
    public ResponseEntity<AppUserDto> getById(Long id) {
        return ResponseEntity.ok(appUserService.findById(id));
    }

    @Override
    public ResponseEntity<AppUserDto> getByEmail(String email) {
        return ResponseEntity.ok(appUserService.findByEmail(email));
    }

    @Override
    public ResponseEntity<AppUserDto> create(AppUserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userFacadeApi.onboardUser(dto));
    }

    @Override
    public ResponseEntity<AppUserDto> update(Long id, AppUserDto dto) {
        return ResponseEntity.ok(appUserService.update(id, dto));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        appUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

