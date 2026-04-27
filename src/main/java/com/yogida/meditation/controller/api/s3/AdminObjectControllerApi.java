package com.yogida.meditation.controller.api.s3;

import com.yogida.meditation.dto.BulkDeleteRequest;
import com.yogida.meditation.dto.ObjectListResponse;
import com.yogida.meditation.dto.ObjectMetadataDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "Admin — S3 Objects", description = "Admin endpoints for managing S3/R2 objects within a bucket")
@RequestMapping("/admin/s3/buckets/{bucketName}/objects")
public interface AdminObjectControllerApi {

    @Operation(summary = "List objects in a bucket", description = "Paginated object listing. Pass continuationToken from previous response to get next page.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Objects retrieved"))
    @GetMapping
    ResponseEntity<ObjectListResponse> listObjects(
            @Parameter(description = "Bucket name", required = true) @PathVariable String bucketName,
            @Parameter(description = "Pagination token from previous response") @RequestParam(required = false) String continuationToken,
            @Parameter(description = "Max number of objects to return (default 100)") @RequestParam(defaultValue = "100") int maxKeys);

    @Operation(summary = "Upload an object", description = "Uploads a file to the specified bucket. Returns object metadata including the S3 URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Object uploaded"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ObjectMetadataDto> uploadObject(
            @Parameter(description = "Bucket name", required = true) @PathVariable String bucketName,
            @Parameter(description = "Object key (file path within the bucket)", required = true) @RequestParam String objectKey,
            @Parameter(description = "File to upload", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart MultipartFile file);

    @Operation(summary = "Delete a single object")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Object deleted"),
            @ApiResponse(responseCode = "404", description = "Object not found")
    })
    @DeleteMapping
    ResponseEntity<Void> deleteObject(
            @Parameter(description = "Bucket name", required = true) @PathVariable String bucketName,
            @Parameter(description = "Object key", required = true) @RequestParam String objectKey);

    @Operation(summary = "Bulk delete objects", description = "Deletes multiple objects in one request.")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Objects deleted"))
    @DeleteMapping("/bulk")
    ResponseEntity<Void> bulkDeleteObjects(
            @Parameter(description = "Bucket name", required = true) @PathVariable String bucketName,
            @Valid @RequestBody BulkDeleteRequest request);

    @Operation(summary = "Generate presigned URL", description = "Returns a time-limited presigned download URL for the specified object.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Presigned URL generated",
            content = @Content(schema = @Schema(type = "object", example = """
                    {"url": "https://r2.example.com/bucket/file.mp3?X-Amz-Signature=..."}"""))))
    @GetMapping("/presign")
    ResponseEntity<Map<String, String>> getPresignedUrl(
            @Parameter(description = "Bucket name", required = true) @PathVariable String bucketName,
            @Parameter(description = "Object key", required = true) @RequestParam String objectKey);
}

