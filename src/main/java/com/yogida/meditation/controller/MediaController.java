package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.MediaControllerApi;
import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.S3ObjectDto;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.service.SecureStreamService;
import com.yogida.meditation.service.UserMediaFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MediaController implements MediaControllerApi {

    private final UserMediaFacadeService userMediaFacadeService;
    private final SecureStreamService secureStreamService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MediaDto>> getAll() {
        return ResponseEntity.ok(userMediaFacadeService.findAllActive());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MediaDto> getById(Long id) {
        return userMediaFacadeService.findById(id)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getStreamUrl(S3ObjectDto s3ObjectDto) {
        return ResponseEntity.ok(secureStreamService.generateSecureStreamingUrl(s3ObjectDto));
    }
}
