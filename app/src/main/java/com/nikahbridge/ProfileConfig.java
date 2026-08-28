package com.nikahbridge;

/**
 * Best Nikah Bridge - Profile configuration.
 *
 * Central configuration for the user profile and profile-quality system.
 * No payment credentials or private user data belong in this file.
 */
public final class ProfileConfig {

    private ProfileConfig() {
        // Prevent instantiation.
    }

    // Profile completion
    public static final int MIN_PROFILE_COMPLETION_PERCENT = 70;
    public static final int COMPLETE_PROFILE_PERCENT = 100;

    // Basic profile limits
    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 80;
    public static final int MAX_ABOUT_LENGTH = 500;

    // Profile sections
    public static final boolean REQUIRE_BASIC_DETAILS = true;
    public static final boolean REQUIRE_MARRIAGE_INTENTION = true;
    public static final boolean REQUIRE_PARTNER_PREFERENCES = true;
    public static final boolean REQUIRE_LOCATION = true;
    public static final boolean REQUIRE_ABOUT_SECTION = true;

    // Trust and verification
    public static final boolean SHOW_VERIFIED_BADGE = true;
    public static final boolean SHOW_ID_VERIFIED_BADGE = true;
    public static final boolean ALLOW_REPORT_PROFILE = true;
    public static final boolean ALLOW_BLOCK_PROFILE = true;

    // Privacy
    public static final boolean HIDE_PHONE_NUMBER_BY_DEFAULT = true;
    public static final boolean HIDE_EMAIL_BY_DEFAULT = true;
    public static final boolean PROTECT_EXACT_LOCATION = true;

    // Marriage-focused profile
    public static final boolean SHOW_MARRIAGE_INTENTION = true;
    public static final boolean SHOW_PARTNER_PREFERENCES = true;
    public static final boolean SHOW_COMPATIBILITY_SCORE = true;
    public static final boolean SHOW_MATCH_REASONS = true;

    // Profile quality
    public static final int HIGH_QUALITY_PROFILE_PERCENT = 90;
    public static final boolean ENABLE_PROFILE_QUALITY_CHECK = true;
    public static final boolean ENABLE_DUPLICATE_PROFILE_PROTECTION = true;

    // Supported profile languages
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_URDU = "ur";
}
