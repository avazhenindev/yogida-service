package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "RevenueCat Webhooks", description = "Inbound RevenueCat webhook processing")
@RequestMapping("/webhooks/revenuecat")
public interface RevenueCatWebhookControllerApi {

    @Operation(summary = "Process a RevenueCat webhook event",
            description = "Authenticated by a shared secret in the Authorization header. Idempotent per event id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event accepted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid webhook authorization")
    })
    @PostMapping
    ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody RevenueCatWebhookRequest request);
}
