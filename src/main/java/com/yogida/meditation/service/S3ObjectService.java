package com.yogida.meditation.service;

import com.yogida.meditation.config.r2.R2Properties;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.S3ObjectRepository;
import com.yogida.meditation.service.api.AdminStorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Service
@RequiredArgsConstructor
public class S3ObjectService {

    private final S3ObjectRepository s3ObjectRepository;
    private final AdminStorageApi adminStorageApi;
    private final R2Properties r2Properties;

    public S3ObjectEntity createMediaObject(String bucketName, String objectUri) {
        String baseUrl = "https://" + r2Properties.accountId() + ".r2.cloudflarestorage.com/" + bucketName;
        return createObject(bucketName, baseUrl, objectUri);
    }

    public S3ObjectEntity createObject(String bucketName, String baseUrl, String objectUri) {
        S3ObjectEntity object = new S3ObjectEntity();
        object.setBucketName(bucketName);
        object.setBaseUrl(baseUrl);
        object.setObjectUri(objectUri);
        object.setCreatedAt(LocalDateTime.now());
        return s3ObjectRepository.save(object);
    }

    public S3ObjectEntity findById(Long id) {
        return s3ObjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("S3Object", id));
    }

    public void deleteObject(S3ObjectEntity object) {
        if (object == null) {
            return;
        }
        deleteObject(object.getBucketName(), object.getObjectUri());
    }

    /**
     * Deletes an object by coordinates rather than by row, for objects that have no row yet.
     * Never throws: cleanup failing must not mask the error that triggered it.
     */
    public void deleteObject(String bucketName, String objectKey) {
        try {
            adminStorageApi.deleteObject(bucketName, objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object [bucket={}, objectUri={}]: {}",
                    bucketName, objectKey, e.getMessage());
        }
    }

    /**
     * Arranges for a just-uploaded object to be removed if the surrounding transaction rolls back.
     *
     * <p>Uploads happen before the row that references them is committed, so a later failure —
     * a validation error, a constraint violation, an ffprobe rejection — used to leave the object
     * in R2 with nothing pointing at it. Those orphans accumulate, and because
     * {@code unique(bucket, base_url, uri)} keys on the object URI, one of them permanently
     * blocks its own key from being reused.
     *
     * <p>Two details matter and both were got wrong in the first design.
     *
     * <p>There is no {@code afterRollback} callback on {@link TransactionSynchronization}; the
     * hook is {@code afterCompletion(int)}. And it must fire ONLY on
     * {@link TransactionSynchronization#STATUS_ROLLED_BACK} — never on {@code STATUS_UNKNOWN},
     * where the commit may in fact have succeeded and deleting would destroy a live object
     * belonging to a committed row.
     */
    /**
     * Uploads an object and stages its removal should the surrounding transaction roll back.
     *
     * <p>The pair is a rule, not a convention: an upload whose rollback is never registered leaves
     * an object in R2 that no row points at, and because {@code unique(bucket, base_url, uri)}
     * keys on the URI, that orphan permanently blocks its own key. Every upload path in the
     * service went through these two calls in sequence and each one could have forgotten the
     * second, so they are one call now.
     *
     * <p>Registering the object rather than the row is deliberate — the caller decides how the
     * object is recorded afterwards, which differs by bucket (a public base URL, an empty one, or
     * the media-specific factory).
     */
    public void uploadStaged(String bucketName, String objectKey, MultipartFile file) {
        adminStorageApi.uploadObject(bucketName, objectKey, file);
        deleteObjectOnRollback(bucketName, objectKey);
    }

    public void deleteObjectOnRollback(String bucketName, String objectKey) {
        if (bucketName == null || objectKey == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Nothing to roll back; the caller is responsible for its own cleanup.
            log.debug("S3ObjectService > No active transaction; skipping rollback registration for {}/{}",
                    bucketName, objectKey);
            return;
        }

        // Guards against a second registration for the same key firing twice.
        AtomicBoolean settled = new AtomicBoolean(false);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_ROLLED_BACK) {
                    return;
                }
                if (!settled.compareAndSet(false, true)) {
                    return;
                }
                log.info("S3ObjectService > Transaction rolled back; removing staged object {}/{}",
                        bucketName, objectKey);
                deleteObject(bucketName, objectKey);
            }
        });
    }

    /**
     * Removes an object from storage once the surrounding transaction commits, and drops its
     * {@code s3_object} row if nothing references it any more.
     *
     * <p>The row deletion happens now, inside the transaction, because after commit there is no
     * transaction left to delete it in. The storage deletion happens after commit, because until
     * then the transaction might still roll back and the object would still be needed.
     *
     * <p><b>Callers must have deleted and flushed the referencing row first.</b> The referent
     * count is what decides whether the row is an orphan, so a caller that registers cleanup
     * before deleting its own reference sees a count of one, concludes the object is still in
     * use, and leaves the row behind for ever. Two call sites in {@code BreathingService} did
     * exactly that.
     *
     * <p>Rows accumulating here is not merely untidy: {@code unique(bucket, base_url, uri)} means
     * an orphan permanently blocks its own key from being written again.
     */
    public void deleteObjectAfterCommit(S3ObjectEntity object) {
        if (object == null) {
            return;
        }

        if (object.getId() != null && s3ObjectRepository.countReferents(object.getId()) == 0) {
            s3ObjectRepository.delete(object);
            log.debug("S3ObjectService > Removed orphaned s3_object row id={}", object.getId());
        } else if (object.getId() != null) {
            // Still referenced elsewhere — leave both the row and the object alone.
            log.debug("S3ObjectService > s3_object row id={} still has referents; keeping the object",
                    object.getId());
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteObject(object);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteObject(object);
            }
        });
    }
}

