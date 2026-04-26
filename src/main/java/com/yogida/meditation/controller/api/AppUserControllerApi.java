package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.AppUserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "CRUD endpoints for application users")
@RequestMapping("/users")
public interface AppUserControllerApi {

    @Operation(summary = "Get all users", description = "Returns a list of all registered users.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Users retrieved successfully"))
    @GetMapping
    ResponseEntity<List<AppUserDto>> getAll();

    @Operation(summary = "Get user by ID", description = "Returns a single user by their ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<AppUserDto> getById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id);

    @Operation(summary = "Find user by email", description = "Returns a single user matching the given email address.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/search")
    ResponseEntity<AppUserDto> getByEmail(
            @Parameter(description = "User email", required = true) @RequestParam String email);

    @Operation(summary = "Create a new user", description = "Registers a new application user.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "User created successfully"))
    @PostMapping
    ResponseEntity<AppUserDto> create(@RequestBody AppUserDto dto);

    @Operation(summary = "Update an existing user", description = "Updates user details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<AppUserDto> update(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @RequestBody AppUserDto dto);

    @Operation(summary = "Delete a user", description = "Removes a user by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "User ID", required = true) @PathVariable Long id);
}

