package com.nikahbridge;

/**
 * Best Nikah Bridge - Privacy Configuration
 *
 * Central configuration for privacy-first matrimonial features.
 *
 * This class contains configuration/constants only.
 * Authentication, database persistence, encryption and
 * server-side enforcement must be implemented separately.
 */
public final class PrivacyConfig {

    private PrivacyConfig() {
        // Prevent instantiation.
    }

    /**
     * Personal contact information must remain private
     * until the application's permitted connection flow
     * allows sharing.
     */
    public static final boolean KEEP_CONTACT_PRIVATE = true;

    /**
     * Direct contact details should not be exposed
     * on public profile screens.
     */
    public static final boolean HIDE_CONTACT_DETAILS = true;

    /**
     * Chat/contact access should follow mutual acceptance.
     */
    public static final boolean REQUIRE_MUTUAL_ACCEPTANCE = true;

    /**
     * Users should be able to report or block another user.
     */
    public static final boolean ENABLE_REPORT_AND_BLOCK = true;

    /**
     * Profile visibility should respect the user's privacy settings.
     */
    public static final boolean RESPECT_PROFILE_VISIBILITY = true;

    /**
     * Do not expose sensitive account information in logs.
     */
    public static final boolean REDACT_SENSITIVE_LOG_DATA = true;

    /**
     * Do not expose passwords, authentication tokens,
     * private contact details or other secrets through
     * client-side configuration.
     */
    public static final boolean NEVER_STORE_SECRETS_IN_CONFIG = true;

    /**
     * Privacy-sensitive actions should be auditable by
     * the appropriate trusted/admin systems.
     */
    public static final boolean AUDIT_PRIVACY_ACTIONS = true;

    /**
     * Default privacy notice shown to users.
     */
    public static final String PRIVACY_NOTICE =
            "Your personal information should remain private "
            + "and should only be shared through permitted "
            + "connection and communication flows.";

    /**
     * Prevent accidental exposure of sensitive fields.
     */
    public static final String[] SENSITIVE_FIELDS = {
            "password",
            "authToken",
            "phoneNumber",
            "email",
            "privateContact",
            "verificationDocument"
    };
}
