package com.google.firebase.ai;

/**
 * Compatibility bridge for the production Java entry point.
 * Delegates to the official Firebase AI Logic backend type.
 */
public final class GenerativeBackend {
    private GenerativeBackend() {}

    public static com.google.firebase.ai.type.GenerativeBackend googleAI() {
        return com.google.firebase.ai.type.GenerativeBackend.googleAI();
    }
}
