package com.nikahbridge;

/**
 * Best Nikah Bridge - Account Security Configuration
 *
 * Central configuration for account protection,
 * authentication safety, and session rules.
 */
public final class AccountSecurityConfig {

    private AccountSecurityConfig() {
        // Prevent instantiation.
    }

    public static final boolean REQUIRE_AUTHENTICATION = true;

    public static final boolean PROTECT_ACCOUNT_DATA = true;

    public static final boolean PROTECT_LOGIN_STATE = true;

    public static final boolean PROTECT_SESSION_STATE = true;

    public static final boolean REQUIRE_AUTHORIZATION_FOR_PRIVATE_DATA = true;

    public static final boolean PREVENT_UNAUTHORIZED_PROFILE_ACCESS = true;

    public static final boolean PREVENT_UNAUTHORIZED_CHAT_ACCESS = true;

    public static final boolean PREVENT_UNAUTHORIZED_ADMIN_ACCESS = true;

    public static final boolean DO_NOT_STORE_PASSWORDS = true;

    public static final boolean DO_NOT_EXPOSE_AUTHENTICATION_DETAILS = true;
}
