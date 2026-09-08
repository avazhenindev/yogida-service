package com.yogida.meditation.service.storage;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Existence probes against S3, shared by the two storage services that each had their own copy.
 *
 * <p>The copies were identical apart from their catch clause, which is exactly the kind of
 * difference that reads as an oversight and is easy to "fix" in the wrong direction. It is kept
 * here as two named methods so the choice is deliberate at every call site: whether a missing
 * bucket means "no such object" or a misconfiguration worth surfacing depends on the caller, and
 * both answers are in use.
 */
public final class S3Objects {

    private S3Objects() {
    }

    /**
     * True if the object is present. A missing <em>bucket</em> propagates, because a caller that
     * is about to write into that bucket wants to hear about it rather than be told "absent".
     */
    public static boolean exists(S3Client s3Client, String bucketName, String objectKey) {
        try {
            head(s3Client, bucketName, objectKey);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * True if the object is present, treating a missing bucket as "absent" too. For read-side
     * probes such as the media health check, where an unreachable bucket is one more unavailable
     * object rather than an error to raise per row.
     */
    public static boolean existsAllowingMissingBucket(S3Client s3Client, String bucketName, String objectKey) {
        try {
            head(s3Client, bucketName, objectKey);
            return true;
        } catch (NoSuchKeyException | NoSuchBucketException e) {
            return false;
        }
    }

    private static void head(S3Client s3Client, String bucketName, String objectKey) {
        s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
    }
}
