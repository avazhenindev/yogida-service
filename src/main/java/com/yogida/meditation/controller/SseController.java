package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.SseControllerApi;
import com.yogida.meditation.enums.SseMessageType;
import com.yogida.meditation.service.CurrentUserService;
import com.yogida.meditation.service.api.SseApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Log4j2
@RestController
@RequiredArgsConstructor
public class SseController implements SseControllerApi {

    private final CurrentUserService currentUserService;
    private final SseApi sseApi;

    @Override
    public ResponseEntity<SseEmitter> stream(String clientId) {
        String keycloakUserId = currentUserService.getCurrentUserOrThrow().getKeycloakUserId();
        // Clients that do not send the header get a random id (legacy accumulate behaviour).
        String effectiveClientId = clientId != null && !clientId.isBlank()
                ? clientId
                : UUID.randomUUID().toString();
        log.info("SseController > SSE stream opened for user {} client {}", keycloakUserId, effectiveClientId);
        return ResponseEntity.ok()
                // Disables response buffering in nginx (X-Accel convention); without it
                // nginx holds SSE bytes in its proxy buffer and clients never see events.
                .header("X-Accel-Buffering", "no")
                .cacheControl(CacheControl.noCache())
                .body(sseApi.subscribe(keycloakUserId, effectiveClientId));
    }

    @Override
    public ResponseEntity<Void> sendTestMessage() {
        String keycloakUserId = currentUserService.getCurrentUserOrThrow().getKeycloakUserId();
        log.info("SseController > Sending TEST message to user {}", keycloakUserId);
        sseApi.publishToUser(keycloakUserId, SseMessageType.TEST.name(), null);
        return ResponseEntity.noContent().build();
    }
}
