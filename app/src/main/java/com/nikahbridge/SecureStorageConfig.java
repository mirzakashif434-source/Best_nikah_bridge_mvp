package com.nikahbridge;

/**
 * Best Nikah Bridge - Secure Storage Configuration
 *
 * Central configuration for protecting locally stored sensitive data.
 */
public final class SecureStorageConfig {

    private SecureStorageConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_SECURE_STORAGE = true;

    public static final boolean PROTECT_AUTH_TOKENS = true;

    public static final boolean PROTECT_SESSION_DATA = true;

    public static final boolean PROTECT_USER_IDENTIFIERS = true;

    public static final boolean PROTECT_PRIVATE_PROFILE_DATA = true;

    public static final boolean AVOID_PLAINTEXT_SECRETS = true;

    public static final boolean CLEAR_SENSITIVE_DATA_ON_LOGOUT = true;

    public static final boolean FAIL_SAFE_ON_STORAGE_ERROR = true;
}
