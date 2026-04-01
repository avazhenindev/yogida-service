package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.FavouriteDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favourites", description = "CRUD endpoints for user favourites")
@RequestMapping("/favourites")
public interface FavouriteControllerApi {

    @Operation(summary = "Get all favourites", description = "Returns a list of all favourites.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Favourites retrieved successfully"))
    @GetMapping
    ResponseEntity<List<FavouriteDto>> getAll();

    @Operation(summary = "Get favourite by ID", description = "Returns a single favourite by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favourite found"),
            @ApiResponse(responseCode = "404", description = "Favourite not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<FavouriteDto> getById(
            @Parameter(description = "Favourite ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create a new favourite", description = "Creates a new user favourite.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Favourite created successfully"))
    @PostMapping
    ResponseEntity<FavouriteDto> create(@RequestBody FavouriteDto dto);

    @Operation(summary = "Update an existing favourite", description = "Updates favourite details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favourite updated successfully"),
            @ApiResponse(responseCode = "404", description = "Favourite not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<FavouriteDto> update(
            @Parameter(description = "Favourite ID", required = true) @PathVariable Long id,
            @RequestBody FavouriteDto dto);

    @Operation(summary = "Delete a favourite", description = "Removes a favourite by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Favourite deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Favourite not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Favourite ID", required = true) @PathVariable Long id);
}

