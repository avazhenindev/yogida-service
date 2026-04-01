package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.SubscriptionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Subscriptions", description = "CRUD endpoints for user subscriptions")
@RequestMapping("/subscriptions")
public interface SubscriptionControllerApi {

    @Operation(summary = "Get all subscriptions", description = "Returns a list of all subscriptions.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Subscriptions retrieved successfully"))
    @GetMapping
    ResponseEntity<List<SubscriptionDto>> getAll();

    @Operation(summary = "Get subscription by ID", description = "Returns a single subscription by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription found"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<SubscriptionDto> getById(
            @Parameter(description = "Subscription ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create a new subscription", description = "Creates a new user subscription.")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Subscription created successfully"))
    @PostMapping
    ResponseEntity<SubscriptionDto> create(@RequestBody SubscriptionDto dto);

    @Operation(summary = "Update an existing subscription", description = "Updates subscription details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription updated successfully"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<SubscriptionDto> update(
            @Parameter(description = "Subscription ID", required = true) @PathVariable Long id,
            @RequestBody SubscriptionDto dto);

    @Operation(summary = "Delete a subscription", description = "Removes a subscription by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Subscription deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Subscription ID", required = true) @PathVariable Long id);
}

