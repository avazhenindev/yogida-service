package com.yogida.meditation.controller;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.controller.api.RevenueCatWebhookControllerApi;
import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.yogida.meditation.service.RevenueCatWebhookService;

@Log4j2
@RestController
@RequiredArgsConstructor
public class RevenueCatWebhookController implements RevenueCatWebhookControllerApi {

    private final RevenueCatWebhookService revenueCatWebhookService;
    private final RevenueCatProperties revenueCatProperties;

    @Override
    public ResponseEntity<Void> handleWebhook(String authorization, RevenueCatWebhookRequest request) {
        // Nothing about the payload is logged before the shared secret is verified: this
        // endpoint is public by necessity, so anyone on the internet could otherwise write
        // arbitrary content into the request log by POSTing to it. Even after the check, only
        // the event id and type are recorded — the full payload carries the subscriber's app
        // user id, product id, store and entitlement ids.
        if (!isAuthorized(authorization)) {
            log.warn("RevenueCatWebhookController > Rejected webhook with invalid authorization");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
