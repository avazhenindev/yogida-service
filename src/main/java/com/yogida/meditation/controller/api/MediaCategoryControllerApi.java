package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.MediaCategoryCreateRequest;
import com.yogida.meditation.dto.MediaCategoryDto;
import com.yogida.meditation.dto.MediaCategoryUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Media Categories", description = "CRUD endpoints for the media category dictionary")
@RequestMapping("/media-categories")
public interface MediaCategoryControllerApi {

    @Operation(summary = "Get all media categories")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Media categories retrieved successfully"))
    @GetMapping
    ResponseEntity<List<MediaCategoryDto>> getAll();

    @Operation(summary = "Get media category by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media category found"),
            @ApiResponse(responseCode = "404", description = "Media category not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<MediaCategoryDto> getById(
            @Parameter(description = "Media category ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create a new media category")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Media category created successfully"))
    @PostMapping
    ResponseEntity<MediaCategoryDto> create(@Valid @RequestBody MediaCategoryCreateRequest request);

    @Operation(summary = "Update an existing media category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Media category updated successfully"),
            @ApiResponse(responseCode = "404", description = "Media category not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<MediaCategoryDto> update(
            @Parameter(description = "Media category ID", required = true) @PathVariable Long id,
            @Valid @RequestBody MediaCategoryUpdateRequest request);

    @Operation(summary = "Delete a media category")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Media category deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Media category not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Media category ID", required = true) @PathVariable Long id);
}

