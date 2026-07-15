package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.SseControllerApi;
import com.yogida.meditation.service.CurrentUserService;
import com.yogida.meditation.service.api.SseApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Log4j2
@RestController
@RequiredArgsConstructor
public class SseController implements SseControllerApi {

    private final CurrentUserService currentUserService;
    private final SseApi sseApi;

    @Override
    public SseEmitter stream() {
        String keycloakUserId = currentUserService.getCurrentUserOrThrow().getKeycloakUserId();
        log.info("SseController > SSE stream opened for user {}", keycloakUserId);
        return sseApi.subscribe(keycloakUserId);
    }
}
