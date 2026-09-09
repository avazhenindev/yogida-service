package com.yogida.meditation.service;

import com.yogida.meditation.constants.BucketNames;
import com.yogida.meditation.dto.*;
import com.yogida.meditation.service.api.AdminStorageApi;
import com.yogida.meditation.service.storage.S3Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminStorageService implements AdminStorageApi {

    private final S3Client s3Client;

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



    /**
     * Uploads a multipart file, streaming it rather than buffering it.
     *
     * <p>Previously {@code RequestBody.fromBytes(file.getBytes())} pulled the entire object into
     * heap — up to the configured 100 MB multipart cap, per concurrent upload. Spring spools
     * large parts to disk precisely so they need not be materialised, and {@code getBytes()}
     * undid that.
     *
     * <p>A {@code ContentStreamProvider} rather than a plain {@code InputStream} because
     * {@code R2ClientConfig} disables chunked encoding, so the SDK signs the full payload: it
     * reads the body once to hash it and again to transmit, and re-reads it on every retry. A
     * provider can reopen the part each time; a single stream cannot rewind.
     */
    @Override
    public void uploadObject(String bucketName, String objectKey, MultipartFile file) {
        // ContentStreamProvider.newStream() declares no checked exception, so getInputStream's
        // IOException has to be wrapped here rather than passed as a method reference.
        ContentStreamProvider provider = () -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read upload stream for key: " + objectKey, e);
            }
        };
        // RequestBody rejects a null content type outright, and a multipart part is not
        // required to declare one.
        String contentType = file.getContentType() != null
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        put(bucketName, objectKey,
                RequestBody.fromContentProvider(provider, file.getSize(), contentType));
    }

    private void put(String bucketName, String objectKey, RequestBody body) {
        log.debug("Uploading object to bucket [{}] with key [{}]", bucketName, objectKey);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(body.contentType())
                        .contentLength(body.optionalContentLength().orElse(null))
                        .build(),
                body);
    }

    @Override
    public void deleteObject(String bucketName, String objectKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
    }

    /**
     * Server-side copy between buckets, used by the one-off migration that moves premium
     * breathing audio out of the public bucket.
     */
    @Override
    public boolean copyObject(String sourceBucket, String sourceKey,
                              String targetBucket, String targetKey) {
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(sourceBucket)
                    .sourceKey(sourceKey)
                    .destinationBucket(targetBucket)
                    .destinationKey(targetKey)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            log.warn("AdminStorageService > Copy source missing: {}/{}", sourceBucket, sourceKey);
            return false;
        }
    }

    @Override
    public boolean objectExists(String bucketName, String objectKey) {
        return S3Objects.exists(s3Client, bucketName, objectKey);
    }
}
