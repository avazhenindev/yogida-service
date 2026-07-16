package com.yogida.meditation.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Entitlement SSE", description = "Server-Sent Events for real-time entitlement updates")
@RequestMapping("/entitlement")
public interface SseControllerApi {

    @Operation(
            summary = "Subscribe to entitlement updates",
            description = "Opens a per-user SSE stream. The authenticated user receives a push event "
                    + "whenever their RevenueCat entitlement changes (purchase, renewal, cancellation, expiration, etc.).",
            operationId = "subscribeToEntitlementStream"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE stream opened"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> stream();

    @Operation(
            summary = "Send a test SSE message",
            description = "Pushes a TEST-type SSE event to all active connections of the authenticated user. "
                    + "Used for connectivity verification.",
            operationId = "sendTestMessage"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Test message sent"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    @PostMapping("/test-message")
    ResponseEntity<Void> sendTestMessage();
}
