package com.nikahbridge;

/**
 * Best Nikah Bridge - Notification Configuration
 *
 * Central configuration for safe, useful and
 * marriage-focused notifications.
 *
 * Actual notification delivery, permissions, channels
 * and server-side notification services must be
 * implemented separately.
 */
public final class NotificationConfig {

    private NotificationConfig() {
        // Prevent instantiation.
    }

    /**
     * Enable notifications for important account activity.
     */
    public static final boolean ENABLE_NOTIFICATIONS = true;

    /**
     * Notify users about genuine mutual connection activity.
     */
    public static final boolean ENABLE_MUTUAL_CONNECTION_ALERTS = true;

    /**
     * Notify users about new genuine interests.
     */
    public static final boolean ENABLE_INTEREST_ALERTS = true;

    /**
     * Notify users about important verification updates.
     */
    public static final boolean ENABLE_VERIFICATION_ALERTS = true;

    /**
     * Notify users about important safety/account events.
     */
    public static final boolean ENABLE_SAFETY_ALERTS = true;

    /**
     * Safe Chat notifications are allowed only for
     * permitted/mutual connections.
     */
    public static final boolean ENABLE_SAFE_CHAT_ALERTS = true;

    /**
     * Avoid unnecessary promotional notifications.
     */
    public static final boolean LIMIT_PROMOTIONAL_NOTIFICATIONS = true;

    /**
     * Do not include sensitive personal information
     * inside notification previews.
     */
    public static final boolean HIDE_SENSITIVE_PREVIEW_CONTENT = true;

    /**
     * Users should be able to control notification preferences.
     */
    public static final boolean ALLOW_USER_NOTIFICATION_SETTINGS = true;

    /**
     * Notification categories used by the application.
     */
    public static final String[] NOTIFICATION_TYPES = {
            "interest",
            "mutual_connection",
            "safe_chat",
            "verification",
            "safety",
            "account"
    };

    /**
     * Sensitive information that must never be placed
     * directly into notification text.
     */
    public static final String[] PROTECTED_NOTIFICATION_FIELDS = {
            "password",
            "authToken",
            "phoneNumber",
            "email",
            "privateContact",
            "verificationDocument"
    };
}
