package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaLogDto;
import com.yogida.meditation.dto.MediaUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin — Media", description = "Admin CRUD for media catalog entries")
@RequestMapping("/admin/media")
public interface AdminMediaControllerApi {

    @Operation(summary = "List all media", description = "Returns all media records regardless of status.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Media list retrieved"))
    @GetMapping
    ResponseEntity<List<MediaDto>> getAll();

    @Operation(summary = "Get media by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media found"),
            @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<MediaDto> getById(
            @Parameter(description = "Media ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create media record", description = "Creates a new media catalog entry pointing to an existing S3 object.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Media created"))
    @PostMapping
    ResponseEntity<MediaDto> create(@Valid @RequestBody MediaUpdateRequest request);

    @Operation(summary = "Update media record", description = "Updates name, s3Url, description, or category.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media updated"),
            @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<MediaDto> update(
            @Parameter(description = "Media ID", required = true) @PathVariable Long id,
            @Valid @RequestBody MediaUpdateRequest request);

    @Operation(summary = "Delete media record", description = "Removes the DB record. Does not delete the S3 object.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Media deleted"),
            @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Media ID", required = true) @PathVariable Long id);

    @Operation(summary = "Get audit log for a media item")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Log entries retrieved"))
    @GetMapping("/{id}/logs")
    ResponseEntity<List<MediaLogDto>> getLogs(
            @Parameter(description = "Media ID", required = true) @PathVariable Long id);
}

