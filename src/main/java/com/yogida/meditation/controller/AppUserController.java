package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.AppUserControllerApi;
import com.yogida.meditation.dto.AppUserDto;
import com.yogida.meditation.mapper.AppUserMapper;
import com.yogida.meditation.service.AppUserService;
import com.yogida.meditation.service.CurrentUserService;
import com.yogida.meditation.service.api.UserFacadeApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AppUserController implements AppUserControllerApi {

    private final AppUserService appUserService;
    private final UserFacadeApi userFacadeApi;
    private final CurrentUserService currentUserService;
    private final AppUserMapper appUserMapper;

    /**
     * The only user endpoint a client application needs.
     *
     * <p>Everything below it is administrative and now says so. These were reachable with any
     * mobile token, which meant the whole user directory could be listed, any account looked
     * up by email, and any account modified or deleted. Signing in provisions the caller's
     * record, so no client has a reason to reach the others.
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AppUserDto> getCurrentUser() {
        return ResponseEntity.ok(appUserMapper.toDto(currentUserService.getCurrentUserOrThrow()));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppUserDto>> getAll() {
        return ResponseEntity.ok(appUserService.findAll());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppUserDto> getById(Long id) {
        return ResponseEntity.ok(appUserService.findById(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppUserDto> getByEmail(String email) {
        return ResponseEntity.ok(appUserService.findByEmail(email));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppUserDto> create(AppUserDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userFacadeApi.onboardUser(dto));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppUserDto> update(Long id, AppUserDto dto) {
        return ResponseEntity.ok(appUserService.update(id, dto));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(Long id) {
        appUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

