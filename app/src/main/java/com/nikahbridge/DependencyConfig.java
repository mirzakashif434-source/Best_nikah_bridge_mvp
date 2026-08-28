package com.nikahbridge;

/**
 * Best Nikah Bridge - Dependency Configuration
 *
 * Central configuration for safe and controlled
 * application dependencies and integrations.
 */
public final class DependencyConfig {

    private DependencyConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_DEPENDENCY_VALIDATION = true;

    public static final boolean REQUIRE_TRUSTED_DEPENDENCIES = true;

    public static final boolean PREVENT_UNSAFE_DEPENDENCIES = true;

    public static final boolean VALIDATE_DEPENDENCY_VERSIONS = true;

    public static final boolean KEEP_DEPENDENCIES_MINIMAL = true;

    public static final boolean DISABLE_UNUSED_DEPENDENCIES = true;

    public static final boolean REVIEW_SECURITY_SENSITIVE_DEPENDENCIES = true;

    public static final boolean LOG_DEPENDENCY_SECURITY_EVENTS = true;
}
