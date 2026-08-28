package com.nikahbridge;

/**
 * Best Nikah Bridge - Session Security Configuration
 *
 * Central configuration for secure user-session handling.
 */
public final class SessionSecurityConfig {

    private SessionSecurityConfig() {
        // Prevent instantiation.
    }

    public static final boolean REQUIRE_AUTHENTICATED_SESSION = true;

    public static final boolean PROTECT_SESSION_DATA = true;

    public static final boolean PREVENT_UNAUTHORIZED_SESSION_ACCESS = true;

    public static final boolean PROTECT_PRIVATE_USER_ACTIONS = true;

    public static final boolean PROTECT_PRIVATE_CHAT_SESSION = true;

    public static final boolean PROTECT_VERIFICATION_SESSION = true;

    public static final boolean PROTECT_ADMIN_SESSION = true;

    public static final boolean DO_NOT_STORE_PASSWORD_IN_SESSION = true;

    public static final boolean DO_NOT_EXPOSE_SESSION_CREDENTIALS = true;

    public static final boolean REQUIRE_SESSION_VALIDATION = true;
}
