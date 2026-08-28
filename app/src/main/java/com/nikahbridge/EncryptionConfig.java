package com.nikahbridge;

/**
 * Best Nikah Bridge - Encryption Configuration
 *
 * Central configuration for protecting sensitive application data.
 */
public final class EncryptionConfig {

    private EncryptionConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_ENCRYPTION = true;

    public static final boolean PROTECT_SENSITIVE_DATA = true;

    public static final boolean PROTECT_SESSION_DATA = true;

    public static final boolean PROTECT_AUTHENTICATION_DATA = true;

    public static final boolean PROTECT_PRIVATE_PROFILE_DATA = true;

    public static final boolean PROTECT_CHAT_DATA = true;

    public static final boolean PROTECT_SECURITY_EVENTS = true;

    public static final boolean FAIL_SAFE_ON_ENCRYPTION_ERROR = true;
}
