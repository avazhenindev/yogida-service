package com.yogida.meditation.controller;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.controller.api.RevenueCatWebhookControllerApi;
import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.service.RevenueCatWebhookSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.yogida.meditation.service.RevenueCatWebhookService;

@Log4j2
@RestController
@RequiredArgsConstructor
public class RevenueCatWebhookController implements RevenueCatWebhookControllerApi {

    private final RevenueCatWebhookService revenueCatWebhookService;
    private final RevenueCatWebhookSignatureVerifier signatureVerifier;
    private final RevenueCatProperties revenueCatProperties;
    // Jackson 3's mapper (tools.jackson), which is the bean Spring Boot 4 auto-configures and
    // the one the message converter would have used had this method still taken the parsed DTO.
    private final ObjectMapper objectMapper;

    @Override
    public ResponseEntity<Void> handleWebhook(String authorization, String signature, byte[] rawBody) {
        // Nothing about the payload is logged before the shared secret is verified: this
        // endpoint is public by necessity, so anyone on the internet could otherwise write
        // arbitrary content into the request log by POSTing to it. Even after the check, only
        // the event id and type are recorded — the full payload carries the subscriber's app
        // user id, product id, store and entitlement ids.
        if (!isAuthorized(authorization)) {
            log.warn("RevenueCatWebhookController > Rejected webhook with invalid authorization");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // The Authorization token is a bearer secret: it is replayable, and anything that has
        // ever seen a delivery has seen it. The HMAC is over the body, so it also establishes
        // that these bytes are the ones RevenueCat signed and are not a replay of an older
        // delivery. Verified against rawBody, before anything parses it.
        if (!signatureVerifier.verify(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Only reachable with signing disabled: verify() rejects a null body once a secret is
        // set. Jackson's readValue(byte[]) throws IllegalArgumentException on null rather than
        // the JacksonException caught below, which would surface as a 500.
        if (rawBody == null || rawBody.length == 0) {
            log.warn("RevenueCatWebhookController > Rejected authenticated webhook with an empty body");
            return ResponseEntity.badRequest().build();
        }

        RevenueCatWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, RevenueCatWebhookRequest.class);
        } catch (JacksonException e) {
            // Reachable only by a caller holding both secrets, so this is a payload RevenueCat
            // changed rather than an attack. The exception message can quote the body, so it is
            // deliberately not logged.
            log.warn("RevenueCatWebhookController > Rejected authenticated webhook whose body "
                    + "did not parse as a RevenueCat event");
            return ResponseEntity.badRequest().build();
        }

        log.info("RevenueCatWebhookController > Accepted webhook event {} of type {}",
            request == null || request.event() == null ? null : request.event().id(),
            request == null || request.event() == null ? null : request.event().type());
        revenueCatWebhookService.processEvent(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Constant-time comparison of the shared webhook secret. Rejects everything when not configured.
     */
    private boolean isAuthorized(String authorization) {
        String expected = revenueCatProperties.webhookAuthToken();
        if (expected == null || expected.isBlank() || authorization == null) {
            return false;
        }
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            authorization.getBytes(StandardCharsets.UTF_8));
    }
}
