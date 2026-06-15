package com.yogida.meditation.service;

import com.yogida.meditation.config.r2.R2Properties;
import com.yogida.meditation.constants.BucketNames;
import com.yogida.meditation.dto.*;
import com.yogida.meditation.service.api.AdminStorageApi;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStorageService implements AdminStorageApi {

    private final S3Client s3Client;
    private final R2StorageApi r2StorageApi;
    private final R2Properties r2Properties;

    @Override
    public List<BucketDto> listBuckets() {
        return s3Client.listBuckets().buckets().stream()
                .filter(b -> !b.name().equals(BucketNames.PICTURES))
                .map(b -> new BucketDto(b.name(), b.creationDate()))
                .toList();
    }

    @Override
    public void createBucket(String bucketName) {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
    }

    @Override
    public void deleteBucket(String bucketName) {
        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }



    @Override
    public ObjectMetadataDto uploadObject(String bucketName, String objectKey, MultipartFile file) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(objectKey)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));

            HeadObjectResponse head = s3Client.headObject(
                    HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build());

            return new ObjectMetadataDto(
                    objectKey,
                    head.contentLength(),
                    head.lastModified(),
                    head.eTag(),
                    buildS3Url(bucketName, objectKey));

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read upload stream for key: " + objectKey, e);
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
    }




    private String buildS3Url(String bucketName, String objectKey) {
        return "https://" + r2Properties.accountId() + ".r2.cloudflarestorage.com/"
                + bucketName + "/" + objectKey;
    }
}

