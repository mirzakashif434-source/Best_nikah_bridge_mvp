package com.nikahbridge;

/**
 * Best Nikah Bridge - Database Configuration
 *
 * Central configuration for safe and consistent
 * application data access.
 */
public final class DatabaseConfig {

    private DatabaseConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_DATABASE = true;

    public static final boolean VALIDATE_DATABASE_OPERATIONS = true;

    public static final boolean PREVENT_INVALID_RECORDS = true;

    public static final boolean PREVENT_DUPLICATE_RECORDS = true;

    public static final boolean USE_TRANSACTIONAL_OPERATIONS = true;

    public static final boolean PROTECT_SENSITIVE_DATA = true;

    public static final boolean LOG_DATABASE_SECURITY_EVENTS = true;

    public static final boolean FAIL_SAFE_ON_DATABASE_ERROR = true;
}
