package com.nikahbridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Best Nikah Bridge - Report & Block Manager
 *
 * Marriage-focused safety controls:
 * - Block unwanted users
 * - Report profiles or behaviour
 * - Prevent duplicate reports
 * - Keep moderation data separate from UI
 *
 * This class is intentionally dependency-free so it can be
 * safely integrated with the Android app and a future backend.
 */
public final class ReportBlockManager {

    public static final String REPORT_PROFILE = "PROFILE";
    public static final String REPORT_HARASSMENT = "HARASSMENT";
    public static final String REPORT_SCAM = "SCAM";
    public static final String REPORT_IMPERSONATION = "IMPERSONATION";
    public static final String REPORT_INAPPROPRIATE = "INAPPROPRIATE";
    public static final String REPORT_OTHER = "OTHER";

    private final Set<String> blockedUsers = new HashSet<>();
    private final Map<String, Set<String>> reportsByReporter =
            new HashMap<>();

    private ReportBlockManager() {
        // Prevent direct instantiation.
    }

    private static final ReportBlockManager INSTANCE =
            new ReportBlockManager();

    /**
     * Returns the single manager instance.
     */
    public static ReportBlockManager getInstance() {
        return INSTANCE;
    }

    /**
     * Blocks another user.
     *
     * @param userId user being blocked
     * @return true when the user ID is valid and is now blocked
     */
    public synchronized boolean blockUser(String userId) {
        String id = normalize(userId);

        if (id.isEmpty()) {
            return false;
        }

        blockedUsers.add(id);
        return true;
    }

    /**
     * Removes a user from the local block list.
     */
    public synchronized boolean unblockUser(String userId) {
        String id = normalize(userId);

        if (id.isEmpty()) {
            return false;
        }

        return blockedUsers.remove(id);
    }

    /**
     * Checks whether a user is blocked.
     */
    public synchronized boolean isBlocked(String userId) {
        String id = normalize(userId);

        if (id.isEmpty()) {
            return false;
        }

        return blockedUsers.contains(id);
    }

    /**
     * Returns a read-only snapshot of blocked users.
     */
    public synchronized Set<String> getBlockedUsers() {
        return Collections.unmodifiableSet(
                new HashSet<>(blockedUsers)
        );
    }

    /**
     * Records a report locally.
     *
     * Duplicate reports from the same reporter for the same
     * target and reason are ignored.
     *
     * The actual report must eventually be submitted to a
     * secure backend/moderation system.
     *
     * @param reporterId person submitting the report
     * @param targetUserId person being reported
     * @param reason report category
     * @return true if a new report was recorded
     */
    public synchronized boolean reportUser(
            String reporterId,
            String targetUserId,
            String reason) {

        String reporter = normalize(reporterId);
        String target = normalize(targetUserId);
        String reportReason = normalize(reason);

        if (reporter.isEmpty()
                || target.isEmpty()
                || reportReason.isEmpty()) {
            return false;
        }

        if (reporter.equals(target)) {
            return false;
        }

        String reportKey = target + "|" + reportReason;

        Set<String> reporterReports =
                reportsByReporter.get(reporter);

        if (reporterReports == null) {
            reporterReports = new HashSet<>();
            reportsByReporter.put(reporter, reporterReports);
        }

        if (reporterReports.contains(reportKey)) {
            return false;
        }

        reporterReports.add(reportKey);
        return true;
    }

    /**
     * Checks whether the reporter has already submitted
     * the same report for the target.
     */
    public synchronized boolean hasReported(
            String reporterId,
            String targetUserId,
            String reason) {

        String reporter = normalize(reporterId);
        String target = normalize(targetUserId);
        String reportReason = normalize(reason);

        if (reporter.isEmpty()
                || target.isEmpty()
                || reportReason.isEmpty()) {
            return false;
        }

        Set<String> reporterReports =
                reportsByReporter.get(reporter);

        if (reporterReports == null) {
            return false;
        }

        return reporterReports.contains(
                target + "|" + reportReason
        );
    }

    /**
     * Clears local safety state.
     *
     * Backend moderation records must never depend on this
     * method and should be retained securely on the server.
     */
    public synchronized void clearLocalState() {
        blockedUsers.clear();
        reportsByReporter.clear();
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
