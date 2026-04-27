package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminStorageApi {

    List<BucketDto> listBuckets();

    void createBucket(String bucketName);

    void deleteBucket(String bucketName);

    ObjectListResponse listObjects(String bucketName, String continuationToken, int maxKeys);

    /**
     * Uploads an object to S3 and returns its full S3 URL.
     */
    ObjectMetadataDto uploadObject(String bucketName, String objectKey, MultipartFile file);

    void deleteObject(String bucketName, String objectKey);

    void bulkDeleteObjects(String bucketName, List<String> keys);

    String generatePresignedUrl(String bucketName, String objectKey);
}

