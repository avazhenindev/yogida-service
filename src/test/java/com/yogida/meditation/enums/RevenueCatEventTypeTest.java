package com.yogida.meditation.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RevenueCatEventTypeTest {

    /** A refund projects "not entitled"; its reversal has to refresh the projection back. */
    @Test
    void refundReversed_isEntitlementAffecting() {
        assertThat(RevenueCatEventType.isEntitlementAffecting("REFUND_REVERSED")).isTrue();
    }

    @Test
    void testEvent_isNotEntitlementAffecting() {
        assertThat(RevenueCatEventType.isEntitlementAffecting("TEST")).isFalse();
    }

    @Test
    void unknownType_isNotEntitlementAffecting() {
        assertThat(RevenueCatEventType.isEntitlementAffecting("FUTURE_UNKNOWN_TYPE")).isFalse();
    }

    @Test
    void nullType_isNotEntitlementAffecting() {
        assertThat(RevenueCatEventType.isEntitlementAffecting(null)).isFalse();
    }
}
