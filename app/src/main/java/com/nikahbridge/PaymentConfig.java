package com.nikahbridge;

/**
 * Best Nikah Bridge - Payment Configuration
 *
 * Controls safe and transparent monetization settings.
 */
public final class PaymentConfig {

    private PaymentConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_PAYMENTS = true;

    public static final boolean ENABLE_PREMIUM = true;

    public static final boolean ENABLE_AD_SUPPORTED_ACCESS = true;

    public static final int FREE_DAILY_AD_MESSAGE_LIMIT = 2;

    public static final boolean REQUIRE_USER_CONFIRMATION = true;

    public static final boolean SHOW_PRICING_BEFORE_PURCHASE = true;

    public static final boolean NO_HIDDEN_CHARGES = true;

    public static final boolean VERIFY_PURCHASES = true;

    public static final boolean ALLOW_REFUND_SUPPORT = true;

    public static final boolean LOG_PAYMENT_EVENTS = true;

    public static final boolean NEVER_STORE_CARD_DETAILS = true;
}
