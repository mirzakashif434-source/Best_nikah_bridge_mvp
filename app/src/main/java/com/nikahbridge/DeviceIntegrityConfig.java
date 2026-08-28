package com.nikahbridge;

/**
 * Best Nikah Bridge - Device Integrity Configuration
 *
 * Central configuration for basic device-integrity
 * and trusted-device checks.
 */
public final class DeviceIntegrityConfig {

    private DeviceIntegrityConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_DEVICE_INTEGRITY = true;

    public static final boolean CHECK_DEVICE_STATE = true;

    public static final boolean DETECT_UNTRUSTED_DEVICE_STATE = true;

    public static final boolean PROTECT_SENSITIVE_OPERATIONS = true;

    public static final boolean REQUIRE_REAUTH_FOR_SENSITIVE_ACTIONS = true;

    public static final boolean FAIL_SAFE_ON_INTEGRITY_FAILURE = true;

    public static final boolean LOG_INTEGRITY_EVENTS = true;
}
