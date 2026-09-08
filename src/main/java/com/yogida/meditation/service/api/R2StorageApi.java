package com.yogida.meditation.service.api;

import java.util.List;

public interface R2StorageApi {

    /**
     * Generates a time-limited presigned URL for the given bucket and object key.
     */
    String generateStreamingUrl(String bucketName, String mediaName);

    /**
     * Returns true if the object exists in S3; false if not found.
     * Throws S3Exception on connectivity or permission errors.
     */
    boolean objectExists(String bucketName, String objectKey);
    /**
     * Every object key in a bucket, following pagination to the end.
     *
     * <p>Used only by the reconciliation report, which compares key sets against
     * {@code s3_object.object_uri}.
     */
    List<String> listObjectKeys(String bucketName);
}
