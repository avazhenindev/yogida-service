package com.yogida.meditation.service;

import com.yogida.meditation.config.RevenueCatProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Verifies the {@code X-RevenueCat-Webhook-Signature} header RevenueCat attaches when "HMAC
 * webhook signing" is enabled on the integration.
 *
 * <p>The header is {@code t=<unix_seconds>,v1=<hmac_sha256_hex>}, and the MAC is taken over
 * {@code "<t>.<raw_json_body>"} with the dashboard's signing secret. Two things about that are
 * easy to get wrong and are why this class exists rather than a few lines in the controller:
 *
 * <ul>
 *   <li>The MAC covers the <em>bytes as they arrived</em>. Deserializing to
 *       {@code RevenueCatWebhookRequest} and re-serializing produces different bytes — key order,
 *       whitespace, unmapped fields this DTO drops — and would fail every genuine delivery. The
 *       controller therefore takes {@code byte[]} and parses only after this returns true.
 *   <li>{@code t} is when RevenueCat signed <em>that HTTP request</em>, not when the event
 *       happened, and it is recomputed for every retry. So the tolerance window covers clock skew
 *       and one POST's latency; it does not need to span the retry ladder.
 * </ul>
 *
 * <p>Without the signature an attacker who learns the Authorization token can forge entitlement
 * events; with it they would also need the signing secret, which never travels on the wire.
 */
@Log4j2
@Component
public class RevenueCatWebhookSignatureVerifier {

    static final String SIGNATURE_HEADER = "X-RevenueCat-Webhook-Signature";

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RevenueCatProperties properties;
    private final Clock clock;

    // Explicit, because the second constructor exists: with two of them Spring will not guess,
    // and the context fails to start rather than falling back to either.
    @Autowired
    public RevenueCatWebhookSignatureVerifier(RevenueCatProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /** Visible for tests, which need the tolerance window measured against a fixed clock. */
    RevenueCatWebhookSignatureVerifier(RevenueCatProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @PostConstruct
    void logConfiguredState() {
        if (enabled()) {
            log.info("RevenueCatWebhookSignatureVerifier > HMAC signature verification enabled "
                    + "(tolerance {})", properties.webhookSignatureTolerance());
        } else {
            // Not an error — a local stack has no secret and should still accept test deliveries.
            // Loud because in production it means the only thing standing between the internet
            // and a forged entitlement event is the Authorization token.
            log.warn("RevenueCatWebhookSignatureVerifier > No app.revenuecat.webhook-signing-secret "
                    + "set; the {} header will not be checked", SIGNATURE_HEADER);
        }
    }

    /**
     * Whether a signing secret is configured. Blank means the header is not checked at all, so a
     * stack brought up without RevenueCat credentials still runs.
     */
    public boolean enabled() {
        String secret = properties.webhookSigningSecret();
        return secret != null && !secret.isBlank();
    }

    /**
     * @param rawBody   the request body exactly as received, before any JSON parsing
     * @param header    the raw {@code X-RevenueCat-Webhook-Signature} value, or null if absent
     * @return true when verification is disabled, or when the header carries a signature this
     *         secret produces for this body inside the tolerance window
     */
    public boolean verify(byte[] rawBody, String header) {
        if (!enabled()) {
            return true;
        }
        if (rawBody == null || header == null || header.isBlank()) {
            log.warn("RevenueCatWebhookSignatureVerifier > Rejected delivery with no {}",
                    SIGNATURE_HEADER);
            return false;
        }

        String timestamp = element(header, "t");
        String signature = element(header, "v1");
        if (timestamp == null || signature == null) {
            log.warn("RevenueCatWebhookSignatureVerifier > Rejected delivery with malformed {}",
                    SIGNATURE_HEADER);
            return false;
        }
        if (!withinTolerance(timestamp)) {
            return false;
        }

        String expected;
        try {
            expected = hexDigest(timestamp, rawBody);
        } catch (GeneralSecurityException e) {
            // The JDK always ships HmacSHA256, so this means the secret itself is unusable.
            log.error("RevenueCatWebhookSignatureVerifier > Cannot compute HMAC; "
                    + "check app.revenuecat.webhook-signing-secret", e);
            return false;
        }

        boolean matches = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            // No body, no header value: either would let an unauthenticated caller write chosen
            // content into the log. The mismatch itself is the whole signal.
            log.warn("RevenueCatWebhookSignatureVerifier > Rejected delivery whose {} did not "
                    + "match the configured signing secret", SIGNATURE_HEADER);
        }
        return matches;
    }

    /**
     * Pulls one {@code key=value} element out of the comma-separated header. Unknown elements are
     * skipped rather than rejected, so a future {@code v2=} alongside {@code v1=} keeps working.
     */
    private static String element(String header, String key) {
        for (String part : header.split(",")) {
            String candidate = part.trim();
            int separator = candidate.indexOf('=');
            if (separator > 0 && candidate.substring(0, separator).trim().equals(key)) {
                String value = candidate.substring(separator + 1).trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }

    private boolean withinTolerance(String timestamp) {
        long signedAt;
        try {
            signedAt = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.warn("RevenueCatWebhookSignatureVerifier > Rejected delivery with a non-numeric "
                    + "timestamp in {}", SIGNATURE_HEADER);
            return false;
        }
        long skewSeconds = Math.abs(clock.instant().getEpochSecond() - signedAt);
        long toleranceSeconds = properties.webhookSignatureTolerance().toSeconds();
        if (skewSeconds > toleranceSeconds) {
            log.warn("RevenueCatWebhookSignatureVerifier > Rejected delivery signed {}s from now, "
                    + "outside the {}s tolerance", skewSeconds, toleranceSeconds);
            return false;
        }
        return true;
    }

    /**
     * HMAC-SHA256 over {@code "<timestamp>." + rawBody}, lowercase hex. The separator and the body
     * are fed as bytes so no charset decoding of the payload happens on the way in.
     */
    private String hexDigest(String timestamp, byte[] rawBody) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(
                properties.webhookSigningSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) '.');
        mac.update(rawBody);
        return HexFormat.of().formatHex(mac.doFinal());
    }
}
