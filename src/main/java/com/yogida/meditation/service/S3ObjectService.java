package com.yogida.meditation.service;

import com.yogida.meditation.config.r2.R2Properties;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.repository.S3ObjectRepository;
import com.yogida.meditation.service.api.AdminStorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

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

        try {
            adminStorageApi.deleteObject(object.getBucketName(), object.getObjectUri());
        } catch (Exception e) {
            log.warn("Failed to delete S3 object [bucket={}, objectUri={}]: {}",
                    object.getBucketName(), object.getObjectUri(), e.getMessage());
        }
    }

    public void deleteObjectAfterCommit(S3ObjectEntity object) {
        if (object == null) {
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

