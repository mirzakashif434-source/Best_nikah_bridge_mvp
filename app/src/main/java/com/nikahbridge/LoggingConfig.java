package com.nikahbridge;

/**
 * Best Nikah Bridge - Logging Configuration
 *
 * Centralized logging controls for production safety.
 */
public final class LoggingConfig {

    private LoggingConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_LOGGING = true;

    public static final boolean ENABLE_DEBUG_LOGS = false;

    public static final boolean ENABLE_ERROR_LOGS = true;

    public static final boolean ENABLE_SECURITY_LOGS = true;

    public static final boolean ENABLE_AUDIT_LOGS = true;

    public static final boolean INCLUDE_SENSITIVE_DATA = false;

    public static final boolean LOG_USER_PASSWORDS = false;

    public static final boolean LOG_AUTH_TOKENS = false;

    public static final boolean LOG_PRIVATE_MESSAGES = false;

    public static final String TAG = "BestNikahBridge";
}
