package com.nikahbridge;

/**
 * Best Nikah Bridge - Content Safety Configuration
 *
 * Central configuration for safe and respectful user-generated content.
 */
public final class ContentSafetyConfig {

    private ContentSafetyConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_CONTENT_SAFETY = true;

    public static final boolean BLOCK_PROHIBITED_CONTENT = true;

    public static final boolean BLOCK_HARASSMENT = true;

    public static final boolean BLOCK_SPAM_CONTENT = true;

    public static final boolean PROTECT_PROFILE_CONTENT = true;

    public static final boolean PROTECT_CHAT_CONTENT = true;

    public static final boolean PROTECT_INTEREST_CONTENT = true;

    public static final boolean SUPPORT_USER_REPORTING = true;

    public static final boolean LOG_SAFETY_EVENTS = true;

    public static final boolean FAIL_SAFE_ON_CONTENT_SAFETY_ERROR = true;
}
