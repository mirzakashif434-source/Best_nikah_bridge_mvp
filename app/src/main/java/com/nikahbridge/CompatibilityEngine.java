package com.nikahbridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Best Nikah Bridge - Smart Compatibility Engine.
 *
 * Calculates a transparent 0-100 compatibility score from
 * user-selected marriage preferences and profile information.
 *
 * This engine is intentionally independent from MainActivity so
 * the matching system can evolve without replacing the main screen.
 */
public final class CompatibilityEngine {

    private CompatibilityEngine() {
        // Prevent instantiation.
    }

    // ============================================================
    // Match result
    // ============================================================

    public static final class MatchResult {

        private final int score;
        private final String level;
        private final List<String> reasons;

        private MatchResult(int score, String level, List<String> reasons) {
            this.score = score;
            this.level = level;
            this.reasons = Collections.unmodifiableList(
                    new ArrayList<>(reasons)
            );
        }

        public int getScore() {
            return score;
        }

        public String getLevel() {
            return level;
        }

        public List<String> getReasons() {
            return reasons;
        }

        public boolean isRecommended() {
            return score >= CompatibilityConfig.RECOMMENDED_MATCH_SCORE;
        }

        public boolean isHighCompatibility() {
            return score >= CompatibilityConfig.HIGH_COMPATIBILITY_SCORE;
        }
    }

    // ============================================================
    // Profile data
    // ============================================================

    /**
     * Lightweight matching profile.
     *
     * The application can later map its database/API profile
     * objects into this class without changing the scoring engine.
     */
    public static final class Profile {

        public final int age;
        public final String country;
        public final String marriageIntention;
        public final String religiousLevel;
        public final String lifestyle;
        public final String communicationPreference;

        public final int profileCompletionPercent;

        public final boolean verified;
        public final boolean idVerified;

        public final int preferredMinAge;
        public final int preferredMaxAge;

        public final String preferredCountry;

        public final List<String> partnerPreferences;

        public Profile(
                int age,
                String country,
                String marriageIntention,
                String religiousLevel,
                String lifestyle,
                String communicationPreference,
                int profileCompletionPercent,
                boolean verified,
                boolean idVerified,
                int preferredMinAge,
                int preferredMaxAge,
                String preferredCountry,
                List<String> partnerPreferences
        ) {
            this.age = age;
            this.country = safe(maybe(country));
            this.marriageIntention = safe(maybe(marriageIntention));
            this.religiousLevel = safe(maybe(religiousLevel));
            this.lifestyle = safe(maybe(lifestyle));
            this.communicationPreference =
                    safe(maybe(communicationPreference));

            this.profileCompletionPercent =
                    clamp(profileCompletionPercent, 0, 100);

            this.verified = verified;
            this.idVerified = idVerified;

            this.preferredMinAge = preferredMinAge;
            this.preferredMaxAge = preferredMaxAge;

            this.preferredCountry = safe(maybe(preferredCountry));

            if (partnerPreferences == null) {
                this.partnerPreferences = new ArrayList<>();
            } else {
                this.partnerPreferences =
                        new ArrayList<>(partnerPreferences);
            }
        }
    }

    // ============================================================
    // Main calculation
    // ============================================================

