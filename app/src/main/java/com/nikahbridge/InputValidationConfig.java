package com.nikahbridge;

/**
 * Best Nikah Bridge - Input Validation Configuration
 *
 * Central configuration for safe validation of user-provided data.
 */
public final class InputValidationConfig {

    private InputValidationConfig() {
        // Prevent instantiation.
    }

    public static final boolean VALIDATE_USER_INPUT = true;

    public static final boolean TRIM_TEXT_INPUT = true;

    public static final boolean REJECT_EMPTY_REQUIRED_FIELDS = true;

    public static final boolean LIMIT_TEXT_LENGTH = true;

    public static final boolean VALIDATE_AGE_RANGE = true;

    public static final boolean VALIDATE_COUNTRY_INPUT = true;

    public static final boolean VALIDATE_PROFILE_FIELDS = true;

    public static final boolean VALIDATE_INTEREST_ACTIONS = true;

    public static final boolean VALIDATE_REPORT_DATA = true;

    public static final boolean VALIDATE_CHAT_INPUT = true;

    public static final boolean REJECT_INVALID_INPUT = true;
}
