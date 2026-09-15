package com.yogida.meditation.dto;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The SSE frame is the one place subscriber data leaves the service for a phone, so what it may
 * carry is pinned here against a webhook with every field RevenueCat sends filled in.
 *
 * <p>The fixture is synthetic on purpose. A real delivery would put a real subscriber's ids and
 * email into the repository.
 */
class EntitlementEventMessageTest {

    private static final Set<String> ALLOWED_KEYS = Set.of(
            "v", "eventId", "type", "productId", "newProductId", "entitlementIds", "periodType",
            "purchasedAtMs", "expirationAtMs", "gracePeriodExpirationAtMs", "eventTimestampMs",
            "cancelReason", "expirationReason", "store", "environment", "isTrialConversion",
            "renewalNumber", "transferDirection", "bannerAllowed");

    /** Values from the fixture that must never reach the app. */
    private static final Set<String> STRIPPED_VALUES = Set.of(
            "synthetic-app-user-0001",
            "$RCAnonymousID",
            "synthetic-transferred-from",
            "synthetic-transferred-to",
            "synthetic.subscriber@example.invalid",
            "Synthetic Subscriber",
            "app-synthetic-0001",
            "synthetic-transaction-0001",
            "synthetic-original-transaction-0001",
            "SYNTHETIC-OFFER",
            "synthetic-offering",
            "synthetic-metadata-value",
            "XTS",
            "\"ZZ\"",
            "4.99",
            "0.15",
            "0.65");

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static RevenueCatWebhookRequest.Event event;

    @BeforeAll
    static void readFixture() throws IOException {
        try (InputStream in = EntitlementEventMessageTest.class.getResourceAsStream("/revenuecat/product-change.json")) {
            assertThat(in).as("fixture on the test classpath").isNotNull();
            event = MAPPER.readValue(in, RevenueCatWebhookRequest.class).event();
        }
    }

    @Test
    void carriesOnlyAllowListedKeys() {
        Map<String, Object> frame = frame(EntitlementEventMessage.from(event, null, true));

        assertThat(ALLOWED_KEYS).containsAll(frame.keySet());
    }

    @Test
    void carriesNoStrippedValue() {
        String json = MAPPER.writeValueAsString(EntitlementEventMessage.from(event, null, true));

        for (String stripped : STRIPPED_VALUES) {
            assertThat(json).as("frame must not contain %s", stripped).doesNotContain(stripped);
        }
    }

    @Test
    void isASingleLine() {
        String json = MAPPER.writeValueAsString(EntitlementEventMessage.from(event, null, true));

        assertThat(json).doesNotContain("\n");
    }

    /** The newly mapped webhook fields make it through parsing and into the frame. */
    @Test
    void forwardsTheMappedFields() {
        Map<String, Object> frame = frame(EntitlementEventMessage.from(event, null, true));

        assertThat(frame)
                .containsEntry("v", 1)
                .containsEntry("eventId", "00000000-0000-4000-8000-00000000e001")
                .containsEntry("type", "PRODUCT_CHANGE")
                .containsEntry("productId", "synthetic.weekly")
                .containsEntry("newProductId", "synthetic.monthly")
                .containsEntry("periodType", "NORMAL")
                .containsEntry("eventTimestampMs", 1767225600000L)
                .containsEntry("renewalNumber", 3)
                .containsEntry("environment", "SANDBOX")
                .containsEntry("bannerAllowed", true);
    }

    @Test
    void namesTheTrialConversionKeyWithItsPrefix() {
        Map<String, Object> frame = frame(EntitlementEventMessage.from(event, null, true));

        assertThat(frame).containsEntry("isTrialConversion", false).doesNotContainKey("trialConversion");
    }

    @Test
    void omitsNulls() {
        String json = MAPPER.writeValueAsString(EntitlementEventMessage.from(event, null, false));

        assertThat(frame(EntitlementEventMessage.from(event, null, false)))
                .doesNotContainKeys("cancelReason", "expirationReason", "gracePeriodExpirationAtMs",
                        "transferDirection");
        assertThat(json).doesNotContain("null");
    }

    @Test
    void carriesTheTransferDirectionWhenGiven() {
        Map<String, Object> frame = frame(
                EntitlementEventMessage.from(event, EntitlementEventMessage.TransferDirection.OUT, true));

        assertThat(frame).containsEntry("transferDirection", "OUT");
    }

    @Test
    void diagnostic_carriesOnlyItsTypeAndNeverAllowsABanner() {
        Map<String, Object> frame = frame(EntitlementEventMessage.diagnostic("TEST"));

        assertThat(frame).containsOnlyKeys("v", "type", "bannerAllowed")
                .containsEntry("type", "TEST")
                .containsEntry("bannerAllowed", false);
    }

    private static Map<String, Object> frame(EntitlementEventMessage message) {
        return MAPPER.readValue(MAPPER.writeValueAsString(message), new TypeReference<Map<String, Object>>() {
        });
    }
}
