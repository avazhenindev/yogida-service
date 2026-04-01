package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.MediaSubscriptionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Media Subscriptions", description = "CRUD endpoints for media-subscription mappings")
@RequestMapping("/media-subscriptions")
public interface MediaSubscriptionControllerApi {

    @Operation(summary = "Get all media-subscription mappings", description = "Returns all links between media and subscriptions.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Mappings retrieved successfully"))
    @GetMapping
    ResponseEntity<List<MediaSubscriptionDto>> getAll();

    @Operation(summary = "Get mapping by ID", description = "Returns a single media-subscription mapping by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mapping found"),
            @ApiResponse(responseCode = "404", description = "Mapping not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<MediaSubscriptionDto> getById(
            @Parameter(description = "Media-subscription ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create a new mapping", description = "Links a media item to a subscription.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Mapping created successfully"))
    @PostMapping
    ResponseEntity<MediaSubscriptionDto> create(@RequestBody MediaSubscriptionDto dto);

    @Operation(summary = "Update an existing mapping", description = "Updates a media-subscription mapping by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mapping updated successfully"),
            @ApiResponse(responseCode = "404", description = "Mapping not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<MediaSubscriptionDto> update(
            @Parameter(description = "Media-subscription ID", required = true) @PathVariable Long id,
            @RequestBody MediaSubscriptionDto dto);

    @Operation(summary = "Delete a mapping", description = "Removes a media-subscription mapping by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Mapping deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Mapping not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Media-subscription ID", required = true) @PathVariable Long id);
}

