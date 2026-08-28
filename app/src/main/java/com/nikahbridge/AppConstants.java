package com.nikahbridge;

/**
 * Best Nikah Bridge - Application Constants
 *
 * Central place for stable, non-secret application constants.
 *
 * Do not put passwords, API keys, tokens or other secrets here.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent instantiation.
    }

    // Application identity
    public static final String APP_NAME = "Best Nikah Bridge";
    public static final String PACKAGE_NAME = "com.nikahbridge";

    // Product purpose
    public static final String APP_TYPE = "Muslim Matrimonial";
    public static final String APP_PURPOSE = "Marriage-focused connections";

    // Connection flow
    public static final String FLOW_PROFILE = "Profile";
    public static final String FLOW_INTEREST = "Express Interest";
    public static final String FLOW_MUTUAL = "Mutual Acceptance";
    public static final String FLOW_CHAT = "Safe Chat";

    // Supported languages
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_URDU = "ur";

    // Safety actions
    public static final String ACTION_REPORT = "report";
    public static final String ACTION_BLOCK = "block";

    // Profile states
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_VERIFIED = "verified";
    public static final String STATUS_SUSPENDED = "suspended";

    // General limits
    public static final int MIN_PROFILE_AGE = 18;
    public static final int MAX_PROFILE_AGE = 100;

    // Privacy
    public static final boolean CONTACT_DETAILS_PRIVATE = true;
    public static final boolean MUTUAL_ACCEPTANCE_REQUIRED = true;

    // Security
    public static final boolean NEVER_STORE_SECRETS = true;
}
