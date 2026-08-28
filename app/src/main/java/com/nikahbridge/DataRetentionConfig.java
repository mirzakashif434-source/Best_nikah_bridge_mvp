package com.nikahbridge;

/**
 * Best Nikah Bridge - Data Retention Configuration
 *
 * Defines safe and privacy-conscious retention rules
 * for application data.
 */
public final class DataRetentionConfig {

    private DataRetentionConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_RETENTION_POLICY = true;

    public static final boolean MINIMIZE_STORED_DATA = true;

    public static final boolean DELETE_EXPIRED_DATA = true;

    public static final boolean DELETE_DELETED_ACCOUNT_DATA = true;

    public static final boolean RETAIN_REQUIRED_SAFETY_RECORDS = true;

    public static final boolean RETAIN_REQUIRED_AUDIT_RECORDS = true;

    public static final boolean PROTECT_RETENTION_RECORDS = true;

    public static final boolean LOG_RETENTION_EVENTS = true;

    public static final int DEFAULT_RETENTION_DAYS = 365;

    public static final int AUDIT_RETENTION_DAYS = 365;

    public static final int EXPIRED_DATA_GRACE_DAYS = 30;
}
