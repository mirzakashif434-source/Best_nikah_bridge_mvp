package com.nikahbridge;

/**
 * Best Nikah Bridge - Audit Log Manager
 *
 * Defines safe, structured audit events for important account,
 * verification, reporting and admin actions.
 *
 * This class contains configuration/constants only.
 * Actual storage, authentication, authorization and server-side
 * audit persistence must be implemented separately.
 */
public final class AuditLogManager {

    private AuditLogManager() {
        // Prevent instantiation.
    }

    // General event categories.
    public static final String CATEGORY_ACCOUNT = "ACCOUNT";
    public static final String CATEGORY_PROFILE = "PROFILE";
    public static final String CATEGORY_VERIFICATION = "VERIFICATION";
    public static final String CATEGORY_CONNECTION = "CONNECTION";
    public static final String CATEGORY_CHAT = "CHAT";
    public static final String CATEGORY_SAFETY = "SAFETY";
    public static final String CATEGORY_ADMIN = "ADMIN";

    // Account events.
    public static final String EVENT_ACCOUNT_CREATED = "ACCOUNT_CREATED";
    public static final String EVENT_ACCOUNT_UPDATED = "ACCOUNT_UPDATED";
    public static final String EVENT_ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";
    public static final String EVENT_ACCOUNT_RESTORED = "ACCOUNT_RESTORED";
    public static final String EVENT_ACCOUNT_DELETED = "ACCOUNT_DELETED";

    // Verification events.
    public static final String EVENT_VERIFICATION_SUBMITTED =
            "VERIFICATION_SUBMITTED";
    public static final String EVENT_VERIFICATION_APPROVED =
            "VERIFICATION_APPROVED";
    public static final String EVENT_VERIFICATION_REJECTED =
            "VERIFICATION_REJECTED";

    // Connection events.
    public static final String EVENT_INTEREST_SENT = "INTEREST_SENT";
    public static final String EVENT_INTEREST_ACCEPTED = "INTEREST_ACCEPTED";
    public static final String EVENT_INTEREST_DECLINED = "INTEREST_DECLINED";
    public static final String EVENT_CONNECTION_CREATED =
            "CONNECTION_CREATED";

    // Safety events.
    public static final String EVENT_REPORT_CREATED = "REPORT_CREATED";
    public static final String EVENT_USER_BLOCKED = "USER_BLOCKED";
    public static final String EVENT_USER_UNBLOCKED = "USER_UNBLOCKED";

    // Admin events.
    public static final String EVENT_REPORT_REVIEWED = "REPORT_REVIEWED";
    public static final String EVENT_ADMIN_ACTION = "ADMIN_ACTION";

    // Audit severity levels.
    public static final String SEVERITY_INFO = "INFO";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String SEVERITY_CRITICAL = "CRITICAL";

    /**
     * Returns whether the supplied event name is supported.
     */
    public static boolean isValidEvent(String event) {
        if (event == null) {
            return false;
        }

        return EVENT_ACCOUNT_CREATED.equals(event)
                || EVENT_ACCOUNT_UPDATED.equals(event)
                || EVENT_ACCOUNT_SUSPENDED.equals(event)
                || EVENT_ACCOUNT_RESTORED.equals(event)
                || EVENT_ACCOUNT_DELETED.equals(event)
                || EVENT_VERIFICATION_SUBMITTED.equals(event)
                || EVENT_VERIFICATION_APPROVED.equals(event)
                || EVENT_VERIFICATION_REJECTED.equals(event)
                || EVENT_INTEREST_SENT.equals(event)
                || EVENT_INTEREST_ACCEPTED.equals(event)
                || EVENT_INTEREST_DECLINED.equals(event)
                || EVENT_CONNECTION_CREATED.equals(event)
                || EVENT_REPORT_CREATED.equals(event)
                || EVENT_USER_BLOCKED.equals(event)
                || EVENT_USER_UNBLOCKED.equals(event)
                || EVENT_REPORT_REVIEWED.equals(event)
                || EVENT_ADMIN_ACTION.equals(event);
    }

    /**
     * Returns whether the supplied severity is supported.
     */
    public static boolean isValidSeverity(String severity) {
        if (severity == null) {
            return false;
        }

        return SEVERITY_INFO.equals(severity)
                || SEVERITY_WARNING.equals(severity)
                || SEVERITY_CRITICAL.equals(severity);
    }

    /**
     * Returns whether an event should receive elevated review.
     */
    public static boolean requiresElevatedReview(String event) {
        if (event == null) {
            return false;
        }

        return EVENT_ACCOUNT_SUSPENDED.equals(event)
                || EVENT_ACCOUNT_DELETED.equals(event)
                || EVENT_VERIFICATION_REJECTED.equals(event)
                || EVENT_USER_BLOCKED.equals(event)
                || EVENT_ADMIN_ACTION.equals(event);
    }
}
