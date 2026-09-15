package com.yogida.meditation.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntitlementEventPropertiesTest {

    @Test
    void bothOn_allowsEveryEnvironment() {
        EntitlementEventProperties properties = new EntitlementEventProperties(true, true);

        assertThat(properties.bannerAllowedFor("PRODUCTION")).isTrue();
        assertThat(properties.bannerAllowedFor("SANDBOX")).isTrue();
        assertThat(properties.bannerAllowedFor(null)).isTrue();
    }

    @Test
    void globalOff_allowsNothing() {
        EntitlementEventProperties properties = new EntitlementEventProperties(false, true);

        assertThat(properties.bannerAllowedFor("PRODUCTION")).isFalse();
        assertThat(properties.bannerAllowedFor("SANDBOX")).isFalse();
        assertThat(properties.bannerAllowedFor(null)).isFalse();
    }

    @Test
    void sandboxOff_silencesOnlySandbox() {
        EntitlementEventProperties properties = new EntitlementEventProperties(true, false);

        assertThat(properties.bannerAllowedFor("SANDBOX")).isFalse();
        assertThat(properties.bannerAllowedFor("sandbox")).isFalse();
        assertThat(properties.bannerAllowedFor("PRODUCTION")).isTrue();
        // An event with no environment is treated as production rather than silenced.
        assertThat(properties.bannerAllowedFor(null)).isTrue();
    }

    /** An empty APP_ENTITLEMENT_EVENTS_* variable binds to null; it must stop startup. */
    @Test
    void nullFlag_isRejected() {
        assertThatThrownBy(() -> new EntitlementEventProperties(null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.entitlement-events.banner-enabled");
        assertThatThrownBy(() -> new EntitlementEventProperties(true, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.entitlement-events.sandbox-banner-enabled");
    }
}
