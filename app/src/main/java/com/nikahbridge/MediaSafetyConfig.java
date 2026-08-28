package com.nikahbridge;

/**
 * Best Nikah Bridge - Media Safety Configuration
 *
 * Controls safe handling of profile and chat media.
 */
public final class MediaSafetyConfig {

    private MediaSafetyConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_MEDIA_SAFETY = true;

    public static final boolean ALLOW_PROFILE_PHOTOS = true;

    public static final boolean ALLOW_CHAT_MEDIA = true;

    public static final boolean REQUIRE_SAFE_MEDIA = true;

    public static final boolean BLOCK_EXPLICIT_CONTENT = true;

    public static final boolean BLOCK_ILLEGAL_CONTENT = true;

    public static final boolean ALLOW_USER_REPORTS = true;

    public static final boolean ALLOW_ADMIN_REVIEW = true;

    public static final boolean PROTECT_PRIVATE_MEDIA = true;

    public static final boolean LOG_MEDIA_SAFETY_EVENTS = true;
}
