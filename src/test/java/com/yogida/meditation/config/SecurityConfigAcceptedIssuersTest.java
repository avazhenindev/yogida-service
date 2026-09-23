package com.yogida.meditation.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigAcceptedIssuersTest {

    private static final String CURRENT = "https://yogida.org/yogida/zxcasdqwe/realms/yogida";
    private static final String PREVIOUS = "https://yogida.org/zxcasdqwe/realms/yogida";

    @Test
    void acceptsThePreviousIssuerAfterTheCurrentOne() {
        assertThat(SecurityConfig.acceptedIssuers(CURRENT, PREVIOUS)).containsExactly(CURRENT, PREVIOUS);
    }

    @Test
    void acceptsOnlyTheCurrentIssuerWhenNoPreviousIsSet() {
        assertThat(SecurityConfig.acceptedIssuers(CURRENT, null)).containsExactly(CURRENT);
        assertThat(SecurityConfig.acceptedIssuers(CURRENT, "")).containsExactly(CURRENT);
        assertThat(SecurityConfig.acceptedIssuers(CURRENT, "  ")).containsExactly(CURRENT);
    }

    @Test
    void doesNotListTheSameIssuerTwice() {
        assertThat(SecurityConfig.acceptedIssuers(CURRENT, CURRENT)).containsExactly(CURRENT);
    }
}
