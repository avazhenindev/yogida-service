package com.yogida.meditation.service;

import com.yogida.meditation.config.RevenueCatProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevenueCatWebhookSignatureVerifierTest {

    private static final String SECRET = "test-signing-secret";
    private static final byte[] BODY =
            "{\"event\":{\"id\":\"evt-1\",\"type\":\"RENEWAL\"}}".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");

    @Test
    void acceptsSignatureProducedByTheConfiguredSecret() {
        assertTrue(verifier(SECRET).verify(BODY, header(NOW.getEpochSecond(), BODY, SECRET)));
    }

    @Test
    void acceptsUppercaseHexSignature() {
        // Nothing in the header's grammar pins the case of the digest.
        String upper = header(NOW.getEpochSecond(), BODY, SECRET).toUpperCase(Locale.ROOT)
                .replace("T=", "t=").replace("V1=", "v1=");
        assertTrue(verifier(SECRET).verify(BODY, upper));
    }

    @Test
    void ignoresUnknownElementsAndWhitespace() {
        String signed = header(NOW.getEpochSecond(), BODY, SECRET);
        String padded = signed.replace(",", ", ") + ", v2=somethingelse";
        assertTrue(verifier(SECRET).verify(BODY, padded));
    }

    @Test
    void rejectsSignatureFromADifferentSecret() {
        assertFalse(verifier(SECRET).verify(BODY, header(NOW.getEpochSecond(), BODY, "other-secret")));
    }

    @Test
    void rejectsWhenTheBodyChangedAfterSigning() {
        String signed = header(NOW.getEpochSecond(), BODY, SECRET);
        byte[] tampered = "{\"event\":{\"id\":\"evt-1\",\"type\":\"CANCELLATION\"}}"
                .getBytes(StandardCharsets.UTF_8);
        assertFalse(verifier(SECRET).verify(tampered, signed));
    }

    @Test
    void rejectsSignatureBoundToADifferentTimestamp() {
        // The timestamp is inside the signed string, so moving it invalidates the digest even
        // when the digest itself is one this secret really produced.
        String digestOnly = header(NOW.getEpochSecond(), BODY, SECRET).split(",")[1];
        assertFalse(verifier(SECRET).verify(BODY, "t=" + (NOW.getEpochSecond() - 10) + "," + digestOnly));
    }

    @Test
    void rejectsDeliverySignedOutsideTheToleranceWindow() {
        long stale = NOW.minus(Duration.ofMinutes(6)).getEpochSecond();
        assertFalse(verifier(SECRET).verify(BODY, header(stale, BODY, SECRET)));
    }

    @Test
    void acceptsDeliverySignedInTheFutureButInsideTolerance() {
        // Clock skew runs both ways; only the magnitude is bounded.
        long ahead = NOW.plus(Duration.ofMinutes(4)).getEpochSecond();
        assertTrue(verifier(SECRET).verify(BODY, header(ahead, BODY, SECRET)));
    }

    @Test
    void rejectsMissingMalformedAndIncompleteHeaders() {
        RevenueCatWebhookSignatureVerifier verifier = verifier(SECRET);
        assertFalse(verifier.verify(BODY, null));
        assertFalse(verifier.verify(BODY, "  "));
        assertFalse(verifier.verify(BODY, "garbage"));
        assertFalse(verifier.verify(BODY, "t=" + NOW.getEpochSecond()));
        assertFalse(verifier.verify(BODY, "v1=deadbeef"));
        assertFalse(verifier.verify(BODY, "t=not-a-number,v1=deadbeef"));
        assertFalse(verifier.verify(null, header(NOW.getEpochSecond(), BODY, SECRET)));
    }

    @Test
    void skipsVerificationEntirelyWhenNoSecretIsConfigured() {
        RevenueCatWebhookSignatureVerifier verifier = verifier("");
        assertFalse(verifier.enabled());
        assertTrue(verifier.verify(BODY, null));
        assertTrue(verifier.verify(BODY, "t=1,v1=deadbeef"));
    }

    private static RevenueCatWebhookSignatureVerifier verifier(String secret) {
        return new RevenueCatWebhookSignatureVerifier(
                properties(secret), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static RevenueCatProperties properties(String signingSecret) {
        return new RevenueCatProperties(
                "premium",
                "auth-token",
                signingSecret,
                Duration.ofMinutes(5),
                "sk_test",
                "https://api.revenuecat.com/v1",
                Duration.ofHours(24),
                Duration.ofHours(24),
                Duration.ofDays(30),
                Duration.ofSeconds(30));
    }

    /** Mirrors what RevenueCat sends: HMAC-SHA256 over "<t>.<body>", lowercase hex. */
    private static String header(long timestamp, byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            mac.update(body);
            return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(mac.doFinal());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
