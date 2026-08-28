package com.nikahbridge;

/**
 * Best Nikah Bridge - Safety Policy Configuration
 *
 * Central configuration for user safety, reporting, blocking,
 * moderation and privacy-related application rules.
 *
 * This class contains configuration/constants only.
 * Authentication, database persistence, moderation workflows
 * and server-side enforcement must be implemented separately.
 */
public final class SafetyPolicyConfig {

    private SafetyPolicyConfig() {
        // Prevent instantiation.
    }

    // Safety defaults.
    public static final boolean REPORTING_ENABLED = true;
    public static final boolean BLOCKING_ENABLED = true;
    public static final boolean PRIVACY_PROTECTION_ENABLED = true;
    public static final boolean MODERATION_REQUIRED = true;

    // Communication safety.
    public static final boolean CHAT_REQUIRES_MUTUAL_ACCEPTANCE = true;
    public static final boolean CONTACT_INFO_PRIVATE_BY_DEFAULT = true;
    public static final boolean UNSAFE_CONTACT_SHARING_RESTRICTED = true;

    // Report handling.
    public static final String REPORT_STATUS_PENDING = "PENDING";
    public static final String REPORT_STATUS_REVIEWED = "REVIEWED";
    public static final String REPORT_STATUS_RESOLVED = "RESOLVED";

    // Supported report categories.
    public static final String REPORT_CATEGORY_SAFETY = "SAFETY";
    public static final String REPORT_CATEGORY_SCAM = "SCAM";
    public static final String REPORT_CATEGORY_IMPERSONATION =
            "IMPERSONATION";
    public static final String REPORT_CATEGORY_HARASSMENT = "HARASSMENT";
    public static final String REPORT_CATEGORY_INAPPROPRIATE_CONTENT =
            "INAPPROPRIATE_CONTENT";
    public static final String REPORT_CATEGORY_OTHER = "OTHER";

    // Account safety states.
    public static final String ACCOUNT_ACTIVE = "ACTIVE";
    public static final String ACCOUNT_REVIEW = "REVIEW";
    public static final String ACCOUNT_SUSPENDED = "SUSPENDED";
    public static final String ACCOUNT_BLOCKED = "BLOCKED";

    /**
     * Returns whether reporting is enabled.
     */
    public static boolean isReportingEnabled() {
        return REPORTING_ENABLED;
    }

    /**
     * Returns whether blocking is enabled.
     */
    public static boolean isBlockingEnabled() {
        return BLOCKING_ENABLED;
    }

    /**
     * Returns whether a report category is supported.
     */
    public static boolean isValidReportCategory(String category) {
        if (category == null) {
            return false;
        }

        return REPORT_CATEGORY_SAFETY.equals(category)
                || REPORT_CATEGORY_SCAM.equals(category)
                || REPORT_CATEGORY_IMPERSONATION.equals(category)
                || REPORT_CATEGORY_HARASSMENT.equals(category)
                || REPORT_CATEGORY_INAPPROPRIATE_CONTENT.equals(category)
                || REPORT_CATEGORY_OTHER.equals(category);
    }

    /**
     * Returns whether a report status is supported.
     */
    public static boolean isValidReportStatus(String status) {
        if (status == null) {
            return false;
        }

        return REPORT_STATUS_PENDING.equals(status)
                || REPORT_STATUS_REVIEWED.equals(status)
                || REPORT_STATUS_RESOLVED.equals(status);
    }

    /**
     * Returns whether an account state is supported.
     */
    public static boolean isValidAccountState(String state) {
        if (state == null) {
            return false;
        }

        return ACCOUNT_ACTIVE.equals(state)
                || ACCOUNT_REVIEW.equals(state)
                || ACCOUNT_SUSPENDED.equals(state)
                || ACCOUNT_BLOCKED.equals(state);
    }
}
