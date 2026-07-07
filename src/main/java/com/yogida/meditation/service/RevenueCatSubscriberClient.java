package com.yogida.meditation.service;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.dto.RevenueCatSubscriberResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Thin client for the RevenueCat Subscriber API, used only for reconciliation
 * of stale local entitlement projections.
 */
@Log4j2
@Component
public class RevenueCatSubscriberClient {

    private final RestClient restClient;

    public RevenueCatSubscriberClient(RevenueCatProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .build();
    }

    /**
     * Fetches the subscriber state for the given RevenueCat app user id.
     * Returns empty when the subscriber cannot be fetched.
     */
    public Optional<RevenueCatSubscriberResponse> getSubscriber(String rcAppUserId) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/subscribers/{appUserId}", rcAppUserId)
                    .retrieve()
                    .body(RevenueCatSubscriberResponse.class));
        } catch (RuntimeException e) {
            log.warn("RevenueCatSubscriberClient > Failed to fetch subscriber {}: {}", rcAppUserId, e.getMessage());
            return Optional.empty();
        }
    }
}
