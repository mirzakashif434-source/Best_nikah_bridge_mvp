package com.nikahbridge;

/**
 * Best Nikah Bridge - Rate Limit Configuration
 *
 * Central configuration for preventing excessive or abusive actions.
 */
public final class RateLimitConfig {

    private RateLimitConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_RATE_LIMITING = true;

    public static final boolean LIMIT_LOGIN_ATTEMPTS = true;

    public static final boolean LIMIT_INTEREST_ACTIONS = true;

    public static final boolean LIMIT_MESSAGE_ACTIONS = true;

    public static final boolean LIMIT_REPORT_ACTIONS = true;

    public static final boolean LIMIT_PROFILE_UPDATES = true;

    public static final boolean LIMIT_VERIFICATION_REQUESTS = true;

    public static final boolean LIMIT_NOTIFICATION_REQUESTS = true;

    public static final boolean LIMIT_ADMIN_ACTIONS = true;

    public static final boolean PREVENT_ACTION_SPAM = true;

    public static final boolean REJECT_EXCESSIVE_REQUESTS = true;
}
