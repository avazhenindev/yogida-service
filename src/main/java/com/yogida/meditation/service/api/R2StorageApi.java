package com.yogida.meditation.service.api;

import java.util.List;
import java.util.Map;

public interface R2StorageApi {

    /**
     * Generates a time-limited presigned URL for the given bucket and object key.
     */
    String generateStreamingUrl(String bucketName, String mediaName);

    /**
     * Generates a presigned URL by parsing bucket and key from a full S3 URL.
     */
    String generateStreamingUrlFromS3Url(String s3Url);

    /**
     * Returns true if the object exists in S3; false if not found.
     * Throws S3Exception on connectivity or permission errors.
     */
    boolean objectExists(String bucketName, String objectKey);


    String[] parseS3Url(String s3Url);
}
