package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin — Media", description = "Admin CRUD for media catalog entries with integrated S3 object lifecycle")
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

    @Operation(summary = "Create media record and upload S3 object",
               description = "Uploads the file to S3 and creates the media catalog entry in one step.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Media created"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<MediaDto> create(@Valid @ModelAttribute MediaCreateRequest request);

    @Operation(summary = "Update media record",
               description = "Updates metadata. If a new file is provided and the object key differs, uploads it to S3 and removes the old object.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media updated"),
            @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<MediaDto> update(
            @Parameter(description = "Media ID", required = true) @PathVariable Long id,
            @Valid @ModelAttribute MediaFileUpdateRequest request);

    @Operation(summary = "Delete media record and its S3 object",
               description = "Removes the DB record and deletes the associated S3 object. S3 deletion failure is logged but does not affect the response.")
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