    /**
     * Calculate compatibility between two profiles.
     *
     * The result is always constrained to 0-100.
     */
    public static MatchResult calculate(Profile first, Profile second) {

        if (first == null || second == null) {
            return new MatchResult(
                    0,
                    "Insufficient data",
                    Collections.singletonList(
                            "More profile information is needed."
                    )
            );
        }

        List<String> reasons = new ArrayList<>();

        // --------------------------------------------------------
        // Hard safety/recommendation rules
        // --------------------------------------------------------

        if (CompatibilityConfig.REQUIRE_MARRIAGE_INTENTION
                && !sameMarriageGoal(first, second)) {

            return new MatchResult(
                    0,
                    "Not compatible",
                    Collections.singletonList(
                            "Marriage intentions do not align."
                    )
            );
        }

        // --------------------------------------------------------
        // Weighted scoring
        // --------------------------------------------------------

        int score = 0;

        // Age
        int ageScore = ageCompatibility(first, second);
        score += weighted(
                ageScore,
                CompatibilityConfig.AGE_WEIGHT
        );

        if (ageScore >= 80) {
            reasons.add("Age preferences are a strong match.");
        }

        // Marriage intention
        int intentionScore =
                marriageIntentionCompatibility(first, second);

        score += weighted(
                intentionScore,
                CompatibilityConfig.MARRIAGE_INTENTION_WEIGHT
        );

        if (intentionScore >= 80) {
            reasons.add("Marriage intentions align.");
        }

        // Partner preferences
        int preferenceScore =
                partnerPreferenceCompatibility(first, second);

        score += weighted(
                preferenceScore,
                CompatibilityConfig.PARTNER_PREFERENCES_WEIGHT
        );

        if (preferenceScore >= 70) {
            reasons.add("Partner preferences show good compatibility.");
        }

        // Location
        int locationScore =
                locationCompatibility(first, second);

        score += weighted(
                locationScore,
                CompatibilityConfig.LOCATION_WEIGHT
        );

        if (locationScore >= 80) {
            reasons.add("Location preferences are compatible.");
        }

        // Lifestyle / values
        int lifestyleScore =
                lifestyleCompatibility(first, second);

        score += weighted(
                lifestyleScore,
                CompatibilityConfig.VALUES_AND_LIFESTYLE_WEIGHT
        );

        if (lifestyleScore >= 80) {
            reasons.add("Lifestyle preferences are compatible.");
        }

        // Religious compatibility
        int religiousScore =
                religiousCompatibility(first, second);

        score += weighted(
                religiousScore,
                CompatibilityConfig.RELIGIOUS_COMPATIBILITY_WEIGHT
        );

        if (religiousScore >= 80) {
            reasons.add("Religious compatibility is strong.");
        }

        // Profile completeness
        int completionScore =
                profileCompletionCompatibility(first, second);

        score += weighted(
                completionScore,
                CompatibilityConfig.PROFILE_COMPLETENESS_WEIGHT
        );

        // Trust / verification
        int trustScore =
                trustCompatibility(first, second);

        score += weighted(
                trustScore,
                CompatibilityConfig.TRUST_AND_VERIFICATION_WEIGHT
        );

        if (trustScore >= 80) {
            reasons.add("Strong trust and verification signals.");
        }

        // Communication
        int communicationScore =
                communicationCompatibility(first, second);

        score += weighted(
                communicationScore,
                CompatibilityConfig.COMMUNICATION_PREFERENCES_WEIGHT
        );

        // --------------------------------------------------------
        // Final score
        // --------------------------------------------------------

        score = clamp(score, 0, CompatibilityConfig.MAX_SCORE);

        // Do not overwhelm the user with too many explanations.
        if (reasons.size() > CompatibilityConfig.MAX_MATCH_REASONS) {
            reasons = new ArrayList<>(
                    reasons.subList(
                            0,
                            CompatibilityConfig.MAX_MATCH_REASONS
                    )
            );
        }

        if (reasons.isEmpty()) {
            reasons.add(
                    CompatibilityConfig.DEFAULT_MATCH_REASON
            );
        }

        return new MatchResult(
                score,
                getScoreLevel(score),
                reasons
        );
    }

    // ============================================================
    // Age compatibility
    // ============================================================

    private static int ageCompatibility(Profile first, Profile second) {

        if (!CompatibilityConfig.RESPECT_AGE_PREFERENCES) {
            return 100;
        }

        boolean firstAcceptsSecond =
                isAgeAccepted(
                        second.age,
                        first.preferredMinAge,
                        first.preferredMaxAge
                );

        boolean secondAcceptsFirst =
                isAgeAccepted(
                        first.age,
                        second.preferredMinAge,
                        second.preferredMaxAge
                );

        if (firstAcceptsSecond && secondAcceptsFirst) {
            return 100;
        }

        if (firstAcceptsSecond || secondAcceptsFirst) {
            return 60;
        }

        int difference =
                Math.abs(first.age - second.age);

        if (difference <= 3) {
            return 70;
        }

        if (difference <= 7) {
            return 50;
        }

        return 25;
    }

