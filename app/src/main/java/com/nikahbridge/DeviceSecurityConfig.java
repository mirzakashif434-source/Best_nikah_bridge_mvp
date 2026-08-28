package com.nikahbridge;

/**
 * Best Nikah Bridge - Device Security Configuration
 *
 * Central configuration for basic device/security rules.
 * No secrets or credentials are stored here.
 */
public final class DeviceSecurityConfig {

    private DeviceSecurityConfig() {
        // Prevent instantiation.
    }

    public static final boolean REQUIRE_SECURE_CONNECTION = true;

    public static final boolean BLOCK_CLEAR_TEXT_TRAFFIC = true;

    public static final boolean PROTECT_USER_SESSIONS = true;

    public static final boolean PROTECT_CONTACT_DETAILS = true;

    public static final boolean PROTECT_PRIVATE_CHAT = true;

    public static final boolean ENABLE_REPORT_BLOCK_SAFETY = true;

    public static final boolean ADMIN_ACTIONS_REQUIRE_AUTHORIZATION = true;

    public static final boolean NEVER_STORE_PASSWORDS_LOCALLY = true;

    public static final boolean NEVER_STORE_AUTH_TOKENS_IN_PLAINTEXT = true;
}
