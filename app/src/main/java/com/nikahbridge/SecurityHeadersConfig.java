package com.nikahbridge;

/**
 * Best Nikah Bridge - Security Headers Configuration
 *
 * Central configuration for secure communication and response policies.
 */
public final class SecurityHeadersConfig {

    private SecurityHeadersConfig() {
        // Prevent instantiation.
    }

    public static final boolean ENABLE_SECURITY_HEADERS = true;

    public static final boolean PREVENT_CONTENT_TYPE_SNIFFING = true;

    public static final boolean PREVENT_CLICKJACKING = true;

    public static final boolean ENFORCE_SECURE_TRANSPORT = true;

    public static final boolean PROTECT_SENSITIVE_RESPONSES = true;

    public static final boolean DISABLE_UNSAFE_CACHING = true;

    public static final boolean REQUIRE_SECURE_CONNECTIONS = true;
}
