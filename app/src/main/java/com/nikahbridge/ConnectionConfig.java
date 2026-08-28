package com.nikahbridge;

/**
 * Best Nikah Bridge - Connection Configuration
 *
 * Defines safe, marriage-focused connection rules:
 * Profile → Express Interest → Mutual Acceptance → Safe Chat.
 *
 * This class contains configuration only.
 * Actual persistence, authentication and server-side enforcement
 * must be implemented separately.
 */
public final class ConnectionConfig {

    private ConnectionConfig() {
        // Prevent instantiation.
    }

    // Connection states
    public static final String STATUS_NONE = "NONE";
    public static final String STATUS_INTEREST_SENT = "INTEREST_SENT";
    public static final String STATUS_INTEREST_RECEIVED = "INTEREST_RECEIVED";
    public static final String STATUS_MUTUAL = "MUTUAL";
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String STATUS_REPORTED = "REPORTED";

    // Interest rules
    public static final boolean ALLOW_EXPRESS_INTEREST = true;
    public static final boolean PREVENT_DUPLICATE_INTEREST = true;
    public static final boolean REQUIRE_MUTUAL_ACCEPTANCE = true;

    // Safe communication
    public static final boolean CHAT_ONLY_AFTER_MUTUAL = true;
    public static final boolean HIDE_CONTACT_DETAILS_BY_DEFAULT = true;
    public static final boolean ALLOW_BLOCKING = true;
    public static final boolean ALLOW_REPORTING = true;

    // Marriage-focused safety
    public static final boolean NO_PUBLIC_CONTACT_DETAILS = true;
    public static final boolean NO_DATING_MODE = true;
    public static final boolean MARRIAGE_INTENT_REQUIRED = true;

    // Connection limits
    public static final int MAX_PENDING_INTERESTS = 50;
    public static final int MAX_INTERESTS_PER_PROFILE = 100;

    // Free-user messaging support
    public static final boolean SUPPORT_LIMITED_FREE_MESSAGING = true;
    public static final int FREE_DAILY_MESSAGE_LIMIT = 2;

    // Compatibility threshold for recommended connections
    public static final int RECOMMENDED_MATCH_MIN_SCORE = 70;

    /**
     * Returns true when chat is allowed.
     * Chat must never be opened for a one-sided interest.
     */
    public static boolean canStartChat(String connectionStatus) {
        return STATUS_MUTUAL.equals(connectionStatus);
    }

    /**
     * Returns true when a new interest can be sent.
     */
    public static boolean canSendInterest(
            String connectionStatus,
            boolean marriageIntentConfirmed) {

        if (!ALLOW_EXPRESS_INTEREST) {
            return false;
        }

        if (!marriageIntentConfirmed) {
            return false;
        }

        if (STATUS_BLOCKED.equals(connectionStatus)
                || STATUS_REPORTED.equals(connectionStatus)
                || STATUS_INTEREST_SENT.equals(connectionStatus)
                || STATUS_MUTUAL.equals(connectionStatus)) {
            return false;
        }

        return true;
    }

    /**
     * Returns true when the connection becomes mutual.
     */
    public static boolean isMutual(String connectionStatus) {
        return STATUS_MUTUAL.equals(connectionStatus);
    }

    /**
     * Returns true when contact details should remain private.
     */
    public static boolean shouldHideContactDetails() {
        return HIDE_CONTACT_DETAILS_BY_DEFAULT;
    }
}
