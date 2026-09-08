package com.yogida.meditation.service;

import com.yogida.meditation.config.r2.R2Properties;
import com.yogida.meditation.service.api.R2StorageApi;
import com.yogida.meditation.service.storage.S3Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class R2StorageService implements R2StorageApi {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final R2Properties r2Properties;

    @Value("${app.audio.presigned-url-duration-minutes:60}")
    private long presignedUrlDurationMinutes;

    @Override
    public String generateStreamingUrl(String bucketName, String mediaName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(mediaName)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlDurationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    @Override
    public boolean objectExists(String bucketName, String objectKey) {
        return S3Objects.existsAllowingMissingBucket(s3Client, bucketName, objectKey);
    }



    /**
     * Already existed as a private helper with correct pagination; promoted to the interface for
     * the reconciliation report rather than writing a second implementation of the same loop.
     */
    @Override
    public List<String> listObjectKeys(String bucketName) {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;
        do {
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(1000);
            if (continuationToken != null) {
                requestBuilder.continuationToken(continuationToken);
            }
            ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
            response.contents().forEach(obj -> keys.add(obj.key()));
            continuationToken = response.isTruncated() ? response.nextContinuationToken() : null;
        } while (continuationToken != null);
        return keys;
    }

}
