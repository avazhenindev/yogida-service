package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
            description = "Authenticated by a shared secret in the Authorization header and, when a "
                    + "signing secret is configured, by the HMAC-SHA256 signature in "
                    + "X-RevenueCat-Webhook-Signature. Idempotent per event id.",
            operationId = "handleRevenueCatWebhook")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "RevenueCat event payload. Bound as a byte array so the HMAC can be taken "
                    + "over the bytes as sent; the schema below is what those bytes parse into.",
            required = true,
            content = @Content(schema = @Schema(implementation = RevenueCatWebhookRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event accepted"),
            @ApiResponse(responseCode = "400", description = "Body is not a readable RevenueCat event"),
            @ApiResponse(responseCode = "401",
                    description = "Missing or invalid webhook authorization or signature")
    })
    @PostMapping
    ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "t=<unix_seconds>,v1=<hmac_sha256_hex>, the HMAC-SHA256 of "
                    + "\"<t>.<raw_body>\" under the signing secret. Required whenever "
                    + "app.revenuecat.webhook-signing-secret is set.")
            @RequestHeader(value = "X-RevenueCat-Webhook-Signature", required = false) String signature,
            // Raw bytes, not the parsed DTO: the signature covers what RevenueCat sent, and a
            // round-trip through Jackson would not reproduce it. Optional so that an empty body
            // is answered by the authorization check below rather than by a 400 from the
            // message converter, which runs before the handler and would leak that the endpoint
            // is live to anyone who can reach it.
            @RequestBody(required = false) byte[] rawBody);
}
