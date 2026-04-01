package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.ProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Profiles", description = "CRUD endpoints for user profiles")
@RequestMapping("/profiles")
public interface ProfileControllerApi {

    @Operation(summary = "Get all profiles", description = "Returns a list of all user profiles.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Profiles retrieved successfully"))
    @GetMapping
    ResponseEntity<List<ProfileDto>> getAll();

    @Operation(summary = "Get profile by ID", description = "Returns a single profile by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile found"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<ProfileDto> getById(
            @Parameter(description = "Profile ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create a new profile", description = "Creates a new user profile.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Profile created successfully"))
    @PostMapping
    ResponseEntity<ProfileDto> create(@RequestBody ProfileDto dto);

    @Operation(summary = "Update an existing profile", description = "Updates profile details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<ProfileDto> update(
            @Parameter(description = "Profile ID", required = true) @PathVariable Long id,
            @RequestBody ProfileDto dto);

    @Operation(summary = "Delete a profile", description = "Removes a profile by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Profile deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Profile ID", required = true) @PathVariable Long id);
}

