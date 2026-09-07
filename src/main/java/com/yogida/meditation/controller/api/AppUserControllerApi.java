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

@Tag(name = "Users", description = "The caller's own account, plus administrative user management")
@RequestMapping("/users")
public interface AppUserControllerApi {

    @Operation(
        operationId = "getCurrentUser",
        summary = "Get the signed-in user",
        description = """
            Returns the account belonging to the bearer token, creating it on first sight.
            This is the only user endpoint a client application needs: identity comes from the
            token, so nothing has to be looked up or supplied by the caller.
            """)
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Current user returned"))
    @GetMapping("/me")
    ResponseEntity<AppUserDto> getCurrentUser();

    @Operation(operationId = "listUsers", summary = "Get all users",
        description = "Administrative. Returns every registered user.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Users retrieved successfully"))
    @GetMapping
    ResponseEntity<List<AppUserDto>> getAll();

    @Operation(operationId = "getUserById", summary = "Get user by ID",
        description = "Administrative. Returns a single user by their ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<AppUserDto> getById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id);

    @Operation(operationId = "findUserByEmail", summary = "Find user by email",
        description = "Administrative. Returns a single user matching the given email address.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/search")
    ResponseEntity<AppUserDto> getByEmail(
            @Parameter(description = "User email", required = true) @RequestParam String email);

    @Operation(operationId = "createUser", summary = "Create a new user",
        description = "Administrative. Clients do not need this: signing in provisions the account.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "User created successfully"))
    @PostMapping
    ResponseEntity<AppUserDto> create(@RequestBody AppUserDto dto);

    @Operation(operationId = "updateUser", summary = "Update an existing user",
        description = "Administrative. Identity fields are ignored; they are not client-settable.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<AppUserDto> update(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @RequestBody AppUserDto dto);

    @Operation(operationId = "deleteUser", summary = "Delete a user", description = "Administrative.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "User ID", required = true) @PathVariable Long id);
}

