package com.nikahbridge;

/**
 * Best Nikah Bridge - Admin Safety Manager
 *
 * Central configuration for admin-only safety actions.
 *
 * This class contains configuration/constants only.
 * Authentication, database persistence, authorization and
 * server-side enforcement must be implemented separately.
 */
public final class AdminSafetyManager {

    private AdminSafetyManager() {
        // Prevent instantiation.
    }

    // Admin-only safety actions.
    public static final String ACTION_REVIEW_REPORT = "REVIEW_REPORT";
    public static final String ACTION_BLOCK_USER = "BLOCK_USER";
    public static final String ACTION_UNBLOCK_USER = "UNBLOCK_USER";
    public static final String ACTION_REVIEW_VERIFICATION = "REVIEW_VERIFICATION";
    public static final String ACTION_APPROVE_VERIFICATION = "APPROVE_VERIFICATION";
    public static final String ACTION_REJECT_VERIFICATION = "REJECT_VERIFICATION";
    public static final String ACTION_SUSPEND_ACCOUNT = "SUSPEND_ACCOUNT";
    public static final String ACTION_RESTORE_ACCOUNT = "RESTORE_ACCOUNT";

    // Safety status values.
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REVIEWED = "REVIEWED";
    public static final String STATUS_BLOCKED = "BLOCKED";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_RESOLVED = "RESOLVED";

    // Report categories.
    public static final String REPORT_SAFETY = "SAFETY";
    public static final String REPORT_SCAM = "SCAM";
    public static final String REPORT_IMPERSONATION = "IMPERSONATION";
    public static final String REPORT_HARASSMENT = "HARASSMENT";
    public static final String REPORT_INAPPROPRIATE_CONTENT = "INAPPROPRIATE_CONTENT";
    public static final String REPORT_OTHER = "OTHER";

    // Verification states.
    public static final String VERIFICATION_PENDING = "PENDING";
    public static final String VERIFICATION_APPROVED = "APPROVED";
    public static final String VERIFICATION_REJECTED = "REJECTED";

    // Security defaults.
    public static final boolean ADMIN_ACTIONS_REQUIRE_AUTHENTICATION = true;
    public static final boolean ADMIN_ACTIONS_REQUIRE_AUTHORIZATION = true;
    public static final boolean SAFETY_ACTIONS_REQUIRE_AUDIT_LOG = true;

    /**
     * Returns whether an action is recognized as an admin safety action.
     */
    public static boolean isAdminAction(String action) {
        if (action == null) {
            return false;
        }

        return ACTION_REVIEW_REPORT.equals(action)
                || ACTION_BLOCK_USER.equals(action)
                || ACTION_UNBLOCK_USER.equals(action)
                || ACTION_REVIEW_VERIFICATION.equals(action)
                || ACTION_APPROVE_VERIFICATION.equals(action)
                || ACTION_REJECT_VERIFICATION.equals(action)
                || ACTION_SUSPEND_ACCOUNT.equals(action)
                || ACTION_RESTORE_ACCOUNT.equals(action);
    }

    /**
     * Returns whether a report category is supported.
     */
    public static boolean isValidReportCategory(String category) {
        if (category == null) {
            return false;
        }

        return REPORT_SAFETY.equals(category)
                || REPORT_SCAM.equals(category)
                || REPORT_IMPERSONATION.equals(category)
                || REPORT_HARASSMENT.equals(category)
                || REPORT_INAPPROPRIATE_CONTENT.equals(category)
                || REPORT_OTHER.equals(category);
    }

    /**
     * Returns whether a verification state is supported.
     */
    public static boolean isValidVerificationStatus(String status) {
        if (status == null) {
            return false;
        }

        return VERIFICATION_PENDING.equals(status)
                || VERIFICATION_APPROVED.equals(status)
                || VERIFICATION_REJECTED.equals(status);
    }
}
