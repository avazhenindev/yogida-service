package com.yogida.meditation.config;

import java.time.Duration;

/**
 * Startup validation for bound configuration values.
 *
 * <p>The @ConfigurationProperties records used to substitute a default for anything blank or
 * non-positive, which meant every default existed twice: once in {@code application.properties},
 * where an operator can see it, and once in a compact constructor, where they cannot. Eight values
 * carried two copies. They happened to agree, and nothing kept them agreeing.
 *
 * <p>application.properties is the single home now. These helpers keep the invariants the
 * substitution branches were also enforcing — a blank string is useless, a zero or negative
 * duration means "never" or worse — but fail loudly instead of quietly picking a value the
 * operator did not choose. That is the convention application.properties already documents for the
 * JWT issuers: unset means the context refuses to start and names the property.
 *
 * <p>Reachable only when a value is explicitly set to something empty or non-positive; every
 * property here has a default in application.properties.
 */
final class ConfigValues {

    private ConfigValues() {
    }

    static String requireText(String value, String property) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(property + " must not be blank");
        }
        return value;
    }

    static Duration requirePositive(Duration value, String property) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(
                    property + " must be a positive duration (got: " + value + ")");
        }
        return value;
    }
}
