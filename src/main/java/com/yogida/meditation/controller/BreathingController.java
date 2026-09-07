package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.BreathingControllerApi;
import com.yogida.meditation.dto.BreathingDto;
import com.yogida.meditation.service.BreathingUserFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BreathingController implements BreathingControllerApi {

    // The user-facing facade, not BreathingApi: BreathingApi is the admin read path and
    // returns every audio URL unconditionally.
    private final BreathingUserFacadeService breathingUserFacadeService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BreathingDto>> getAll() {
        return ResponseEntity.ok(breathingUserFacadeService.findAllForCurrentUser());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BreathingDto> getById(Long id) {
        return ResponseEntity.ok(breathingUserFacadeService.findByIdForCurrentUser(id));
    }
}
