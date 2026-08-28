package com.nikahbridge;

/**
 * Best Nikah Bridge - Security Event Configuration
 *
 * Central configuration for security-related event handling.
 */
public final class SecurityEventConfig {

    private SecurityEventConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_SECURITY_EVENTS = true;

    public static final boolean LOG_LOGIN_EVENTS = true;

    public static final boolean LOG_ACCOUNT_SECURITY_EVENTS = true;

    public static final boolean LOG_ABUSE_EVENTS = true;

    public static final boolean LOG_RATE_LIMIT_EVENTS = true;

    public static final boolean LOG_REPORT_BLOCK_EVENTS = true;

    public static final boolean LOG_ADMIN_SECURITY_EVENTS = true;

    public static final boolean PROTECT_SENSITIVE_DATA = true;

    public static final boolean FAIL_SAFE_ON_SECURITY_ERROR = true;
}
