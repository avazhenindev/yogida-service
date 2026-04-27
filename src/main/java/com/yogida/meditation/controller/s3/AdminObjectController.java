package com.yogida.meditation.controller.s3;

import com.yogida.meditation.controller.api.s3.AdminObjectControllerApi;
import com.yogida.meditation.dto.BulkDeleteRequest;
import com.yogida.meditation.dto.ObjectListResponse;
import com.yogida.meditation.dto.ObjectMetadataDto;
import com.yogida.meditation.service.api.AdminStorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AdminObjectController implements AdminObjectControllerApi {

    private final AdminStorageApi adminStorageApi;

    @Override
    public ResponseEntity<ObjectListResponse> listObjects(String bucketName, String continuationToken, int maxKeys) {
        return ResponseEntity.ok(adminStorageApi.listObjects(bucketName, continuationToken, maxKeys));
    }

    @Override
    public ResponseEntity<ObjectMetadataDto> uploadObject(String bucketName, String objectKey, MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminStorageApi.uploadObject(bucketName, objectKey, file));
    }

    @Override
    public ResponseEntity<Void> deleteObject(String bucketName, String objectKey) {
        adminStorageApi.deleteObject(bucketName, objectKey);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> bulkDeleteObjects(String bucketName, BulkDeleteRequest request) {
        adminStorageApi.bulkDeleteObjects(bucketName, request.keys());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Map<String, String>> getPresignedUrl(String bucketName, String objectKey) {
        return ResponseEntity.ok(Map.of("url", adminStorageApi.generatePresignedUrl(bucketName, objectKey)));
    }
}

