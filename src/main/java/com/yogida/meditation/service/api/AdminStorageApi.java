package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminStorageApi {

    List<BucketDto> listBuckets();

    void createBucket(String bucketName);

    void deleteBucket(String bucketName);


    ObjectMetadataDto uploadObject(String bucketName, String objectKey, MultipartFile file);

    void deleteObject(String bucketName, String objectKey);
}

