package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.AdminBreathingControllerApi;
import com.yogida.meditation.dto.*;
import com.yogida.meditation.service.BreathingAudioMigrationService;
import com.yogida.meditation.service.api.BreathingApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminBreathingController implements AdminBreathingControllerApi {

    private final BreathingApi breathingApi;
    private final BreathingAudioMigrationService breathingAudioMigrationService;

    @Override
    public ResponseEntity<List<BreathingDto>> getAll() {
        return ResponseEntity.ok(breathingApi.findAll());
    }

    @Override
    public ResponseEntity<BreathingDto> getById(Long id) {
        return ResponseEntity.ok(breathingApi.findById(id));
    }

    @Override
    public ResponseEntity<BreathingDto> create(BreathingCreateRequest meta, MultipartFile iconFile,
                                               List<MultipartFile> audioFiles) {
        return ResponseEntity.status(HttpStatus.CREATED).body(breathingApi.create(meta, iconFile, audioFiles));
    }

    @Override
    public ResponseEntity<BreathingDto> update(Long id, BreathingUpdateRequest meta, MultipartFile iconFile,
                                               List<MultipartFile> audioFiles) {
        return ResponseEntity.ok(breathingApi.update(id, meta, iconFile, audioFiles));
    }

    @Override
    public ResponseEntity<Void> delete(Long id) {
        breathingApi.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> reorder(List<BreathingReorderItem> items) {
        breathingApi.reorder(items);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<BreathingDto> addAudio(Long phaseId, MultipartFile audioFile) {
        return ResponseEntity.ok(breathingApi.addAudioToPhase(phaseId, audioFile));
    }

    @Override
    public ResponseEntity<BreathingDto> removeAudio(Long phaseId, Long audioObjectId) {
        return ResponseEntity.ok(breathingApi.removeAudioFromPhase(phaseId, audioObjectId));
    }

    @Override
    public ResponseEntity<BreathingAudioMigrationResult> migrateAudioToPrivateBucket(boolean dryRun) {
        return ResponseEntity.ok(breathingAudioMigrationService.migrateToPrivateBucket(dryRun));
    }
}
