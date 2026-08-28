package com.nikahbridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Best Nikah Bridge - Match Notification Manager
 *
 * Keeps notification events for important marriage-focused actions:
 * - New Express Interest
 * - Mutual connection
 * - Verification updates
 * - Profile/match updates
 *
 * This class is intentionally dependency-free.
 * Persistent notifications and push delivery should be implemented
 * securely by the application/backend layer.
 */
public final class MatchNotificationManager {

    public static final String TYPE_INTEREST = "INTEREST";
    public static final String TYPE_MUTUAL = "MUTUAL";
    public static final String TYPE_VERIFICATION = "VERIFICATION";
    public static final String TYPE_MATCH = "MATCH";
    public static final String TYPE_SAFETY = "SAFETY";

    private final List<Notification> notifications =
            new ArrayList<>();

    private MatchNotificationManager() {
        // Prevent direct instantiation.
    }

    private static final MatchNotificationManager INSTANCE =
            new MatchNotificationManager();

    /**
     * Returns the single manager instance.
     */
    public static MatchNotificationManager getInstance() {
        return INSTANCE;
    }

    /**
     * Adds a notification.
     *
     * @return true when the notification is valid and added.
     */
    public synchronized boolean addNotification(
            String userId,
            String type,
            String title,
            String message) {

        String normalizedUserId = normalize(userId);
        String normalizedType = normalize(type);
        String normalizedTitle = normalize(title);
        String normalizedMessage = normalize(message);

        if (normalizedUserId.isEmpty()
                || normalizedType.isEmpty()
                || normalizedTitle.isEmpty()
                || normalizedMessage.isEmpty()) {
            return false;
        }

        notifications.add(new Notification(
                normalizedUserId,
                normalizedType,
                normalizedTitle,
                normalizedMessage
        ));

        return true;
    }

    /**
     * Creates an Express Interest notification.
     */
    public synchronized boolean notifyInterest(
            String userId,
            String senderName) {

        String name = normalize(senderName);

        if (name.isEmpty()) {
            return false;
        }

        return addNotification(
                userId,
                TYPE_INTEREST,
                "New Interest",
                name + " expressed interest in your profile."
        );
    }

    /**
     * Creates a mutual-connection notification.
     */
    public synchronized boolean notifyMutual(
            String userId,
            String matchName) {

        String name = normalize(matchName);

        if (name.isEmpty()) {
            return false;
        }

        return addNotification(
                userId,
                TYPE_MUTUAL,
                "Mutual Connection",
                "You and " + name
                        + " have mutually accepted the connection."
        );
    }

    /**
     * Creates a verification-status notification.
     */
    public synchronized boolean notifyVerification(
            String userId,
            String statusMessage) {

        String message = normalize(statusMessage);

        if (message.isEmpty()) {
            return false;
        }

        return addNotification(
                userId,
                TYPE_VERIFICATION,
                "Verification Update",
                message
        );
    }

    /**
     * Creates a new-match notification.
     */
    public synchronized boolean notifyMatch(
            String userId,
            String matchName) {

        String name = normalize(matchName);

        if (name.isEmpty()) {
            return false;
        }

        return addNotification(
                userId,
                TYPE_MATCH,
                "New Match",
                "A compatible marriage profile is available: "
                        + name + "."
        );
    }

    /**
     * Creates a safety notification.
     */
    public synchronized boolean notifySafety(
            String userId,
            String safetyMessage) {

        String message = normalize(safetyMessage);

        if (message.isEmpty()) {
            return false;
        }

        return addNotification(
                userId,
                TYPE_SAFETY,
                "Safety Update",
                message
        );
    }

    /**
     * Returns all notifications for a user.
     */
    public synchronized List<Notification> getNotifications(
            String userId) {

        String normalizedUserId = normalize(userId);

        if (normalizedUserId.isEmpty()) {
            return Collections.emptyList();
        }

        List<Notification> result = new ArrayList<>();

        for (Notification notification : notifications) {
            if (normalizedUserId.equals(notification.getUserId())) {
                result.add(notification);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Returns unread notifications for a user.
     */
    public synchronized List<Notification> getUnreadNotifications(
            String userId) {

        String normalizedUserId = normalize(userId);

        if (normalizedUserId.isEmpty()) {
            return Collections.emptyList();
        }

        List<Notification> result = new ArrayList<>();

        for (Notification notification : notifications) {
            if (normalizedUserId.equals(notification.getUserId())
                    && !notification.isRead()) {
                result.add(notification);
            }
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Marks all notifications for a user as read.
     */
    public synchronized int markAllAsRead(String userId) {

        String normalizedUserId = normalize(userId);

        if (normalizedUserId.isEmpty()) {
            return 0;
        }

        int changed = 0;

        for (Notification notification : notifications) {
            if (normalizedUserId.equals(notification.getUserId())
                    && !notification.isRead()) {
                notification.markRead();
                changed++;
            }
        }

        return changed;
    }

    /**
     * Removes all local notifications for a user.
     *
     * Server-side notification records should be managed
     * independently and securely.
     */
    public synchronized int clearUserNotifications(String userId) {

        String normalizedUserId = normalize(userId);

        if (normalizedUserId.isEmpty()) {
            return 0;
        }

        int removed = 0;

        for (int i = notifications.size() - 1; i >= 0; i--) {
            if (normalizedUserId.equals(
                    notifications.get(i).getUserId())) {
                notifications.remove(i);
                removed++;
            }
        }

        return removed;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    /**
     * Immutable notification data with a mutable read state.
     */
    public static final class Notification {

        private final String userId;
        private final String type;
        private final String title;
        private final String message;
        private boolean read;

        private Notification(
                String userId,
                String type,
                String title,
                String message) {

            this.userId = userId;
            this.type = type;
            this.title = title;
            this.message = message;
            this.read = false;
        }

        public String getUserId() {
            return userId;
        }

        public String getType() {
            return type;
        }

        public String getTitle() {
            return title;
        }

        public String getMessage() {
            return message;
        }

        public boolean isRead() {
            return read;
        }

        private void markRead() {
            read = true;
        }
    }
}
