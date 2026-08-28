package com.nikahbridge;

/**
 * Best Nikah Bridge - Abuse Prevention Configuration
 *
 * Central configuration for anti-abuse and platform safety controls.
 */
public final class AbusePreventionConfig {

    private AbusePreventionConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_ABUSE_PREVENTION = true;

    public static final boolean BLOCK_REPEATED_SPAM = true;

    public static final boolean BLOCK_SUSPICIOUS_ACTIVITY = true;

    public static final boolean PROTECT_INTEREST_ACTIONS = true;

    public static final boolean PROTECT_CHAT_ACTIONS = true;

    public static final boolean PROTECT_REPORT_ACTIONS = true;

    public static final boolean PROTECT_PROFILE_ACTIONS = true;

    public static final boolean DETECT_REPEATED_ACTIONS = true;

    public static final boolean REQUIRE_SAFE_ACTION_FLOW = true;

    public static final boolean LOG_SECURITY_EVENTS = true;

    public static final boolean FAIL_SAFE_ON_SUSPICIOUS_ACTIVITY = true;
}
