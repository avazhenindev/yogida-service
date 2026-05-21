package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.UserSubscriptionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User Subscriptions", description = "CRUD endpoints for user subscription instances")
@RequestMapping("/user-subscriptions")
public interface UserSubscriptionControllerApi {

    @Operation(summary = "Get all user subscriptions")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "User subscriptions retrieved successfully"))
    @GetMapping
    ResponseEntity<List<UserSubscriptionDto>> getAll();

    @Operation(summary = "Get user subscription by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User subscription found"),
            @ApiResponse(responseCode = "404", description = "User subscription not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<UserSubscriptionDto> getById(
            @Parameter(description = "User Subscription ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create a new user subscription")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "User subscription created successfully"))
    @PostMapping
    ResponseEntity<UserSubscriptionDto> create(@RequestBody UserSubscriptionDto dto);

    @Operation(summary = "Update an existing user subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User subscription updated successfully"),
            @ApiResponse(responseCode = "404", description = "User subscription not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<UserSubscriptionDto> update(
            @Parameter(description = "User Subscription ID", required = true) @PathVariable Long id,
            @RequestBody UserSubscriptionDto dto);

    @Operation(summary = "Delete a user subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User subscription deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User subscription not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "User Subscription ID", required = true) @PathVariable Long id);
}

