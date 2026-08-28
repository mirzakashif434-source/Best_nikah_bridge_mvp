package com.nikahbridge;

/**
 * Best Nikah Bridge - User Session Configuration
 *
 * Central configuration for safe user-session behavior.
 *
 * Authentication, token storage, persistent login and
 * server-side session validation must be implemented separately.
 */
public final class UserSessionConfig {

    private UserSessionConfig() {
        // Prevent instantiation.
    }

    /**
     * A session is considered valid only after authentication.
     */
    public static final boolean REQUIRE_AUTHENTICATION = true;

    /**
     * Keep session state private to the authenticated user.
     */
    public static final boolean PRIVATE_SESSION_STATE = true;

    /**
     * Do not expose authentication credentials through UI/logs.
     */
    public static final boolean PROTECT_AUTH_DATA = true;

    /**
     * Session should be cleared when the user explicitly signs out.
     */
    public static final boolean CLEAR_SESSION_ON_LOGOUT = true;

    /**
     * Do not keep sensitive authentication data in this
     * configuration class.
     */
    public static final boolean NEVER_STORE_CREDENTIALS_HERE = true;

    /**
     * Server-side authentication/session validation is required
     * for protected operations.
     */
    public static final boolean REQUIRE_SERVER_VALIDATION = true;

    /**
     * Protected matrimonial features should require an
     * authenticated session.
     */
    public static final boolean PROTECT_USER_FEATURES = true;

    /**
     * Example protected feature areas.
     */
    public static final String[] PROTECTED_FEATURES = {
            "profile",
            "partner_preferences",
            "matches",
            "express_interest",
            "mutual_connections",
            "safe_chat",
            "verification",
            "reports"
    };

    /**
     * Session-related values that must never be written
     * to ordinary application logs.
     */
    public static final String[] SENSITIVE_SESSION_FIELDS = {
            "password",
            "authToken",
            "refreshToken",
            "sessionToken",
            "privateContact"
    };
}
