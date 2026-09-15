package com.yogida.meditation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The data of one {@code entitlement-update} SSE frame: a RevenueCat event, reduced to what the
 * app needs to decide whether to show a banner and what it should say.
 *
 * <p>This is the only thing ever written to the stream — never the inbound
 * {@link RevenueCatWebhookRequest} and never the raw body. The allow-list is the component list
 * itself, so anything RevenueCat adds later stays on the server until it is named here. What is
 * left out on purpose: every user id and alias (a TRANSFER's included), subscriber attributes,
 * price, currency, tax, commission and take-home, country code, app id, transaction ids, offer
 * code and metadata.
 *
 * <p>Absent values are omitted rather than sent as null. {@code type} is RevenueCat's raw string,
 * so a type this service has never heard of still reaches the app, which ignores what it does
 * not know.
 *
 * @param v                 wire format version, always {@code 1}. The app refuses a banner for any
 *                          other value but still refreshes.
 * @param transferDirection which side of a TRANSFER this recipient is on; null for every other type
 * @param bannerAllowed     whether backend configuration lets the app show a banner for this event.
 *                          See {@code EntitlementEventProperties}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntitlementEventMessage(
        int v,
        String eventId,
        String type,
        String productId,
        String newProductId,
        List<String> entitlementIds,
        String periodType,
        Long purchasedAtMs,
        Long expirationAtMs,
        Long gracePeriodExpirationAtMs,
        Long eventTimestampMs,
        String cancelReason,
        String expirationReason,
        String store,
        String environment,
        // Pinned so the "is" prefix is never stripped into "trialConversion" by accessor naming.
        @JsonProperty("isTrialConversion") Boolean isTrialConversion,
        Integer renewalNumber,
        TransferDirection transferDirection,
        boolean bannerAllowed
) {

    public static final int VERSION = 1;

    /** The side of a TRANSFER a recipient is on. */
    public enum TransferDirection {
        /** The purchases moved to this user. */
        IN,
        /** The purchases moved away from this user. */
        OUT
    }

    public static EntitlementEventMessage from(
            RevenueCatWebhookRequest.Event event, TransferDirection transferDirection, boolean bannerAllowed) {
        return new EntitlementEventMessage(
                VERSION,
                event.id(),
                event.type(),
                event.productId(),
                event.newProductId(),
                event.entitlementIds(),
                event.periodType(),
                event.purchasedAtMs(),
                event.expirationAtMs(),
                event.gracePeriodExpirationAtMs(),
                event.eventTimestampMs(),
                event.cancelReason(),
                event.expirationReason(),
                event.store(),
                event.environment(),
                event.isTrialConversion(),
                event.renewalNumber(),
                transferDirection,
                bannerAllowed);
    }

    /** A frame carrying only a type, for the admin connectivity check. Never shows a banner. */
    public static EntitlementEventMessage diagnostic(String type) {
        return new EntitlementEventMessage(
                VERSION, null, type, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, false);
    }
}
