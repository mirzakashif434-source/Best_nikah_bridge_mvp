package com.nikahbridge;

/**
 * Best Nikah Bridge - Notification Safety Configuration
 *
 * Controls privacy-friendly and safe app notifications.
 */
public final class NotificationSafetyConfig {

    private NotificationSafetyConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_NOTIFICATIONS = true;

    public static final boolean ENABLE_MATCH_NOTIFICATIONS = true;

    public static final boolean ENABLE_INTEREST_NOTIFICATIONS = true;

    public static final boolean ENABLE_MUTUAL_NOTIFICATIONS = true;

    public static final boolean ENABLE_SAFETY_ALERTS = true;

    public static final boolean HIDE_SENSITIVE_CONTENT = true;

    public static final boolean HIDE_PRIVATE_MESSAGE_PREVIEW = true;

    public static final boolean ALLOW_USER_NOTIFICATION_CONTROL = true;

    public static final boolean RESPECT_DEVICE_NOTIFICATION_SETTINGS = true;
}
