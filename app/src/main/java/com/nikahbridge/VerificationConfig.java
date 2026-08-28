package com.nikahbridge;

/**
 * Best Nikah Bridge - Verification Configuration.
 *
 * Central configuration for profile, identity and trust verification.
 *
 * Verification must remain accessible and must not require payment.
 * Payment must never be used as a trust signal.
 *
 * This class contains configuration only.
 * Actual document processing, secure storage and server-side
 * verification must be implemented separately.
 */
public final class VerificationConfig {

    private VerificationConfig() {
        // Prevent instantiation.
    }

    // ---------------------------------------------------------
    // Verification types
    // ---------------------------------------------------------

    public static final String TYPE_PROFILE = "PROFILE";
    public static final String TYPE_IDENTITY = "IDENTITY";
    public static final String TYPE_PHONE = "PHONE";
    public static final String TYPE_EMAIL = "EMAIL";

    // ---------------------------------------------------------
    // Verification status
    // ---------------------------------------------------------

    public static final String STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_VERIFIED = "VERIFIED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    // ---------------------------------------------------------
    // Trust levels
    // ---------------------------------------------------------

    public static final String TRUST_BASIC = "Basic";
    public static final String TRUST_PROFILE_VERIFIED = "Profile Verified";
    public static final String TRUST_ID_VERIFIED = "ID Verified";

    // ---------------------------------------------------------
    // Verification requirements
    // ---------------------------------------------------------

    public static final boolean PROFILE_VERIFICATION_ENABLED = true;
    public static final boolean IDENTITY_VERIFICATION_ENABLED = true;
    public static final boolean PHONE_VERIFICATION_ENABLED = true;
    public static final boolean EMAIL_VERIFICATION_ENABLED = true;

    // Verification is never locked behind payment.
    public static final boolean VERIFICATION_REQUIRES_PAYMENT = false;

    // ---------------------------------------------------------
    // Profile quality requirements
    // ---------------------------------------------------------

    public static final int MIN_PROFILE_COMPLETION_PERCENT = 70;

    public static final boolean REQUIRE_PROFILE_PHOTO = true;
    public static final boolean REQUIRE_MARRIAGE_INTENTION = true;
    public static final boolean REQUIRE_PARTNER_PREFERENCES = true;

    // ---------------------------------------------------------
    // Safety controls
    // ---------------------------------------------------------

    public static final boolean SHOW_VERIFIED_BADGE = true;
    public static final boolean SHOW_TRUST_LEVEL = true;
    public static final boolean ENABLE_REPORT_BUTTON = true;
    public static final boolean ENABLE_BLOCK_BUTTON = true;

    // Private verification documents must never be public.
    public static final boolean VERIFICATION_DOCUMENTS_PUBLIC = false;

    // ---------------------------------------------------------
    // Review controls
    // ---------------------------------------------------------

    public static final boolean ENABLE_MANUAL_REVIEW = true;
    public static final boolean ENABLE_VERIFICATION_RETRY = true;

    public static final int MAX_VERIFICATION_RETRY_ATTEMPTS = 3;

    // ---------------------------------------------------------
    // Verification validity
    // ---------------------------------------------------------

    public static final int IDENTITY_VERIFICATION_VALIDITY_DAYS = 365;

    // ---------------------------------------------------------
    // Badge rules
    // ---------------------------------------------------------

    public static final String BADGE_NONE = "NONE";
    public static final String BADGE_PROFILE = "PROFILE_VERIFIED";
    public static final String BADGE_ID = "ID_VERIFIED";

    public static final boolean ID_VERIFICATION_HAS_PRIORITY = true;

    // ---------------------------------------------------------
    // Privacy rules
    // ---------------------------------------------------------

    public static final boolean HIDE_ID_DOCUMENTS_FROM_USERS = true;
    public static final boolean HIDE_PRIVATE_CONTACT_DETAILS = true;
    public static final boolean VERIFICATION_VISIBLE_TO_MATCHES = true;

    // ---------------------------------------------------------
    // Admin safety
    // ---------------------------------------------------------

    public static final boolean ADMIN_REVIEW_REQUIRED_FOR_ID = true;
    public static final boolean KEEP_VERIFICATION_AUDIT_LOG = true;

    // ---------------------------------------------------------
    // Future-ready trust features
    // ---------------------------------------------------------

    public static final boolean ENABLE_VERIFICATION_HISTORY = true;
    public static final boolean ENABLE_TRUST_SCORE = true;
    public static final boolean ENABLE_SAFETY_ASSISTANT = true;

    // A trust score is informational only.
    public static final boolean TRUST_SCORE_IS_GUARANTEE = false;
}
