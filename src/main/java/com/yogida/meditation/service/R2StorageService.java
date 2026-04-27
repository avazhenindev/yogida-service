package com.yogida.meditation.service;

import com.yogida.meditation.config.r2.R2Properties;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URI;
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
    public String generateStreamingUrlFromS3Url(String s3Url) {
        String[] parts = parseS3Url(s3Url);
        return generateStreamingUrl(parts[0], parts[1]);
    }

    @Override
    public boolean objectExists(String bucketName, String objectKey) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
            return true;
        } catch (NoSuchKeyException | NoSuchBucketException e) {
            return false;
        }
    }

    @Override
    public Map<String, List<String>> getAllBucketsWithObjects() {
        List<Bucket> buckets = s3Client.listBuckets().buckets();
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Bucket bucket : buckets) {
            result.put(bucket.name(), listAllObjectKeys(bucket.name()));
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

    @Override
    public String[] parseS3Url(String s3Url) {
        // URI.create rejects unencoded spaces/apostrophes — strip scheme+host manually
        String path;
        try {
            // Try standard parse first (works for already-encoded URLs)
            path = URI.create(s3Url.replace(" ", "%20")).getPath();
            // Decode percent-encoding so the raw key is returned to callers
            path = java.net.URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // Fallback: strip "https://host" prefix by finding third slash
            int schemeEnd = s3Url.indexOf("://");
            int pathStart = schemeEnd < 0 ? 0 : s3Url.indexOf('/', schemeEnd + 3);
            path = pathStart < 0 ? s3Url : s3Url.substring(pathStart);
        }
        String stripped = path.startsWith("/") ? path.substring(1) : path;
        int slash = stripped.indexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("Cannot parse bucket/key from S3 URL: " + s3Url);
        }
        return new String[]{stripped.substring(0, slash), stripped.substring(slash + 1)};
    }
}
