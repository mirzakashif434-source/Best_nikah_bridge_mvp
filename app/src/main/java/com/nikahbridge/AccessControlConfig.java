package com.nikahbridge;

/**
 * Best Nikah Bridge - Access Control Configuration
 *
 * Central configuration for authorization and access-control rules.
 */
public final class AccessControlConfig {

    private AccessControlConfig() {
        // Prevent instantiation.
    }

    public static final boolean REQUIRE_AUTHORIZATION = true;

    public static final boolean PROTECT_USER_PROFILES = true;

    public static final boolean PROTECT_PRIVATE_PROFILE_DATA = true;

    public static final boolean PROTECT_CONNECTION_DATA = true;

    public static final boolean PROTECT_PRIVATE_CHAT_DATA = true;

    public static final boolean PROTECT_VERIFICATION_DATA = true;

    public static final boolean PROTECT_REPORT_DATA = true;

    public static final boolean ADMIN_ACCESS_REQUIRES_AUTHORIZATION = true;

    public static final boolean USERS_CANNOT_ACCESS_ADMIN_DATA = true;

    public static final boolean USERS_CANNOT_MODIFY_OTHER_USERS_DATA = true;

    public static final boolean DENY_UNAUTHORIZED_ACCESS = true;
}
