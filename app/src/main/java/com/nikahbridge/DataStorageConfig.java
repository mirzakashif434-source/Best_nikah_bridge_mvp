package com.nikahbridge;

/**
 * Best Nikah Bridge - Data Storage Configuration
 *
 * Central configuration for safe, controlled and
 * privacy-conscious local application data storage.
 */
public final class DataStorageConfig {

    private DataStorageConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_DATA_STORAGE = true;

    public static final boolean USE_SECURE_STORAGE = true;

    public static final boolean PROTECT_SENSITIVE_DATA = true;

    public static final boolean MINIMIZE_LOCAL_DATA = true;

    public static final boolean CLEAR_SENSITIVE_DATA_ON_LOGOUT = true;

    public static final boolean CLEAR_SENSITIVE_DATA_ON_ACCOUNT_DELETE = true;

    public static final boolean PREVENT_UNSAFE_PLAINTEXT_STORAGE = true;

    public static final boolean VALIDATE_STORED_DATA = true;

    public static final boolean LOG_STORAGE_SECURITY_EVENTS = true;

    public static final boolean FAIL_SAFE_ON_STORAGE_ERROR = true;
}
