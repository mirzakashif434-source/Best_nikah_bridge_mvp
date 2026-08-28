package com.nikahbridge;

/**
 * Best Nikah Bridge - Safe Chat Configuration
 *
 * Marriage-focused communication rules.
 *
 * Chat is available only after mutual acceptance.
 * Personal contact information remains private by default.
 *
 * This class contains configuration only.
 * Authentication, database persistence, moderation and
 * server-side enforcement must be implemented separately.
 */
public final class SafeChatConfig {

    private SafeChatConfig() {
        // Prevent instantiation.
    }

    // Chat access
    public static final boolean CHAT_ENABLED = true;
    public static final boolean REQUIRE_MUTUAL_CONNECTION = true;
    public static final boolean BLOCK_ONE_SIDED_CHAT = true;

    // Privacy
    public static final boolean HIDE_PHONE_NUMBER = true;
    public static final boolean HIDE_EMAIL_ADDRESS = true;
    public static final boolean HIDE_SOCIAL_MEDIA_CONTACTS = true;
    public static final boolean PROTECT_PERSONAL_CONTACT_DETAILS = true;

    // Safety
    public static final boolean ALLOW_BLOCK = true;
    public static final boolean ALLOW_REPORT = true;
    public static final boolean ALLOW_MUTE = true;

    // Marriage-focused communication
    public static final boolean MARRIAGE_PURPOSE_ONLY = true;
    public static final boolean NO_PUBLIC_CHAT = true;
    public static final boolean NO_DATING_MODE = true;

    // Message limits
    public static final int FREE_DAILY_MESSAGE_LIMIT = 2;
    public static final int MAX_MESSAGE_LENGTH = 1000;

    // Abuse prevention
    public static final boolean PREVENT_SPAM = true;
    public static final boolean PREVENT_REPEATED_INTEREST = true;
    public static final boolean SUPPORT_SAFETY_REVIEW = true;

    /**
     * Chat is allowed only for a mutual connection.
     */
    public static boolean canChat(String connectionStatus) {
        return REQUIRE_MUTUAL_CONNECTION
                && ConnectionConfig.STATUS_MUTUAL.equals(connectionStatus);
    }

    /**
     * Determines whether personal contact information should remain hidden.
     */
    public static boolean shouldHidePersonalContactDetails() {
        return PROTECT_PERSONAL_CONTACT_DETAILS;
    }

    /**
     * Checks whether a message length is acceptable.
     */
    public static boolean isMessageLengthValid(String message) {
        if (message == null) {
            return false;
        }

        int length = message.trim().length();

        return length > 0 && length <= MAX_MESSAGE_LENGTH;
    }

    /**
     * Returns whether safety actions are available.
     */
    public static boolean safetyActionsEnabled() {
        return ALLOW_BLOCK && ALLOW_REPORT;
    }
}
