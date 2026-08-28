package com.nikahbridge;

/**
 * Best Nikah Bridge - Secure Network Configuration
 *
 * Central configuration for secure network communication.
 */
public final class SecureNetworkConfig {

    private SecureNetworkConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_SECURE_NETWORK = true;

    public static final boolean REQUIRE_HTTPS = true;

    public static final boolean BLOCK_CLEARTEXT_TRAFFIC = true;

    public static final boolean VALIDATE_SECURE_ENDPOINTS = true;

    public static final boolean PROTECT_AUTHENTICATION_TRAFFIC = true;

    public static final boolean PROTECT_PROFILE_TRAFFIC = true;

    public static final boolean PROTECT_CHAT_TRAFFIC = true;

    public static final boolean PROTECT_SECURITY_TRAFFIC = true;

    public static final boolean FAIL_SAFE_ON_NETWORK_SECURITY_ERROR = true;
}
