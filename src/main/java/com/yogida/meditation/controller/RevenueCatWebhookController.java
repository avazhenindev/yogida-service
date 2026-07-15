package com.yogida.meditation.controller;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.controller.api.RevenueCatWebhookControllerApi;
import com.yogida.meditation.dto.RevenueCatWebhookRequest;
import com.yogida.meditation.service.api.RevenueCatWebhookApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Log4j2
@RestController
@RequiredArgsConstructor
public class RevenueCatWebhookController implements RevenueCatWebhookControllerApi {

    private final RevenueCatWebhookApi revenueCatWebhookService;
    private final RevenueCatProperties revenueCatProperties;

    @Override
    public ResponseEntity<Void> handleWebhook(String authorization, RevenueCatWebhookRequest request) {
        if (!isAuthorized(authorization)) {
            log.warn("RevenueCatWebhookController > Rejected webhook with invalid authorization");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        revenueCatWebhookService.processEvent(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Constant-time comparison of the shared webhook secret. Rejects everything when not configured.
     */
    private boolean isAuthorized(String authorization) {
        String expected = revenueCatProperties.webhookAuthToken();
        log.error("RevenueCatWebhookController > Auth headers > Expected: {}, Provided: {}", expected, authorization);
        if (expected == null || expected.isBlank() || authorization == null) {
            return false;
        }
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            authorization.getBytes(StandardCharsets.UTF_8));
    }
}
