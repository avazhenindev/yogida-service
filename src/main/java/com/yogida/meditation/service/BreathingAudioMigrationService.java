package com.yogida.meditation.service;

import com.yogida.meditation.constants.BucketNames;
import com.yogida.meditation.dto.BreathingAudioMigrationResult;
import com.yogida.meditation.entity.BreathingEntity;
import com.yogida.meditation.entity.BreathingPhaseAudioEntity;
import com.yogida.meditation.entity.BreathingPhaseEntity;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.service.storage.StorageConfigs;
import com.yogida.meditation.repository.BreathingRepository;
import com.yogida.meditation.repository.S3ObjectRepository;
import com.yogida.meditation.service.api.AdminStorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One-off migration moving breathing phase audio out of the public R2 bucket into the private one.
 *
 * <p>Why this cannot be a Liquibase changeset: the fix is a byte-level move in object storage, and
 * repointing {@code s3_object.bucket_name} without moving the object leaves a row that presigns to
 * a 404. Both halves have to happen together, so it lives in application code.
 *
 * <p><b>Until this has run, existing premium breathing audio is still publicly reachable.</b>
 * Moving new uploads to the private bucket closes the hole for new content only; every object
 * uploaded before that change is still sitting behind an unsigned public URL. That is the whole
 * purpose of this migration and why it is not optional.
 *
 * <p>Idempotent and re-runnable: rows already pointing at the private bucket are skipped, and a
 * copy that has already happened is detected before the delete. Supports a dry run, which reports
 * exactly what would move and changes nothing — worth using first, because the delete step is not
 * reversible.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BreathingAudioMigrationService {

    private final BreathingRepository breathingRepository;
    private final S3ObjectRepository s3ObjectRepository;
    private final AdminStorageApi adminStorageApi;

    @Value("${cloudflare.r2.bucket:}")
    private String privateBucket;

    /**
     * Moves every breathing phase audio object still held in the public bucket.
     *
     * @param dryRun when true, reports what would move and performs no copy, delete or DB write
     */
    @Transactional
    public BreathingAudioMigrationResult migrateToPrivateBucket(boolean dryRun) {
        String target = StorageConfigs.requireBucketName(privateBucket, "cloudflare.r2.bucket", "private audio bucket");

        List<String> moved = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        // One S3 object can be attached to several phases, so iterating phase-audio rows visits
        // the same object more than once. Deduplicating keeps the report honest and avoids
        // issuing a second copy-and-delete for an object already handled in this run.
        Set<Long> visited = new HashSet<>();

        for (BreathingEntity exercise : breathingRepository.findAll()) {
            for (BreathingPhaseEntity phase : exercise.getPhases()) {
                if (phase.getAudioFiles() == null) {
                    continue;
                }
                for (BreathingPhaseAudioEntity audio : phase.getAudioFiles()) {
                    S3ObjectEntity object = audio.getAudioObject();
                    if (object == null || object.getObjectUri() == null) {
                        continue;
                    }
                    if (!visited.add(object.getId())) {
                        continue;
                    }
                    if (!BucketNames.PUBLIC.equals(object.getBucketName())) {
                        skipped.add(describe(object) + " (already private)");
                        continue;
                    }
                    migrateOne(object, target, dryRun, moved, failed);
                }
            }
        }

        BreathingAudioMigrationResult result =
                new BreathingAudioMigrationResult(dryRun, target, moved, skipped, failed);
        log.info("BreathingAudioMigrationService > {} complete: {} moved, {} skipped, {} failed",
                dryRun ? "Dry run" : "Migration", moved.size(), skipped.size(), failed.size());
        return result;
    }

    private void migrateOne(S3ObjectEntity object, String target, boolean dryRun,
                            List<String> moved, List<String> failed) {
        String key = object.getObjectUri();

        if (dryRun) {
            moved.add(describe(object) + " -> " + target + "/" + key);
            return;
        }

        try {
            // Copy first, and treat an already-present target as success — a previous run may
            // have copied and then failed before the delete.
            boolean copied = adminStorageApi.objectExists(target, key)
                    || adminStorageApi.copyObject(BucketNames.PUBLIC, key, target, key);
            if (!copied) {
                failed.add(describe(object) + " (source object missing in public bucket)");
                return;
            }

            // Repoint the row before deleting the public copy. If the delete then fails, the
            // object is merely still readable publicly — the state it is in today. Deleting
            // first would strand a row pointing at nothing.
            object.setBucketName(target);
            object.setBaseUrl("");
            s3ObjectRepository.save(object);

            adminStorageApi.deleteObject(BucketNames.PUBLIC, key);
            moved.add(describe(object) + " -> " + target + "/" + key);
        } catch (RuntimeException e) {
            log.warn("BreathingAudioMigrationService > Failed to migrate {}: {}",
                    describe(object), e.getMessage());
            failed.add(describe(object) + " (" + e.getMessage() + ")");
        }
    }

    private String describe(S3ObjectEntity object) {
        return "s3_object#" + object.getId() + " " + object.getBucketName() + "/" + object.getObjectUri();
    }

}
