package com.nikahbridge;

/**
 * Best Nikah Bridge - Data Integrity Configuration
 *
 * Central configuration for protecting consistency,
 * validity, and reliability of application data.
 */
public final class DataIntegrityConfig {

    private DataIntegrityConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_DATA_INTEGRITY = true;

    public static final boolean VALIDATE_USER_DATA = true;

    public static final boolean VALIDATE_PROFILE_DATA = true;

    public static final boolean VALIDATE_PREFERENCE_DATA = true;

    public static final boolean VALIDATE_CONNECTION_DATA = true;

    public static final boolean VALIDATE_INTEREST_DATA = true;

    public static final boolean PREVENT_DUPLICATE_RECORDS = true;

    public static final boolean PREVENT_INVALID_STATE_TRANSITIONS = true;

    public static final boolean FAIL_SAFE_ON_INTEGRITY_ERROR = true;

    public static final boolean LOG_INTEGRITY_EVENTS = true;
}
