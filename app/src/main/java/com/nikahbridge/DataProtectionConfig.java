package com.nikahbridge;

/**
 * Best Nikah Bridge - Data Protection Configuration
 *
 * Central configuration for privacy and protection of
 * sensitive user information.
 */
public final class DataProtectionConfig {

    private DataProtectionConfig() {
        // Prevent instantiation.
    }

    public static final boolean PROTECT_PERSONAL_DATA = true;

    public static final boolean PROTECT_PHONE_NUMBER = true;

    public static final boolean PROTECT_EMAIL_ADDRESS = true;

    public static final boolean PROTECT_PROFILE_DATA = true;

    public static final boolean PROTECT_PRIVATE_MESSAGES = true;

    public static final boolean PROTECT_VERIFICATION_DATA = true;

    public static final boolean MINIMIZE_DATA_COLLECTION = true;

    public static final boolean DO_NOT_EXPOSE_PRIVATE_DATA = true;

    public static final boolean REQUIRE_USER_CONSENT = true;

    public static final boolean SUPPORT_DATA_DELETION = true;
}
