package com.yogida.meditation.service;

import com.yogida.meditation.config.r2.R2Properties;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class R2StorageService implements R2StorageApi {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final R2Properties r2Properties;

    @Value("${app.audio.presigned-url-duration-minutes:15}")
    private long presignedUrlDurationMinutes;

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
    public Map<String, List<String>> getAllBucketsWithObjects() {
        List<Bucket> buckets = s3Client.listBuckets().buckets();
        Map<String, List<String>> result = new LinkedHashMap<>();

        for (Bucket bucket : buckets) {
            List<String> allKeys = listAllObjectKeys(bucket.name());
            result.put(bucket.name(), allKeys);
        }

        return result;
    }

    private List<String> listAllObjectKeys(String bucketName) {
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
