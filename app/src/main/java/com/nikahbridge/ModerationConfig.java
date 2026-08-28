package com.nikahbridge;

/**
 * Best Nikah Bridge - Content Moderation Configuration
 *
 * Centralized moderation and abuse-prevention controls.
 */
public final class ModerationConfig {

    private ModerationConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_MODERATION = true;

    public static final boolean ENABLE_AUTOMATED_FILTERING = true;

    public static final boolean BLOCK_EXPLICIT_CONTENT = true;

    public static final boolean BLOCK_HATEFUL_CONTENT = true;

    public static final boolean BLOCK_HARASSMENT = true;

    public static final boolean BLOCK_SPAM = true;

    public static final boolean BLOCK_SCAM_CONTENT = true;

    public static final boolean ENABLE_USER_REPORTS = true;

    public static final boolean ENABLE_ADMIN_REVIEW = true;

    public static final boolean LOG_MODERATION_EVENTS = true;

    public static final boolean PROTECT_REPORTER_PRIVACY = true;
}
