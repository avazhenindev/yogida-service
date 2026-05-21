package com.yogida.meditation.controller.s3;

import com.yogida.meditation.controller.api.s3.AdminBucketControllerApi;
import com.yogida.meditation.dto.BucketDto;
import com.yogida.meditation.service.api.AdminStorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminBucketController implements AdminBucketControllerApi {

    private final AdminStorageApi adminStorageApi;

    @Override
    public ResponseEntity<List<BucketDto>> listBuckets() {
        return ResponseEntity.ok(adminStorageApi.listBuckets());
    }

    @Override
    public ResponseEntity<Void> createBucket(String bucketName) {
        adminStorageApi.createBucket(bucketName);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<Void> deleteBucket(String bucketName) {
        adminStorageApi.deleteBucket(bucketName);
        return ResponseEntity.noContent().build();
    }
}

