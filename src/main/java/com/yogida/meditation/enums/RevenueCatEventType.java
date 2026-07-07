package com.yogida.meditation.enums;

/**
 * RevenueCat webhook event types relevant to entitlement projection.
 * Unknown types map to {@link #UNKNOWN} and are audited as ignored.
 */
public enum RevenueCatEventType {
    INITIAL_PURCHASE,
    RENEWAL,
    PRODUCT_CHANGE,
    UNCANCELLATION,
    NON_RENEWING_PURCHASE,
    CANCELLATION,
    EXPIRATION,
    BILLING_ISSUE,
    UNKNOWN;

    public static RevenueCatEventType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
