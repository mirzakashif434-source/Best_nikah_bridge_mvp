package com.nikahbridge;

/**
 * Best Nikah Bridge - V2 configuration.
 *
 * Central configuration for V2 features and subscription tiers.
 * Keep payment credentials, bank details and secrets OUT of source code.
 */
public final class V2Config {

    private V2Config() {
        // Prevent instantiation.
    }

    // App identity
    public static final String APP_NAME = "Best Nikah Bridge";
    public static final String V2_VERSION = "2.0";

    // Subscription prices in Saudi Riyals.
    // Actual purchase processing will use Google Play Billing products.
    public static final int STARTER_PRICE_SAR = 20;
    public static final int PREMIUM_PRICE_SAR = 40;
    public static final int GOLDEN_PRICE_SAR = 60;

    // Google Play Billing product IDs.
    // These IDs can later be connected to Play Console products.
    public static final String PRODUCT_STARTER = "bnb_starter_20";
    public static final String PRODUCT_PREMIUM = "bnb_premium_40";
    public static final String PRODUCT_GOLDEN = "bnb_golden_60";

    // Core V2 feature flags.
    public static final boolean ENABLE_VERIFICATION = true;
    public static final boolean ENABLE_ID_VERIFICATION = true;
    public static final boolean ENABLE_WALI_SUPPORT = true;
    public static final boolean ENABLE_PRIVACY_CONTROLS = true;
    public static final boolean ENABLE_REPORT_BLOCK = true;
    public static final boolean ENABLE_SAFE_MATCHING = true;
    public static final boolean ENABLE_MUTUAL_CONNECTIONS = true;
    public static final boolean ENABLE_SAFE_CHAT = true;
    public static final boolean ENABLE_VOICE_VIDEO_PLAN = true;
    public static final boolean ENABLE_SAFETY_ASSISTANT = true;
    public static final boolean ENABLE_URDU_ENGLISH = true;
    public static final boolean ENABLE_ACCOUNT_DELETION = true;

    // Free-access principle:
    // Essential safety and verification should not depend on payment.
    public static final boolean SAFETY_FEATURES_REQUIRE_PAYMENT = false;
    public static final boolean VERIFICATION_REQUIRES_PAYMENT = false;

    // Low-income access model.
    public static final boolean ENABLE_AD_SUPPORTED_ACCESS = true;
    public static final int FREE_DAILY_AD_LIMIT = 2;

    // Matching quality controls.
    public static final int MIN_PROFILE_COMPLETION_PERCENT = 70;
    public static final int HIGH_COMPATIBILITY_PERCENT = 80;

    // Trust levels.
    public static final String TRUST_BASIC = "Basic";
    public static final String TRUST_VERIFIED = "Verified";
    public static final String TRUST_ID_VERIFIED = "ID Verified";

    // Supported languages.
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_URDU = "ur";
}
