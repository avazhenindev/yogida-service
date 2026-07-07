package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.S3ObjectDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Media", description = "Public endpoints for browsing and streaming meditation media")
@RequestMapping("/media")
public interface MediaControllerApi {

    @Operation(summary = "List all active media", description = "Returns all media catalog entries with ACTIVE status.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Media list retrieved"))
    @GetMapping
    ResponseEntity<List<MediaDto>> getAll();

    @Operation(summary = "Get media by ID", description = "Returns a single media item by its database ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Media found"),
        @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<MediaDto> getById(
        @Parameter(description = "Media ID", required = true) @PathVariable Long id);

    @Operation(summary = "Get presigned streaming URL", description = "Returns a time-limited presigned URL for streaming the media object.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Presigned URL generated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(type = "object", example = """
                    {"url": "https://r2.example.com/bucket/media.mp3?X-Amz-Signature=..."}"""))),
        @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @PostMapping("/stream")
    ResponseEntity<Map<String, String>> getStreamUrl(@RequestBody S3ObjectDto s3ObjectDto);
}

