package com.nikahbridge;

/**
 * Best Nikah Bridge - Localization Configuration
 *
 * Centralized language and localization settings.
 */
public final class LocalizationConfig {

    private LocalizationConfig() {
        // Prevent instantiation.
    }

    public static final String DEFAULT_LANGUAGE = "en";

    public static final String URDU_LANGUAGE = "ur";

    public static final String ARABIC_LANGUAGE = "ar";

    public static final boolean ENABLE_URDU = true;

    public static final boolean ENABLE_ARABIC = true;

    public static final boolean USE_DEVICE_LANGUAGE = true;

    public static final boolean FALLBACK_TO_DEFAULT_LANGUAGE = true;
}
