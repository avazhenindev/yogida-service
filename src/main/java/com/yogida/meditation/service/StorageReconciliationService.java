package com.yogida.meditation.service;

import com.yogida.meditation.config.r2.R2Properties;
import com.yogida.meditation.constants.BucketNames;
import com.yogida.meditation.dto.StorageReconciliationReport;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.repository.S3ObjectRepository;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares the {@code s3_object} table against the buckets, and reports the differences.
 *
 * <p><b>Report only.</b> It deletes nothing, in either direction. Automatically deleting storage
 * on the strength of a query is how a bug becomes data loss, and the two orphan directions differ
 * in cost anyway: an orphaned row blocks a key from ever being reused, while an orphaned object
 * merely occupies space.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageReconciliationService {

    private final S3ObjectRepository s3ObjectRepository;
    private final R2StorageApi r2StorageApi;
    private final R2Properties r2Properties;

    @Transactional(readOnly = true)
    public StorageReconciliationReport reconcile() {
        List<S3ObjectEntity> allObjects = s3ObjectRepository.findAll();

        List<String> orphanedRows = s3ObjectRepository.findOrphans().stream()
                .map(o -> "s3_object#" + o.getId() + " " + o.getBucketName() + "/" + o.getObjectUri())
                .toList();

        Map<String, Set<String>> knownKeysByBucket = allObjects.stream().collect(Collectors.groupingBy(
                S3ObjectEntity::getBucketName,
                LinkedHashMap::new,
                Collectors.mapping(S3ObjectEntity::getObjectUri, Collectors.toCollection(LinkedHashSet::new))));

        Map<String, List<String>> orphanedObjects = new LinkedHashMap<>();
        Map<String, List<String>> missingObjects = new LinkedHashMap<>();
        List<String> inspected = new ArrayList<>();
        Map<String, String> failed = new LinkedHashMap<>();

        for (String bucket : bucketsToInspect(knownKeysByBucket.keySet())) {
            Set<String> known = knownKeysByBucket.getOrDefault(bucket, Set.of());
            try {
                List<String> actualKeys = r2StorageApi.listObjectKeys(bucket);
                inspected.add(bucket);

                List<String> unreferenced = actualKeys.stream().filter(k -> !known.contains(k)).toList();
                if (!unreferenced.isEmpty()) {
                    orphanedObjects.put(bucket, unreferenced);
                }
                Set<String> actual = new LinkedHashSet<>(actualKeys);
                List<String> absent = known.stream().filter(k -> !actual.contains(k)).toList();
                if (!absent.isEmpty()) {
                    missingObjects.put(bucket, absent);
                }
            } catch (RuntimeException e) {
                // A bucket that cannot be listed must be reported as such, never silently treated
                // as empty — that would report every one of its rows as a missing object.
                log.warn("StorageReconciliationService > Could not list bucket {}: {}", bucket, e.getMessage());
                failed.put(bucket, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }

        log.info("StorageReconciliationService > {} orphaned row(s); inspected {} bucket(s), {} failed",
                orphanedRows.size(), inspected.size(), failed.size());
        return new StorageReconciliationReport(
                orphanedRows, orphanedObjects, missingObjects, inspected, failed);
    }

    /**
     * The buckets this system actually stores objects in.
     *
     * <p>Deliberately not {@code AdminStorageApi.listBuckets()}, which filters out
     * {@code BucketNames.PICTURES} — and pictures are not a bucket at all, they are a key prefix
     * inside the public one. Driving the report off that list would have inspected the wrong set
     * and reported real objects as orphans.
     *
     * <p>Derived from the rows themselves plus the two configured buckets, so a bucket that
     * currently holds nothing is still checked for objects with no row.
     */
    private Set<String> bucketsToInspect(Set<String> bucketsFromRows) {
        Set<String> buckets = new LinkedHashSet<>(bucketsFromRows);
        buckets.add(BucketNames.PUBLIC);
        if (r2Properties.bucket() != null && !r2Properties.bucket().isBlank()) {
            buckets.add(r2Properties.bucket());
        }
        buckets.removeIf(b -> b == null || b.isBlank());
        return buckets;
    }
}
