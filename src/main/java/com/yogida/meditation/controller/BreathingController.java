package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.BreathingControllerApi;
import com.yogida.meditation.dto.BreathingDto;
import com.yogida.meditation.service.api.BreathingApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BreathingController implements BreathingControllerApi {

    private final BreathingApi breathingApi;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BreathingDto>> getAll() {
        return ResponseEntity.ok(breathingApi.findAll());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BreathingDto> getById(Long id) {
        return ResponseEntity.ok(breathingApi.findById(id));
    }
}
