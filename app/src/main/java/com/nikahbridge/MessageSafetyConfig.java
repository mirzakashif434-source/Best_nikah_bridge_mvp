package com.nikahbridge;

/**
 * Best Nikah Bridge - Message Safety Configuration
 *
 * Controls safe, respectful and privacy-focused messaging.
 */
public final class MessageSafetyConfig {

    private MessageSafetyConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_MESSAGE_SAFETY = true;

    public static final boolean MUTUAL_CONNECTION_REQUIRED = true;

    public static final boolean BLOCK_REPORTED_USERS = true;

    public static final boolean ALLOW_MESSAGE_REPORTING = true;

    public static final boolean ALLOW_MESSAGE_BLOCKING = true;

    public static final boolean FILTER_ABUSIVE_CONTENT = true;

    public static final boolean FILTER_SPAM_CONTENT = true;

    public static final boolean PROTECT_CONTACT_INFORMATION = true;

    public static final boolean LOG_SAFETY_EVENTS = true;

    public static final boolean ADMIN_REVIEW_ENABLED = true;
}
