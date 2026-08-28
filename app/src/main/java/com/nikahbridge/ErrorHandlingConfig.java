package com.nikahbridge;

/**
 * Best Nikah Bridge - Error Handling Configuration
 *
 * Central configuration for safe and user-friendly error handling.
 */
public final class ErrorHandlingConfig {

    private ErrorHandlingConfig() {
        // Prevent instantiation.
    }

    public static final boolean HANDLE_ERRORS_SAFELY = true;

    public static final boolean SHOW_USER_FRIENDLY_MESSAGES = true;

    public static final boolean HIDE_INTERNAL_ERROR_DETAILS = true;

    public static final boolean HIDE_SECURITY_DETAILS = true;

    public static final boolean HIDE_AUTHENTICATION_DETAILS = true;

    public static final boolean HIDE_PRIVATE_DATA_FROM_ERRORS = true;

    public static final boolean AVOID_CRASH_ON_EXPECTED_ERRORS = true;

    public static final boolean VALIDATE_BEFORE_PROCESSING = true;

    public static final boolean LOG_SAFE_DIAGNOSTIC_INFORMATION = true;

    public static final boolean NEVER_LOG_PASSWORDS = true;

    public static final boolean NEVER_LOG_AUTH_TOKENS = true;

    public static final boolean NEVER_LOG_PRIVATE_MESSAGES = true;
}
