package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.AdminBreathingControllerApi;
import com.yogida.meditation.dto.*;
import com.yogida.meditation.service.BreathingAudioMigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import com.yogida.meditation.service.BreathingService;

@RestController
@RequiredArgsConstructor
public class AdminBreathingController implements AdminBreathingControllerApi {

    private final BreathingService breathingService;
    private final BreathingAudioMigrationService breathingAudioMigrationService;

    @Override
    public ResponseEntity<List<BreathingDto>> getAll() {
        return ResponseEntity.ok(breathingService.findAll());
    }

    @Override
    public ResponseEntity<BreathingDto> getById(Long id) {
        return ResponseEntity.ok(breathingService.findById(id));
    }

    @Override
    public ResponseEntity<BreathingDto> create(BreathingCreateRequest meta, MultipartFile iconFile,
                                               List<MultipartFile> audioFiles) {
        return ResponseEntity.status(HttpStatus.CREATED).body(breathingService.create(meta, iconFile, audioFiles));
    }

    @Override
    public ResponseEntity<BreathingDto> update(Long id, BreathingUpdateRequest meta, MultipartFile iconFile,
                                               List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(breathingService.update(id, meta, iconFile, audioFiles));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        breathingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> reorder(List<BreathingReorderItem> items) {
        breathingService.reorder(items);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<BreathingDto> addAudio(Long phaseId, MultipartFile audioFile) {
        return ResponseEntity.ok(breathingService.addAudioToPhase(phaseId, audioFile));
    }

    @Override
    public ResponseEntity<BreathingDto> removeAudio(Long phaseId, Long audioObjectId) {
        return ResponseEntity.ok(breathingService.removeAudioFromPhase(phaseId, audioObjectId));
    }

    @Override
    public ResponseEntity<BreathingAudioMigrationResult> migrateAudioToPrivateBucket(boolean dryRun) {
        return ResponseEntity.ok(breathingAudioMigrationService.migrateToPrivateBucket(dryRun));
    }
}