    private static boolean isAgeAccepted(
            int age,
            int minAge,
            int maxAge
    ) {
        if (minAge <= 0 || maxAge <= 0) {
            return true;
        }

        int low = Math.min(minAge, maxAge);
        int high = Math.max(minAge, maxAge);

        return age >= low && age <= high;
    }

    // ============================================================
    // Marriage intention
    // ============================================================

    private static int marriageIntentionCompatibility(
            Profile first,
            Profile second
    ) {
        if (sameMarriageGoal(first, second)) {
            return 100;
        }

        if (containsAny(
                first.marriageIntention,
                "marriage",
                "nikah"
        ) && containsAny(
                second.marriageIntention,
                "marriage",
                "nikah"
        )) {
            return 80;
        }

        return 20;
    }

    private static boolean sameMarriageGoal(
            Profile first,
            Profile second
    ) {
        String a = normalize(first.marriageIntention);
        String b = normalize(second.marriageIntention);

        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }

        if (a.equals(b)) {
            return true;
        }

        boolean firstMarriage =
                containsAny(a, "marriage", "nikah", "serious");

        boolean secondMarriage =
                containsAny(b, "marriage", "nikah", "serious");

        return firstMarriage && secondMarriage;
    }

    // ============================================================
    // Partner preferences
    // ============================================================

    private static int partnerPreferenceCompatibility(
            Profile first,
            Profile second
    ) {
        if (first.partnerPreferences.isEmpty()
                || second.partnerPreferences.isEmpty()) {
            return 60;
        }

        int firstMatches = countMatches(
                first.partnerPreferences,
                second.partnerPreferences
        );

        int secondMatches = countMatches(
                second.partnerPreferences,
                first.partnerPreferences
        );

        int total =
                first.partnerPreferences.size()
                        + second.partnerPreferences.size();

        if (total == 0) {
            return 60;
        }

        double ratio =
                (double) (firstMatches + secondMatches)
                        / (double) total;

        if (ratio >= 0.75) {
            return 100;
        }

        if (ratio >= 0.50) {
            return 85;
        }

        if (ratio >= 0.25) {
            return 65;
        }

        return 40;
    }

    // ============================================================
    // Location
    // ============================================================

    private static int locationCompatibility(
            Profile first,
            Profile second
    ) {
        if (!CompatibilityConfig.RESPECT_LOCATION_PREFERENCES) {
            return 100;
        }

        if (isBlank(first.country)
                || isBlank(second.country)) {
            return 60;
        }

        if (equalsIgnoreCase(first.country, second.country)) {
            return 100;
        }

        boolean firstPrefersSecond =
                equalsIgnoreCase(
                        first.preferredCountry,
                        second.country
                );

        boolean secondPrefersFirst =
                equalsIgnoreCase(
                        second.preferredCountry,
                        first.country
                );

        if (firstPrefersSecond && secondPrefersFirst) {
            return 100;
        }

        if (firstPrefersSecond || secondPrefersFirst) {
            return 80;
        }

        return 50;
    }

    // ============================================================
    // Lifestyle / values
    // ============================================================

    private static int lifestyleCompatibility(
            Profile first,
            Profile second
    ) {
        if (isBlank(first.lifestyle)
                || isBlank(second.lifestyle)) {
            return 60;
        }

        if (equalsIgnoreCase(
                first.lifestyle,
                second.lifestyle
        )) {
            return 100;
        }

        if (containsSharedKeyword(
                first.lifestyle,
                second.lifestyle
        )) {
            return 80;
        }

        return 50;
    }

    // ============================================================
    // Religious compatibility
    // ============================================================

    private static int religiousCompatibility(
            Profile first,
            Profile second
    ) {
        if (isBlank(first.religiousLevel)
                || isBlank(second.religiousLevel)) {
            return 60;
        }

        if (equalsIgnoreCase(
                first.religiousLevel,
                second.religiousLevel
        )) {
            return 100;
        }

        if (containsAny(
                first.religiousLevel,
                "practicing",
                "religious"
        ) && containsAny(
                second.religiousLevel,
                "practicing",
                "religious"
        )) {
            return 80;
        }

        return 50;
    }

    // ============================================================
    // Profile completion
    // ============================================================

    private static int profileCompletionCompatibility(
            Profile first,
            Profile second
    ) {
        int average =
                (first.profileCompletionPercent
                        + second.profileCompletionPercent) / 2;

        if (average >= 90) {
            return 100;
        }

        if (average >= 80) {
            return 90;
        }

        if (average >= CompatibilityConfig.MIN_PROFILE_COMPLETION_PERCENT) {
            return 75;
        }

        return 45;
    }

    // ============================================================
    // Trust / verification
    // ============================================================

    private static int trustCompatibility(
            Profile first,
            Profile second
    ) {
        int firstTrust = trustValue(first);
        int secondTrust = trustValue(second);

        return (firstTrust + secondTrust) / 2;
    }

    private static int trustValue(Profile profile) {

        if (profile.idVerified) {
            return 100;
        }

        if (profile.verified) {
            return 85;
        }

        return 55;
    }

    // ============================================================
    // Communication
    // ============================================================

    private static int communicationCompatibility(
            Profile first,
            Profile second
    ) {
        if (isBlank(first.communicationPreference)
                || isBlank(second.communicationPreference)) {
            return 60;
        }

        if (equalsIgnoreCase(
                first.communicationPreference,
                second.communicationPreference
        )) {
            return 100;
        }

        return 65;
    }

    // ============================================================
    // Score helpers
    // ============================================================

    private static int weighted(
            int percentage,
            int weight
    ) {
        return (percentage * weight) / 100;
    }

    private static String getScoreLevel(int score) {

        if (score >= CompatibilityConfig.EXCELLENT_COMPATIBILITY_SCORE) {
            return "Excellent Match";
        }

        if (score >= CompatibilityConfig.HIGH_COMPATIBILITY_SCORE) {
            return "High Compatibility";
        }

        if (score >= CompatibilityConfig.RECOMMENDED_MATCH_SCORE) {
            return "Recommended Match";
        }

        if (score >= 50) {
            return "Possible Match";
        }

        return "Low Compatibility";
    }

    // ============================================================
    // Collection helpers
    // ============================================================

    private static int countMatches(
            List<String> first,
            List<String> second
    ) {
        int count = 0;

        for (String a : first) {
            for (String b : second) {
                if (equalsIgnoreCase(a, b)) {
                    count++;
                    break;
                }
            }
        }

        return count;
    }

    private static boolean containsSharedKeyword(
            String first,
            String second
    ) {
        String[] firstWords =
                normalize(first).split("\\s+");

        String normalizedSecond =
                normalize(second);

        for (String word : firstWords) {
            if (word.length() >= 4
                    && normalizedSecond.contains(word)) {
                return true;
            }
        }

        return false;
    }

    // ============================================================
    // String helpers
    // ============================================================

    private static String maybe(String value) {
        return value == null ? "" : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalize(String value) {
        return safe(value)
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static boolean equalsIgnoreCase(
            String first,
            String second
    ) {
        if (isBlank(first) || isBlank(second)) {
            return false;
        }

        return normalize(first).equals(
                normalize(second)
        );
    }

    private static boolean containsAny(
            String value,
            String... keywords
    ) {
        String normalized = normalize(value);

        if (normalized.isEmpty()) {
            return false;
        }

        for (String keyword : keywords) {
            if (normalized.contains(
                    normalize(keyword)
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static int clamp(
            int value,
            int min,
            int max
    ) {
        return Math.max(min, Math.min(max, value));
    }
              }
