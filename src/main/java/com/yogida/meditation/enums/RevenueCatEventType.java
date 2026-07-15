package com.yogida.meditation.enums;

import java.util.Set;

/**
 * All documented RevenueCat webhook event types.
 *
 * @see <a href="https://www.revenuecat.com/docs/integrations/webhooks/event-types-and-fields">
 *      RC Webhook Event Types and Fields</a>
 */
public enum RevenueCatEventType {

    // --- Dashboard test ---

    /** RevenueCat issued a test event. */
    TEST,

    // --- Subscription lifecycle ---

    /** A new subscription was purchased. */
    INITIAL_PURCHASE,

    /** An existing subscription was renewed, or a lapsed user resubscribed. */
    RENEWAL,

    /** A subscription or non-renewing purchase was canceled or refunded. */
    CANCELLATION,

    /** A non-expired canceled subscription was re-enabled. */
    UNCANCELLATION,

    /** A customer made a purchase that will not auto-renew. */
    NON_RENEWING_PURCHASE,

    /**
     * The subscription was scheduled to pause at the end of the current period.
     * Do not revoke access on this event; revoke only on {@code EXPIRATION} with
     * {@code expiration_reason=SUBSCRIPTION_PAUSED}.
     */
    SUBSCRIPTION_PAUSED,

    /** A subscription has expired. Revoke access on this event. */
    EXPIRATION,

    /**
     * An attempt to charge the subscriber failed.
     * This does not immediately expire access. Access should be revoked only on
     * {@code EXPIRATION} with {@code expiration_reason=BILLING_ERROR}.
     */
    BILLING_ISSUE,

    /**
     * A subscriber changed the product of their subscription.
     * The new subscription may not be in effect immediately.
     */
    PRODUCT_CHANGE,

    /** An existing subscription was extended (expiration date pushed back). */
    SUBSCRIPTION_EXTENDED,

    /** A refund was reversed. */
    REFUND_REVERSED,

    /** A new, unpaid invoice was issued. Applies to RevenueCat Billing only. */
    INVOICE_ISSUANCE,

    // --- Transfer ---

    /**
     * A transfer of transactions and entitlements between App User IDs was initiated.
     * The webhook is sent only for the destination user.
     */
    TRANSFER,

    // --- Temporary entitlement grant ---

    /**
     * RevenueCat issued a short-term entitlement grant during a store validation outage.
     * Expect a follow-up {@code INITIAL_PURCHASE} or {@code EXPIRATION} once connectivity is restored.
     */
    TEMPORARY_ENTITLEMENT_GRANT,

    // --- Other lifecycle ---

    /** A virtual currency transaction occurred. */
    VIRTUAL_CURRENCY_TRANSACTION,

    /** A customer was enrolled in an experiment. */
    EXPERIMENT_ENROLLMENT,

    /** A Paddle, RevenueCat Billing, or Stripe purchase was redeemed. */
    PURCHASE_REDEEMED,

    // --- Paywall UI ---

    /** A paywall was displayed to a customer. */
    PAYWALL_IMPRESSION,

    /** A paywall was closed by a customer. */
    PAYWALL_CLOSE,

    /** A customer dismissed the payment confirmation on a paywall. */
    PAYWALL_CANCEL,

    /** An exit offer was shown on a paywall. */
    PAYWALL_EXIT_OFFER,

    /** A customer changed a paywall control (tab, package, purchase button, etc.). */
    PAYWALL_COMPONENT_INTERACTED,

    // --- Legacy / Price ---

    /** Deprecated. A new App User ID was registered for an existing subscriber. */
    SUBSCRIBER_ALIAS,

    /** A price increase requires customer consent before the subscription can renew at the new price. */
    PRICE_INCREASE_CONSENT_REQUIRED,

    /** A customer consented to a pending price increase. */
    PRICE_INCREASE_CONSENT_APPROVED;

    /**
     * Event types that signal a potential change in a user's entitlement status.
     * When one of these events arrives, the entitlement cache must be evicted and
     * any connected SSE clients notified with fresh customer info.
     *
     * <p>Inclusion rationale:
     * <ul>
     *   <li>{@code BILLING_ISSUE} — a billing failure can lead to access loss;
     *       clients benefit from knowing immediately even though access is not yet revoked.</li>
     *   <li>{@code SUBSCRIPTION_PAUSED} — pause does not revoke access during the current
     *       period, but entitlement state will change at period end; notify early.</li>
     *   <li>{@code TEMPORARY_ENTITLEMENT_GRANT} — temporary access granted during store
     *       outages represents a real entitlement change.</li>
     * </ul>
     */
    private static final Set<RevenueCatEventType> ENTITLEMENT_AFFECTING = Set.of(
            INITIAL_PURCHASE,
            RENEWAL,
            CANCELLATION,
            UNCANCELLATION,
            NON_RENEWING_PURCHASE,
            SUBSCRIPTION_PAUSED,
            EXPIRATION,
            BILLING_ISSUE,
            PRODUCT_CHANGE,
            SUBSCRIPTION_EXTENDED,
            TEMPORARY_ENTITLEMENT_GRANT,
            TRANSFER
    );

    /**
     * Returns {@code true} when the given raw event type string maps to an
     * entitlement-affecting event. Unknown or {@code null} strings return {@code false}.
     *
     * @param type the raw {@code type} field from the RevenueCat webhook payload
     * @return {@code true} if the event affects entitlement state
     */
    public static boolean isEntitlementAffecting(String type) {
        if (type == null) {
            return false;
        }
        try {
            return ENTITLEMENT_AFFECTING.contains(RevenueCatEventType.valueOf(type));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
