package com.nikahbridge;

/**
 * Best Nikah Bridge - Feature Flag Configuration
 *
 * Centralized feature controls for safe rollout,
 * testing, and controlled production releases.
 */
public final class FeatureFlagConfig {

    private FeatureFlagConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_V2_FEATURES = true;

    public static final boolean ENABLE_PROFILE_FEATURES = true;

    public static final boolean ENABLE_MATCHING_FEATURES = true;

    public static final boolean ENABLE_INTEREST_FEATURES = true;

    public static final boolean ENABLE_SAFE_CHAT_FEATURES = true;

    public static final boolean ENABLE_VERIFICATION_FEATURES = true;

    public static final boolean ENABLE_REPORT_BLOCK_FEATURES = true;

    public static final boolean ENABLE_SAFETY_FEATURES = true;

    public static final boolean ENABLE_NOTIFICATIONS = true;

    public static final boolean ENABLE_ADMIN_SAFETY_FEATURES = true;

    public static final boolean FAIL_SAFE_ON_FEATURE_ERROR = true;
}
