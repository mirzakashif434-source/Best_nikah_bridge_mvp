package com.nikahbridge;

/**
 * Best Nikah Bridge - Trust & Safety Configuration.
 *
 * Central configuration for privacy, reporting, blocking,
 * safe communication and account-safety rules.
 *
 * This class contains configuration only.
 * Actual enforcement must be implemented by the app/backend.
 */
public final class TrustSafetyConfig {

    private TrustSafetyConfig() {
        // Prevent instantiation.
    }

    // ---------------------------------------------------------
    // Privacy
    // ---------------------------------------------------------

    public static final boolean ENABLE_PRIVACY_CONTROLS = true;
    public static final boolean HIDE_PERSONAL_CONTACT_INFO = true;
    public static final boolean HIDE_EXACT_LOCATION = true;
    public static final boolean SHOW_APPROXIMATE_LOCATION = true;
    public static final boolean ALLOW_PROFILE_VISIBILITY_CONTROL = true;

    // ---------------------------------------------------------
    // Report & Block
    // ---------------------------------------------------------

    public static final boolean ENABLE_REPORT = true;
    public static final boolean ENABLE_BLOCK = true;
    public static final boolean ENABLE_REPORT_REASON_SELECTION = true;
    public static final boolean PREVENT_BLOCKED_PROFILE_CONTACT = true;

    // Serious safety reports should receive higher priority.
    public static final boolean PRIORITIZE_SAFETY_REPORTS = true;

    // ---------------------------------------------------------
    // Safe Communication
    // ---------------------------------------------------------

    public static final boolean ENABLE_SAFE_CHAT = true;
    public static final boolean CHAT_REQUIRES_MUTUAL_CONNECTION = true;
    public static final boolean PREVENT_UNMATCHED_DIRECT_MESSAGES = true;
    public static final boolean KEEP_PHONE_NUMBER_PRIVATE = true;
    public static final boolean KEEP_EMAIL_PRIVATE = true;

    // ---------------------------------------------------------
    // Contact Sharing Protection
    // ---------------------------------------------------------

    public static final boolean ENABLE_CONTACT_PRIVACY = true;
    public static final boolean WARN_BEFORE_CONTACT_SHARING = true;
    public static final boolean ALLOW_USER_TO_CONTROL_CONTACT_SHARING = true;

    // ---------------------------------------------------------
    // Safety Assistance
    // ---------------------------------------------------------

    public static final boolean ENABLE_SAFETY_ASSISTANT = true;
    public static final boolean ENABLE_HELP_LINE = true;
    public static final boolean SHOW_SAFETY_GUIDANCE = true;
    public static final boolean SHOW_FIRST_MEETING_SAFETY_TIPS = true;

    // ---------------------------------------------------------
    // Profile Trust
    // ---------------------------------------------------------

    public static final boolean ENABLE_VERIFIED_BADGE = true;
    public static final boolean ENABLE_ID_VERIFIED_BADGE = true;
    public static final boolean SHOW_VERIFICATION_STATUS = true;

    // Verification is a trust signal, not a guarantee of identity or safety.
    public static final boolean SHOW_VERIFICATION_DISCLAIMER = true;

    // ---------------------------------------------------------
    // Anti-Abuse Protection
    // ---------------------------------------------------------

    public static final boolean ENABLE_SPAM_PROTECTION = true;
    public static final boolean ENABLE_MESSAGE_RATE_LIMITING = true;
    public static final boolean ENABLE_REPEATED_REPORT_DETECTION = true;
    public static final boolean ENABLE_SUSPICIOUS_ACTIVITY_REVIEW = true;

    // ---------------------------------------------------------
    // Account Safety
    // ---------------------------------------------------------

    public static final boolean ENABLE_ACCOUNT_DELETION = true;
    public static final boolean ENABLE_ACCOUNT_BLOCKING = true;
    public static final boolean ENABLE_SECURITY_ALERTS = true;

    // ---------------------------------------------------------
    // Content Safety
    // ---------------------------------------------------------

    public static final boolean ENABLE_PROFILE_CONTENT_REVIEW = true;
    public static final boolean ENABLE_MESSAGE_SAFETY_REVIEW = true;
    public static final boolean DISCOURAGE_DATING_BEHAVIOR = true;

    // Best Nikah Bridge is intended for serious marriage purposes.
    public static final boolean MARRIAGE_FIRST_PLATFORM = true;

    // ---------------------------------------------------------
    // Admin Safety Review
    // ---------------------------------------------------------

    public static final boolean ENABLE_ADMIN_REPORT_REVIEW = true;
    public static final boolean ENABLE_ADMIN_USER_REVIEW = true;
    public static final boolean ENABLE_SAFETY_AUDIT_LOG = true;

    // ---------------------------------------------------------
    // Safety Principles
    // ---------------------------------------------------------

    public static final boolean SAFETY_FEATURES_REQUIRE_PAYMENT = false;
    public static final boolean VERIFICATION_REQUIRES_PAYMENT = false;
    public static final boolean REPORTING_REQUIRES_PAYMENT = false;
    public static final boolean BLOCKING_REQUIRES_PAYMENT = false;

    // Core safety should remain accessible to every user.
}
