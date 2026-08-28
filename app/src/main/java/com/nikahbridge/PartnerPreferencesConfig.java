package com.nikahbridge;

/**
 * Best Nikah Bridge - Partner preferences configuration.
 *
 * Defines the preference and matching rules for the future
 * marriage-focused compatibility engine.
 */
public final class PartnerPreferencesConfig {

    private PartnerPreferencesConfig() {
        // Prevent instantiation.
    }

    // Age preferences
    public static final int MIN_PREFERRED_AGE = 18;
    public static final int MAX_PREFERRED_AGE = 80;

    // Marriage intention
    public static final boolean REQUIRE_MARRIAGE_INTENTION = true;
    public static final boolean SHOW_MARRIAGE_TIMELINE = true;

    // Location preferences
    public static final boolean ENABLE_COUNTRY_PREFERENCE = true;
    public static final boolean ENABLE_CITY_PREFERENCE = true;
    public static final boolean PROTECT_EXACT_LOCATION = true;

    // Education and profession
    public static final boolean ENABLE_EDUCATION_PREFERENCE = true;
    public static final boolean ENABLE_PROFESSION_PREFERENCE = true;

    // Family and lifestyle
    public static final boolean ENABLE_FAMILY_PREFERENCE = true;
    public static final boolean ENABLE_LIFESTYLE_PREFERENCE = true;

    // Islamic / Nikah-focused preferences
    public static final boolean ENABLE_RELIGIOUS_PREFERENCE = true;
    public static final boolean ENABLE_NIKAH_VALUES = true;
    public static final boolean ENABLE_WALI_PREFERENCE = true;

    // Compatibility engine
    public static final boolean ENABLE_SMART_MATCHING = true;
    public static final boolean ENABLE_COMPATIBILITY_SCORE = true;
    public static final boolean ENABLE_MATCH_REASONS = true;
    public static final boolean ENABLE_MUTUAL_PREFERENCE_CHECK = true;

    // Matching quality
    public static final int MIN_COMPATIBILITY_SCORE = 50;
    public static final int GOOD_COMPATIBILITY_SCORE = 70;
    public static final int HIGH_COMPATIBILITY_SCORE = 80;
    public static final int EXCELLENT_COMPATIBILITY_SCORE = 90;

    // Safety
    public static final boolean EXCLUDE_BLOCKED_PROFILES = true;
    public static final boolean EXCLUDE_REPORTED_PROFILES = true;
    public static final boolean EXCLUDE_UNVERIFIED_HIGH_RISK_PROFILES = true;

    // Privacy
    public static final boolean DO_NOT_EXPOSE_PRIVATE_PREFERENCES = true;
    public static final boolean KEEP_PHONE_PRIVATE = true;
    public static final boolean KEEP_EMAIL_PRIVATE = true;

    // Supported languages
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_URDU = "ur";
}
