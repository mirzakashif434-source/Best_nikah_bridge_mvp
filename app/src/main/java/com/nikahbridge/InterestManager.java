package com.nikahbridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Best Nikah Bridge - Interest Manager
 *
 * Controls the marriage-focused Express Interest flow:
 *
 * Profile
 *   ↓
 * Express Interest
 *   ↓
 * Accepted
 *   ↓
 * Mutual Connection
 *   ↓
 * Safe Chat
 *
 * This class keeps the business rules separate from the UI.
 * Production persistence, authentication and server-side enforcement
 * must be implemented separately.
 */
public final class InterestManager {

    public static final String STATUS_NONE = "NONE";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_DECLINED = "DECLINED";
    public static final String STATUS_MUTUAL = "MUTUAL";
    public static final String STATUS_BLOCKED = "BLOCKED";

    private static final Map<String, String> interestStates =
            new HashMap<>();

    private InterestManager() {
        // Prevent instantiation.
    }

    /**
     * Creates a unique key for two users.
     */
    private static String createKey(String senderId, String receiverId) {
        if (senderId == null || receiverId == null) {
            return "";
        }

        return senderId.trim() + "->" + receiverId.trim();
    }

    /**
     * Sends an Express Interest.
     *
     * Duplicate interests and blocked users are rejected.
     */
    public static synchronized boolean sendInterest(
            String senderId,
            String receiverId,
            boolean marriageIntentConfirmed) {

        if (senderId == null
                || receiverId == null
                || senderId.trim().isEmpty()
                || receiverId.trim().isEmpty()) {
            return false;
        }

        if (senderId.trim().equals(receiverId.trim())) {
            return false;
        }

        if (!marriageIntentConfirmed) {
            return false;
        }

        String key = createKey(senderId, receiverId);
        String currentStatus = interestStates.get(key);

        if (STATUS_PENDING.equals(currentStatus)
                || STATUS_ACCEPTED.equals(currentStatus)
                || STATUS_MUTUAL.equals(currentStatus)
                || STATUS_BLOCKED.equals(currentStatus)) {
            return false;
        }

        interestStates.put(key, STATUS_PENDING);
        return true;
    }

    /**
     * Accepts an incoming interest.
     *
     * If the receiver has already expressed interest back,
     * the connection becomes MUTUAL.
     */
    public static synchronized boolean acceptInterest(
            String senderId,
            String receiverId) {

        String incomingKey = createKey(senderId, receiverId);
        String reverseKey = createKey(receiverId, senderId);

        if (!interestStates.containsKey(incomingKey)) {
            return false;
        }

        if (STATUS_BLOCKED.equals(interestStates.get(incomingKey))
                || STATUS_BLOCKED.equals(interestStates.get(reverseKey))) {
            return false;
        }

        String reverseStatus = interestStates.get(reverseKey);

        if (STATUS_PENDING.equals(reverseStatus)
                || STATUS_ACCEPTED.equals(reverseStatus)) {

            interestStates.put(incomingKey, STATUS_MUTUAL);
            interestStates.put(reverseKey, STATUS_MUTUAL);
        } else {
            interestStates.put(incomingKey, STATUS_ACCEPTED);
        }

        return true;
    }

    /**
     * Declines an incoming interest.
     */
    public static synchronized boolean declineInterest(
            String senderId,
            String receiverId) {

        String key = createKey(senderId, receiverId);

        if (!interestStates.containsKey(key)) {
            return false;
        }

        if (STATUS_BLOCKED.equals(interestStates.get(key))) {
            return false;
        }

        interestStates.put(key, STATUS_DECLINED);
        return true;
    }

    /**
     * Blocks the connection in both directions.
     */
    public static synchronized boolean blockConnection(
            String userId,
            String otherUserId) {

        if (userId == null || otherUserId == null) {
            return false;
        }

        String key = createKey(userId, otherUserId);
        String reverseKey = createKey(otherUserId, userId);

        interestStates.put(key, STATUS_BLOCKED);
        interestStates.put(reverseKey, STATUS_BLOCKED);

        return true;
    }

    /**
     * Returns the current connection status.
     */
    public static synchronized String getStatus(
            String senderId,
            String receiverId) {

        String key = createKey(senderId, receiverId);
        String status = interestStates.get(key);

        return status == null ? STATUS_NONE : status;
    }

    /**
     * Returns true only when both users have a mutual connection.
     */
    public static synchronized boolean isMutual(
            String userId,
            String otherUserId) {

        String status = getStatus(userId, otherUserId);
        String reverseStatus = getStatus(otherUserId, userId);

        return STATUS_MUTUAL.equals(status)
                && STATUS_MUTUAL.equals(reverseStatus);
    }

    /**
     * Chat is allowed only after mutual acceptance.
     */
    public static synchronized boolean canStartChat(
            String userId,
            String otherUserId) {

        return isMutual(userId, otherUserId)
                && SafeChatConfig.canChat(ConnectionConfig.STATUS_MUTUAL);
    }

    /**
     * Returns a read-only snapshot of the current in-memory states.
     */
    public static synchronized Map<String, String> getStateSnapshot() {
        return Collections.unmodifiableMap(
                new HashMap<>(interestStates)
        );
    }

    /**
     * Clears in-memory state.
     *
     * Production code should normally use persistent storage instead.
     */
    public static synchronized void clearInMemoryState() {
        interestStates.clear();
    }
}
