package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

public interface AdminStorageApi {

    List<BucketDto> listBuckets();

    void createBucket(String bucketName);

    void deleteBucket(String bucketName);


    ObjectMetadataDto uploadObject(String bucketName, String objectKey, MultipartFile file);

    /**
     * Uploads a file already materialised on disk. The bytes stream from the file, so object
     * size does not affect heap and the SDK can re-read them for payload signing and retries.
     */
    ObjectMetadataDto uploadObject(String bucketName, String objectKey, Path file, String contentType);

    void deleteObject(String bucketName, String objectKey);

    /**
     * Server-side copy of an object between buckets. The bytes never travel through this
     * service — R2 performs the copy itself, so object size does not affect heap or latency.
     *
     * @return true when the copy succeeded, false when the source object does not exist
     */
    boolean copyObject(String sourceBucket, String sourceKey, String targetBucket, String targetKey);

    /** Whether an object exists, without transferring it. */
    boolean objectExists(String bucketName, String objectKey);
}

