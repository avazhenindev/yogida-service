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

@Tag(name = "Subscriptions", description = "CRUD endpoints for subscription plan catalog")
@RequestMapping("/subscriptions")
public interface SubscriptionControllerApi {

    @Operation(summary = "Get all subscription plans")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Subscription plans retrieved successfully"))
    @GetMapping
    ResponseEntity<List<SubscriptionDto>> getAll();

    @Operation(summary = "Find subscription plan by name")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription plan found"),
            @ApiResponse(responseCode = "404", description = "Subscription plan not found")
    })
    @GetMapping("/search")
    ResponseEntity<SubscriptionDto> getByName(
            @Parameter(description = "Subscription name", required = true) @RequestParam String name);

    @Operation(summary = "Get subscription plan by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription plan found"),
            @ApiResponse(responseCode = "404", description = "Subscription plan not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<SubscriptionDto> getById(
            @Parameter(description = "Subscription ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create a new subscription plan")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Subscription plan created successfully"))
    @PostMapping
    ResponseEntity<SubscriptionDto> create(@RequestBody SubscriptionDto dto);

    @Operation(summary = "Update an existing subscription plan")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subscription plan updated successfully"),
            @ApiResponse(responseCode = "404", description = "Subscription plan not found")
    })
    @PutMapping("/{id}")
    ResponseEntity<SubscriptionDto> update(
            @Parameter(description = "Subscription ID", required = true) @PathVariable Long id,
            @RequestBody SubscriptionDto dto);

    @Operation(summary = "Delete a subscription plan")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Subscription plan deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Subscription plan not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Subscription ID", required = true) @PathVariable Long id);
}
