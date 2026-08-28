package com.nikahbridge;

/**
 * Best Nikah Bridge - Smart Compatibility Configuration.
 *
 * Central configuration for the future compatibility engine.
 * This class contains scoring weights, thresholds and matching rules.
 *
 * Important:
 * - This is configuration only; no payment credentials or secrets are stored here.
 * - Final compatibility decisions should be made from verified profile data.
 * - A compatibility score is a recommendation, not a guarantee of marriage success.
 */
public final class CompatibilityConfig {

    private CompatibilityConfig() {
        // Prevent instantiation.
    }

    // ============================================================
    // Compatibility score
    // ============================================================

    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 100;

    /**
     * Minimum score required for a profile to be considered
     * a recommended match.
     */
    public static final int RECOMMENDED_MATCH_SCORE = 70;

    /**
     * High compatibility threshold.
     */
    public static final int HIGH_COMPATIBILITY_SCORE = 80;

    /**
     * Very strong compatibility threshold.
     */
    public static final int EXCELLENT_COMPATIBILITY_SCORE = 90;

    // ============================================================
    // Matching weights
    // Total = 100
    // ============================================================

    public static final int AGE_WEIGHT = 15;

    public static final int MARRIAGE_INTENTION_WEIGHT = 15;

    public static final int PARTNER_PREFERENCES_WEIGHT = 15;

    public static final int LOCATION_WEIGHT = 10;

    public static final int VALUES_AND_LIFESTYLE_WEIGHT = 15;

    public static final int RELIGIOUS_COMPATIBILITY_WEIGHT = 10;

    public static final int PROFILE_COMPLETENESS_WEIGHT = 5;

    public static final int TRUST_AND_VERIFICATION_WEIGHT = 10;

    public static final int COMMUNICATION_PREFERENCES_WEIGHT = 5;

    // ============================================================
    // Profile quality requirements
    // ============================================================

    /**
     * Profiles below this completion percentage should not receive
     * the strongest recommendation level.
     */
    public static final int MIN_PROFILE_COMPLETION_PERCENT = 70;

    /**
     * Fully completed profile target.
     */
    public static final int FULL_PROFILE_COMPLETION_PERCENT = 100;

    // ============================================================
    // Trust levels
    // ============================================================

    public static final int TRUST_BASIC_BONUS = 0;

    public static final int TRUST_VERIFIED_BONUS = 5;

    public static final int TRUST_ID_VERIFIED_BONUS = 10;

    // ============================================================
    // Recommendation rules
    // ============================================================

    /**
     * Do not recommend profiles that fail a mandatory compatibility rule.
     */
    public static final boolean ENABLE_HARD_MATCH_RULES = true;

    /**
     * Marriage intention is treated as an important compatibility factor.
     */
    public static final boolean REQUIRE_MARRIAGE_INTENTION = true;

    /**
     * Respect the user's selected age preferences.
     */
    public static final boolean RESPECT_AGE_PREFERENCES = true;

    /**
     * Respect selected location preferences when supplied.
     */
    public static final boolean RESPECT_LOCATION_PREFERENCES = true;

    /**
     * Respect selected partner preferences.
     */
    public static final boolean RESPECT_PARTNER_PREFERENCES = true;

    /**
     * Give additional confidence to profiles with verification.
     */
    public static final boolean USE_VERIFICATION_IN_SCORE = true;

    // ============================================================
    // Match explanation
    // ============================================================

    /**
     * Enable human-readable reasons such as:
     * "Same marriage intention"
     * "Age preference matches"
     * "Strong preference compatibility"
     */
    public static final boolean ENABLE_MATCH_REASONS = true;

    /**
     * Maximum number of reasons displayed on a match card.
     */
    public static final int MAX_MATCH_REASONS = 4;

    // ============================================================
    // Safety and trust
    // ============================================================

    /**
     * Reported/blocked profiles must never be recommended.
     */
    public static final boolean EXCLUDE_BLOCKED_PROFILES = true;

    public static final boolean EXCLUDE_REPORTED_PROFILES = true;

    /**
     * A profile that has been blocked by either side must not
     * enter the recommendation pool.
     */
    public static final boolean BLOCK_MUTUAL_RECOMMENDATION = true;

    // ============================================================
    // Mutual interest
    // ============================================================

    /**
     * Express Interest remains one-way until the other person
     * also accepts/expresses interest.
     */
    public static final boolean REQUIRE_MUTUAL_INTEREST_FOR_CHAT = true;

    /**
     * Prevent duplicate interest actions.
     */
    public static final boolean PREVENT_DUPLICATE_INTEREST = true;

    // ============================================================
    // Score bands
    // ============================================================

    public static final int SCORE_BAND_LOW_MAX = 49;

    public static final int SCORE_BAND_POSSIBLE_MAX = 69;

    public static final int SCORE_BAND_RECOMMENDED_MAX = 79;

    public static final int SCORE_BAND_HIGH_MAX = 89;

    public static final int SCORE_BAND_EXCELLENT_MAX = 100;

    // ============================================================
    // Future engine versioning
    // ============================================================

    /**
     * Version of the compatibility rules.
     * Keeping this separate makes future scoring improvements easier
     * without changing the rest of the application structure.
     */
    public static final String ENGINE_VERSION = "1.0";

    /**
     * Default explanation shown when no stronger reason is available.
     */
    public static final String DEFAULT_MATCH_REASON =
            "Your preferences and marriage goals show good compatibility.";

    /**
     * Validate the configuration at runtime during development.
     */
    public static boolean isConfigurationValid() {

        int totalWeight =
                AGE_WEIGHT
                        + MARRIAGE_INTENTION_WEIGHT
                        + PARTNER_PREFERENCES_WEIGHT
                        + LOCATION_WEIGHT
                        + VALUES_AND_LIFESTYLE_WEIGHT
                        + RELIGIOUS_COMPATIBILITY_WEIGHT
                        + PROFILE_COMPLETENESS_WEIGHT
                        + TRUST_AND_VERIFICATION_WEIGHT
                        + COMMUNICATION_PREFERENCES_WEIGHT;

        return totalWeight == 100
                && MIN_SCORE == 0
                && MAX_SCORE == 100
                && RECOMMENDED_MATCH_SCORE >= 0
                && HIGH_COMPATIBILITY_SCORE >= RECOMMENDED_MATCH_SCORE
                && EXCELLENT_COMPATIBILITY_SCORE >= HIGH_COMPATIBILITY_SCORE
                && MIN_PROFILE_COMPLETION_PERCENT >= 0
                && MIN_PROFILE_COMPLETION_PERCENT <= 100
                && FULL_PROFILE_COMPLETION_PERCENT == 100
                && MAX_MATCH_REASONS > 0;
    }
}
