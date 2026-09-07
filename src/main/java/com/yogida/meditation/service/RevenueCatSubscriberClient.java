package com.yogida.meditation.service;

import com.yogida.meditation.config.RevenueCatProperties;
import com.yogida.meditation.dto.RevenueCatSubscriberResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin client for the RevenueCat Subscriber API — the single source of truth for entitlement.
 *
 * <p>Read by {@link EntitlementService} on a cache and projection miss, and by
 * {@link RevenueCatWebhookService} when refreshing the projection after an
 * entitlement-affecting event.
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
     *
     * <p>Never throws. The three-way {@link SubscriberLookup} result is what lets the caller
     * cache an authoritative negative while refusing to cache an outage.
     */
    public SubscriberLookup getSubscriber(String rcAppUserId) {
        try {
            RevenueCatSubscriberResponse body = restClient.get()
                    .uri("/subscribers/{appUserId}", rcAppUserId)
                    .retrieve()
                    .body(RevenueCatSubscriberResponse.class);
            if (body == null || body.subscriber() == null) {
                log.warn("RevenueCatSubscriberClient > Empty subscriber body for {}", rcAppUserId);
                return new SubscriberLookup.Unavailable("empty response body");
            }
            return new SubscriberLookup.Found(body);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("RevenueCatSubscriberClient > Subscriber {} unknown to RevenueCat (404)", rcAppUserId);
            return new SubscriberLookup.Unknown();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            // Almost always a rotated or mistyped secret key. Loud, because every entitlement
            // check in the system fails closed until it is fixed.
            log.error("RevenueCatSubscriberClient > RevenueCat rejected our API key ({}); "
                    + "entitlement checks are failing closed", e.getStatusCode());
            return new SubscriberLookup.Unavailable("api key rejected: " + e.getStatusCode());
        } catch (RestClientException e) {
            log.warn("RevenueCatSubscriberClient > Failed to fetch subscriber {}: {}",
                    rcAppUserId, e.getMessage());
            return new SubscriberLookup.Unavailable(e.getClass().getSimpleName());
        }
    }
}
