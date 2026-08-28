package com.nikahbridge;

/**
 * Best Nikah Bridge - Profile Safety Configuration
 *
 * Controls privacy, authenticity and safe profile presentation.
 */
public final class ProfileSafetyConfig {

    private ProfileSafetyConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_PROFILE_SAFETY = true;

    public static final boolean REQUIRE_VALID_PROFILE_DATA = true;

    public static final boolean REQUIRE_MARRIAGE_INTENT = true;

    public static final boolean ENABLE_PROFILE_REPORTING = true;

    public static final boolean ENABLE_PROFILE_BLOCKING = true;

    public static final boolean PROTECT_PRIVATE_CONTACT_INFO = true;

    public static final boolean REQUIRE_SAFE_PROFILE_PHOTOS = true;

    public static final boolean ENABLE_PROFILE_VERIFICATION = true;

    public static final boolean ENABLE_ADMIN_PROFILE_REVIEW = true;

    public static final boolean LOG_PROFILE_SAFETY_EVENTS = true;
}
