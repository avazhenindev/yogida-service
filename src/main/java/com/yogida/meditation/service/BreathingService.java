package com.yogida.meditation.service;

import com.yogida.meditation.dto.*;
import com.yogida.meditation.entity.BreathingEntity;
import com.yogida.meditation.entity.BreathingPhaseAudioEntity;
import com.yogida.meditation.entity.BreathingPhaseEntity;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.exception.BreathingNotFoundException;
import com.yogida.meditation.mapper.BreathingMapper;
import com.yogida.meditation.repository.BreathingPhaseAudioRepository;
import com.yogida.meditation.repository.BreathingPhaseRepository;
import com.yogida.meditation.repository.BreathingRepository;
import com.yogida.meditation.service.api.BreathingApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class BreathingService implements BreathingApi {

    private final BreathingRepository breathingRepository;
    private final BreathingPhaseRepository breathingPhaseRepository;
    private final BreathingPhaseAudioRepository breathingPhaseAudioRepository;
    private final BreathingStorageService breathingStorageService;
    private final S3ObjectService s3ObjectService;
    private final BreathingMapper breathingMapper;

    @Override
    @Transactional(readOnly = true)
    public List<BreathingDto> findAll() {
        return breathingMapper.toDtoList(breathingRepository.findAllByOrderByDisplayOrderAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public BreathingDto findById(Long id) {
        return breathingMapper.toDto(loadOrThrow(id));
    }

    @Override
    @Transactional
    public BreathingDto create(BreathingCreateRequest request, MultipartFile iconFile, List<MultipartFile> audioFiles) {
        S3ObjectEntity iconObject = breathingStorageService.uploadIcon(iconFile);

        BreathingEntity entity = new BreathingEntity();
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setColor(request.color());
        entity.setCycles(request.cycles());
        entity.setPremium(Boolean.TRUE.equals(request.isPremium()));
        entity.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        entity.setIconObject(iconObject);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        BreathingEntity saved = breathingRepository.save(entity);

        if (request.phases() != null) {
            for (int i = 0; i < request.phases().size(); i++) {
                BreathingPhaseCreateRequest req = request.phases().get(i);
                BreathingPhaseEntity phase = buildPhase(req, i, saved);
                breathingPhaseRepository.save(phase);
                attachAudioFiles(phase, req, audioFiles);
            }
        }

        breathingRepository.flush();
        log.info("BreathingService > Created breathing exercise id={}", saved.getId());
        return breathingMapper.toDto(loadOrThrow(saved.getId()));
    }

    @Override
    @Transactional
    public BreathingDto update(Long id, BreathingUpdateRequest request, MultipartFile iconFile,
                               List<MultipartFile> audioFiles) {
        BreathingEntity entity = loadOrThrow(id);

        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.color() != null) entity.setColor(request.color());
        if (request.cycles() != null) entity.setCycles(request.cycles());
        if (request.isPremium() != null) entity.setPremium(request.isPremium());
        if (request.displayOrder() != null) entity.setDisplayOrder(request.displayOrder());

        if (iconFile != null && !iconFile.isEmpty()) {
            S3ObjectEntity oldIcon = entity.getIconObject();
            entity.setIconObject(breathingStorageService.uploadIcon(iconFile));
            s3ObjectService.deleteObjectAfterCommit(oldIcon);
        }

        if (request.phases() != null) {
            List<BreathingPhaseEntity> existingPhases = new ArrayList<>(entity.getPhases());

            // Determine which existing phase IDs are still present in the request
            Set<Long> keptIds = request.phases().stream()
                    .filter(r -> r.id() != null)
                    .map(BreathingPhaseCreateRequest::id)
                    .collect(Collectors.toSet());

            // Delete phases that were removed by the user (cascade deletes their audio rows via DB FK)
            existingPhases.stream()
                    .filter(p -> !keptIds.contains(p.getId()))
                    .forEach(p -> {
                        // The referencing rows must be gone and flushed BEFORE the object
                        // cleanup is registered: it counts remaining referents to decide whether
                        // the s3_object row can go too, and would otherwise still see this one.
                        List<S3ObjectEntity> orphanCandidates = p.getAudioFiles().stream()
                                .map(BreathingPhaseAudioEntity::getAudioObject)
                                .filter(java.util.Objects::nonNull)
                                .toList();
                        entity.getPhases().remove(p);
                        breathingPhaseRepository.delete(p);
                        breathingPhaseRepository.flush();
                        orphanCandidates.forEach(s3ObjectService::deleteObjectAfterCommit);
                    });

            // Process each phase in the request (ordered by request position = new displayOrder)
            for (int i = 0; i < request.phases().size(); i++) {
                BreathingPhaseCreateRequest req = request.phases().get(i);

                if (req.id() != null) {
                    // Update existing phase in-place — preserves all audio not explicitly removed
                    BreathingPhaseEntity existing = existingPhases.stream()
                            .filter(p -> p.getId().equals(req.id()))
                            .findFirst()
                            .orElseThrow(() -> new BreathingNotFoundException(
                                    "Phase not found with id: " + req.id()));

                    existing.setName(req.name());
                    existing.setLabel(req.label( ));
                    existing.setDurationSeconds(req.durationSeconds());
                    existing.setColor(req.color());
                    existing.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : i);
                    breathingPhaseRepository.save(existing);

                    // Remove audio files the user deleted
                    if (req.audioObjectIdsToRemove() != null) {
                        for (Long audioObjId : req.audioObjectIdsToRemove()) {
                            breathingPhaseAudioRepository
                                    .findByPhaseIdAndAudioObjectId(existing.getId(), audioObjId)
                                    .ifPresent(entry -> {
                                        // Same ordering requirement as above.
                                        S3ObjectEntity orphanCandidate = entry.getAudioObject();
                                        breathingPhaseAudioRepository.delete(entry);
                                        breathingPhaseAudioRepository.flush();
                                        s3ObjectService.deleteObjectAfterCommit(orphanCandidate);
                                    });
                        }
                    }

                    // Add new audio files
                    attachAudioFiles(existing, req, audioFiles);
                } else {
                    // New phase
                    BreathingPhaseEntity newPhase = buildPhase(req, i, entity);
                    breathingPhaseRepository.save(newPhase);
                    attachAudioFiles(newPhase, req, audioFiles);
                }
            }
        }

        entity.setUpdatedAt(LocalDateTime.now());
        breathingRepository.flush();
        log.info("BreathingService > Updated breathing exercise id={}", id);
        return breathingMapper.toDto(loadOrThrow(id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BreathingEntity entity = loadOrThrow(id);

        // Collect all audio S3 objects before cascade delete wipes the join records
        List<S3ObjectEntity> audioObjects = entity.getPhases().stream()
                .flatMap(p -> p.getAudioFiles().stream())
                .map(BreathingPhaseAudioEntity::getAudioObject)
                .toList();

        breathingRepository.delete(entity);

        s3ObjectService.deleteObjectAfterCommit(entity.getIconObject());
        audioObjects.forEach(s3ObjectService::deleteObjectAfterCommit);
        log.info("BreathingService > Deleted breathing exercise id={}, cleaning {} audio object(s) from R2", id, audioObjects.size());
    }

    @Override
    @Transactional
    public void reorder(List<BreathingReorderItem> items) {
        for (BreathingReorderItem item : items) {
            BreathingEntity entity = loadOrThrow(item.id());
            entity.setDisplayOrder(item.displayOrder());
            breathingRepository.save(entity);
        }
        log.info("BreathingService > Reordered {} breathing exercises", items.size());
    }

    @Override
    @Transactional
    public BreathingDto addAudioToPhase(Long phaseId, MultipartFile audioFile) {
        BreathingPhaseEntity phase = findPhaseOrThrow(phaseId);
        S3ObjectEntity audioObject = breathingStorageService.uploadAudio(audioFile);

        // Save via the owning-side repository — the inverse collection (phase.audioFiles)
        // is not the owning side, so mutations to it are ignored by Hibernate.
        BreathingPhaseAudioEntity audioEntry = new BreathingPhaseAudioEntity();
        audioEntry.setPhase(phase);
        audioEntry.setAudioObject(audioObject);
        breathingPhaseAudioRepository.save(audioEntry);

        // Reload the exercise so the returned DTO reflects the newly persisted audio
        breathingRepository.flush();
        log.info("BreathingService > Added audio to phase id={}", phaseId);
        return breathingMapper.toDto(loadOrThrow(phase.getBreathing().getId()));
    }

    @Override
    @Transactional
    public BreathingDto removeAudioFromPhase(Long phaseId, Long audioObjectId) {
        BreathingPhaseEntity phase = findPhaseOrThrow(phaseId);

        BreathingPhaseAudioEntity audioEntry = breathingPhaseAudioRepository
                .findByPhaseIdAndAudioObjectId(phaseId, audioObjectId)
                .orElseThrow(() -> new BreathingNotFoundException(
                        "Audio object " + audioObjectId + " not found on phase " + phaseId));

        S3ObjectEntity audioObject = audioEntry.getAudioObject();
        breathingPhaseAudioRepository.delete(audioEntry);
        breathingRepository.flush();
        s3ObjectService.deleteObjectAfterCommit(audioObject);

        log.info("BreathingService > Removed audio objectId={} from phase id={}", audioObjectId, phaseId);
        return breathingMapper.toDto(loadOrThrow(phase.getBreathing().getId()));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BreathingEntity loadOrThrow(Long id) {
        return breathingRepository.findById(id)
                .orElseThrow(() -> new BreathingNotFoundException(id));
    }

    private BreathingPhaseEntity findPhaseOrThrow(Long phaseId) {
        return breathingPhaseRepository.findById(phaseId)
                .orElseThrow(() -> new BreathingNotFoundException("Phase not found with id: " + phaseId));
    }

    private BreathingPhaseEntity buildPhase(BreathingPhaseCreateRequest req, int index, BreathingEntity parent) {
        BreathingPhaseEntity phase = new BreathingPhaseEntity();
        phase.setBreathing(parent);
        phase.setName(req.name());
        phase.setLabel(req.label());
        phase.setDurationSeconds(req.durationSeconds());
        phase.setColor(req.color());
        phase.setDisplayOrder(req.displayOrder() != null ? req.displayOrder() : index);
        phase.setAudioFiles(new ArrayList<>());
        return phase;
    }

    /** Uploads and links new audio files for a phase using the indices specified in the request. */
    private void attachAudioFiles(BreathingPhaseEntity phase, BreathingPhaseCreateRequest req,
                                   List<MultipartFile> audioFiles) {
        if (req.newAudioFileIndices() == null || audioFiles == null || audioFiles.isEmpty()) return;
        for (int idx : req.newAudioFileIndices()) {
            if (idx < 0 || idx >= audioFiles.size()) continue;
            MultipartFile file = audioFiles.get(idx);
            if (file == null || file.isEmpty()) continue;
            S3ObjectEntity audioObject = breathingStorageService.uploadAudio(file);
            BreathingPhaseAudioEntity entry = new BreathingPhaseAudioEntity();
            entry.setPhase(phase);
            entry.setAudioObject(audioObject);
            breathingPhaseAudioRepository.save(entry);
        }
    }
}
