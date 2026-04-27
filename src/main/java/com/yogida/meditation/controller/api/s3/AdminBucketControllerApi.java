package com.yogida.meditation.controller.api.s3;

import com.yogida.meditation.dto.BucketDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin — S3 Buckets", description = "Admin endpoints for managing S3/R2 buckets")
@RequestMapping("/admin/s3/buckets")
public interface AdminBucketControllerApi {

    @Operation(summary = "List all buckets")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Buckets retrieved"))
    @GetMapping
    ResponseEntity<List<BucketDto>> listBuckets();

    @Operation(summary = "Create a bucket")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bucket created"),
            @ApiResponse(responseCode = "400", description = "Invalid bucket name or bucket already exists")
    })
    @PostMapping("/{bucketName}")
    ResponseEntity<Void> createBucket(
            @Parameter(description = "Bucket name", required = true) @PathVariable String bucketName);

    @Operation(summary = "Delete a bucket", description = "Bucket must be empty before deletion.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Bucket deleted"),
            @ApiResponse(responseCode = "404", description = "Bucket not found"),
            @ApiResponse(responseCode = "409", description = "Bucket is not empty")
    })
    @DeleteMapping("/{bucketName}")
    ResponseEntity<Void> deleteBucket(
            @Parameter(description = "Bucket name", required = true) @PathVariable String bucketName);
}

