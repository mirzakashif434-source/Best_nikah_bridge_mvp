package com.nikahbridge;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private LinearLayout root;

    private final int blue = Color.rgb(43, 107, 87);
    private final int dark = Color.rgb(24, 50, 44);
    private final int gray = Color.rgb(100, 117, 111);
    private final int lightBg = Color.rgb(247, 250, 249);
    private final int white = Color.WHITE;
    private final int border = Color.rgb(220, 230, 226);

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("BestNikahBridge", MODE_PRIVATE);

        showHome();
    }

    // =========================================================
    // BASIC UI HELPERS
    // =========================================================

    private TextView title(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(dark);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 12, 12, 12);
        return t;
    }

    private TextView subtitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(17);
        t.setTextColor(gray);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 8, 12, 20);
        return t;
    }

    private TextView bodyText(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(16);
        t.setTextColor(dark);
        t.setPadding(12, 8, 12, 8);
        return t;
    }

    private Button appButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(58);
        b.setPadding(18, 10, 18, 10);
        b.setBackgroundColor(blue);
        return b;
    }

    private Button secondaryButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTextColor(blue);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(54);
        b.setPadding(18, 8, 18, 8);
        b.setBackgroundColor(Color.rgb(232, 242, 238));
        return b;
    }

    private void setupRoot() {

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(lightBg);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(24, 30, 24, 30);

        scroll.addView(
                root,
                new ScrollView.LayoutParams(
                        ScrollView.LayoutParams.MATCH_PARENT,
                        ScrollView.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(scroll);
    }

    private void addSpace(int height) {
        Space space = new Space(this);

        root.addView(
                space,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        height
                )
        );
    }

    private void addInput(EditText input, int height) {

        input.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        height
                );

        params.setMargins(0, 0, 0, 10);

        root.addView(input, params);
    }

    // =========================================================
    // HOME
    // =========================================================

    private void showHome() {

        setupRoot();

        addSpace(20);

        root.addView(
                title("Best Nikah Bridge", 32),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                subtitle("Trusted • Simple • Safe • Serious Nikah"),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        addSpace(10);

        Button createProfile = appButton("Create / Update Profile");

        root.addView(
                createProfile,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        70
                )
        );

        addSpace(14);

        Button viewMatches = appButton("View Recommended Matches");

        root.addView(
                viewMatches,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        70
                )
        );

        createProfile.setOnClickListener(v -> showCreateProfile());
        viewMatches.setOnClickListener(v -> showMatches());

        addSpace(35);

        TextView info = new TextView(this);

        info.setText(
                "A serious Muslim matrimonial platform\n" +
                "for safe and respectful Nikah.\n\n" +
                "Our matching system considers age,\n" +
                "country and marriage intention."
        );

        info.setTextSize(16);
        info.setTextColor(gray);
        info.setGravity(Gravity.CENTER);
        info.setPadding(10, 10, 10, 10);

        root.addView(
                info,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );
    }

    // =========================================================
    // CREATE PROFILE
    // =========================================================

    private void showCreateProfile() {

        setupRoot();

        root.setGravity(Gravity.TOP);

        root.addView(
                title("Create Your Profile", 28),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        TextView guide = bodyText(
                "Complete your profile so we can recommend better Nikah matches."
        );
        guide.setGravity(Gravity.CENTER);

        root.addView(guide);

        addSpace(10);

        EditText name = new EditText(this);
        name.setHint("Full name");
        name.setTextSize(17);

        if (prefs.contains("name")) {
            name.setText(prefs.getString("name", ""));
        }

        addInput(name, 65);

        EditText age = new EditText(this);
        age.setHint("Age");
        age.setTextSize(17);
        age.setInputType(InputType.TYPE_CLASS_NUMBER);

        if (prefs.contains("age")) {
            age.setText(String.valueOf(prefs.getInt("age", 0)));
        }

        addInput(age, 65);

        EditText country = new EditText(this);
        country.setHint("Country");
        country.setTextSize(17);

        if (prefs.contains("country")) {
            country.setText(prefs.getString("country", ""));
        }

        addInput(country, 65);

        EditText intention = new EditText(this);
        intention.setHint("Marriage intention — e.g. Serious");
        intention.setTextSize(17);

        if (prefs.contains("intention")) {
            intention.setText(prefs.getString("intention", "Serious"));
        }

        addInput(intention, 65);

        EditText about = new EditText(this);
        about.setHint("About yourself");
        about.setTextSize(17);
        about.setGravity(Gravity.TOP);
        about.setMinLines(4);
        about.setPadding(16, 16, 16, 16);

        if (prefs.contains("about")) {
            about.setText(prefs.getString("about", ""));
        }

        root.addView(
                about,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        140
                )
        );

        addSpace(15);

        TextView prefTitle = title("Partner Preferences", 22);

        root.addView(
                prefTitle,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        TextView prefInfo = bodyText(
                "These preferences help improve your recommended matches."
        );
        prefInfo.setGravity(Gravity.CENTER);

        root.addView(prefInfo);

        addSpace(8);

        EditText minAge = new EditText(this);
        minAge.setHint("Preferred minimum age");
        minAge.setTextSize(17);
        minAge.setInputType(InputType.TYPE_CLASS_NUMBER);

        if (prefs.contains("minAge")) {
            minAge.setText(String.valueOf(prefs.getInt("minAge", 21)));
        } else {
            minAge.setText("21");
        }

        addInput(minAge, 65);

        EditText maxAge = new EditText(this);
        maxAge.setHint("Preferred maximum age");
        maxAge.setTextSize(17);
        maxAge.setInputType(InputType.TYPE_CLASS_NUMBER);

        if (prefs.contains("maxAge")) {
            maxAge.setText(String.valueOf(prefs.getInt("maxAge", 35)));
        } else {
            maxAge.setText("35");
        }

        addInput(maxAge, 65);

        EditText preferredCountry = new EditText(this);
        preferredCountry.setHint("Preferred country (optional)");
        preferredCountry.setTextSize(17);

        if (prefs.contains("preferredCountry")) {
            preferredCountry.setText(
                    prefs.getString("preferredCountry", "")
            );
        }

        addInput(preferredCountry, 65);

        addSpace(15);

        Button save = appButton("Save Profile & Find Matches");

        root.addView(
                save,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        68
                )
        );

        addSpace(12);

        Button back = secondaryButton("Back");

        root.addView(
                back,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        save.setOnClickListener(v -> {

            String n = name.getText().toString().trim();
            String ageText = age.getText().toString().trim();
            String c = country.getText().toString().trim();
            String i = intention.getText().toString().trim();
            String a = about.getText().toString().trim();

            if (n.isEmpty()) {
                name.setError("Please enter your name");
                name.requestFocus();
                return;
            }

            if (ageText.isEmpty()) {
                age.setError("Please enter your age");
                age.requestFocus();
                return;
            }

            int userAge;

            try {
                userAge = Integer.parseInt(ageText);
            } catch (Exception e) {
                age.setError("Enter a valid age");
                age.requestFocus();
                return;
            }

            if (userAge < 18 || userAge > 80) {
                age.setError("Age must be between 18 and 80");
                age.requestFocus();
                return;
            }

            if (c.isEmpty()) {
                country.setError("Please enter your country");
                country.requestFocus();
                return;
            }

            int min;
            int max;

            try {
                min = Integer.parseInt(minAge.getText().toString().trim());
            } catch (Exception e) {
                min = 21;
            }

            try {
                max = Integer.parseInt(maxAge.getText().toString().trim());
            } catch (Exception e) {
                max = 35;
            }

            if (min < 18) {
                min = 18;
            }

            if (max < min) {
                max = min;
            }

            if (i.isEmpty()) {
                i = "Serious";
            }

            prefs.edit()
                    .putString("name", n)
                    .putInt("age", userAge)
                    .putString("country", c)
                    .putString("intention", i)
                    .putString("about", a)
                    .putInt("minAge", min)
                    .putInt("maxAge", max)
                    .putString(
                            "preferredCountry",
                            preferredCountry.getText().toString().trim()
                    )
                    .apply();

            Toast.makeText(
                    MainActivity.this,
                    "Profile saved successfully",
                    Toast.LENGTH_SHORT
            ).show();

            showMatches();
        });

        back.setOnClickListener(v -> showHome());
    }

    // =========================================================
    // MATCH DATA
    // =========================================================

    private static class MatchCandidate {

        String name;
        int age;
        String country;
        String intention;
        String about;
        boolean verified;

        MatchCandidate(
                String name,
                int age,
                String country,
                String intention,
                String about,
                boolean verified
        ) {
            this.name = name;
            this.age = age;
            this.country = country;
            this.intention = intention;
            this.about = about;
            this.verified = verified;
        }
    }

    private List<MatchCandidate> getCandidates() {

        List<MatchCandidate> list = new ArrayList<>();

        list.add(
                new MatchCandidate(
                        "Ayesha",
                        27,
                        "Pakistan",
                        "Serious",
                        "Looking for a respectful and sincere Nikah.",
                        true
                )
        );

        list.add(
                new MatchCandidate(
                        "Fatima",
                        29,
                        "Saudi Arabia",
                        "Serious",
                        "Family-oriented and looking for marriage.",
                        true
                )
        );

        list.add(
                new MatchCandidate(
                        "Maryam",
                        26,
                        "Germany",
                        "Serious",
                        "Seeking a serious Muslim marriage.",
                        true
                )
        );

        list.add(
                new MatchCandidate(
                        "Hafsa",
                        31,
                        "United Kingdom",
                        "Serious",
                        "Interested in a respectful Islamic marriage.",
                        true
                )
        );

        list.add(
                new MatchCandidate(
                        "Zainab",
                        24,
                        "Pakistan",
                        "Serious",
                        "Serious about finding a suitable spouse.",
                        true
                )
        );

        list.add(
                new MatchCandidate(
                        "Sana",
                        28,
                        "UAE",
                        "Serious",
                        "Family values and halal marriage are important.",
                        false
                )
        );

        return list;
    }

    // =========================================================
    // MATCH SCORE
    // =========================================================

    private int calculateScore(MatchCandidate candidate) {

        int userAge = prefs.getInt("age", 0);
        int minAge = prefs.getInt("minAge", 21);
        int maxAge = prefs.getInt("maxAge", 35);

        String userCountry =
                prefs.getString("country", "").trim();

        String preferredCountry =
                prefs.getString("preferredCountry", "").trim();

        String userIntention =
                prefs.getString("intention", "Serious").trim();

        int score = 0;

        // Age preference
        if (candidate.age >= minAge &&
                candidate.age <= maxAge) {

            score += 35;

        } else {

            int difference = 0;

            if (candidate.age < minAge) {
                difference = minAge - candidate.age;
            } else if (candidate.age > maxAge) {
                difference = candidate.age - maxAge;
            }

            if (difference <= 2) {
                score += 25;
            } else if (difference <= 5) {
                score += 15;
            } else {
                score += 5;
            }
        }

        // Country preference
        if (!preferredCountry.isEmpty() &&
                candidate.country.equalsIgnoreCase(preferredCountry)) {

            score += 30;

        } else if (
                candidate.country.equalsIgnoreCase(userCountry)
        ) {

            score += 20;

        } else {

            score += 10;
        }

        // Marriage intention
        if (
                userIntention.equalsIgnoreCase("Serious") &&
                candidate.intention.equalsIgnoreCase("Serious")
        ) {

            score += 25;

        } else {

            score += 10;
        }

        // Verified profile
        if (candidate.verified) {
            score += 10;
        }

        // Age closeness bonus
        if (userAge > 0) {

            int difference =
                    Math.abs(userAge - candidate.age);

            if (difference <= 3) {
                score += 5;
            } else if (difference <= 6) {
                score += 3;
            }
        }

        // Maximum score
        if (score > 100) {
            score = 100;
        }

        return score;
    }

    private String getMatchReasons(MatchCandidate candidate) {

        int minAge = prefs.getInt("minAge", 21);
        int maxAge = prefs.getInt("maxAge", 35);

        String userCountry =
                prefs.getString("country", "").trim();

        String preferredCountry =
                prefs.getString("preferredCountry", "").trim();

        StringBuilder reasons = new StringBuilder();

        if (candidate.age >= minAge &&
                candidate.age <= maxAge) {

            reasons.append("✓ Age matches your preference\n");
        }

        if (!preferredCountry.isEmpty() &&
                candidate.country.equalsIgnoreCase(preferredCountry)) {

            reasons.append("✓ Preferred country\n");

        } else if (
                candidate.country.equalsIgnoreCase(userCountry)
        ) {

            reasons.append("✓ Same country\n");
        }

        if (candidate.intention.equalsIgnoreCase("Serious")) {
            reasons.append("✓ Serious marriage intention\n");
        }

        if (candidate.verified) {
            reasons.append("✓ Verified profile");
        }

        if (reasons.length() == 0) {
            reasons.append("✓ Potential Nikah compatibility");
        }

        return reasons.toString().trim();
    }

    // =========================================================
    // MATCHES SCREEN
    // =========================================================

    private void showMatches() {

        setupRoot();

        root.setGravity(Gravity.TOP);

        root.addView(
                title("Recommended Matches", 28),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        String userName =
                prefs.getString("name", "");

        if (userName.isEmpty()) {

            TextView warning = bodyText(
                    "Please create your profile first so we can recommend better matches."
            );

            warning.setGravity(Gravity.CENTER);

            root.addView(warning);

            addSpace(15);

            Button create = appButton("Create Profile");

            root.addView(
                    create,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            65
                    )
            );

            create.setOnClickListener(v -> showCreateProfile());

            return;
        }

        TextView intro = bodyText(
                "Assalamu Alaikum " +
                        userName +
                        "\n\nHere are profiles recommended using your preferences."
        );

        intro.setGravity(Gravity.CENTER);

        root.addView(intro);

        addSpace(10);

        List<MatchCandidate> candidates =
                getCandidates();

        int shown = 0;

        for (MatchCandidate candidate : candidates) {

            int score = calculateScore(candidate);

            // Only show suitable profiles
            if (score >= 45) {

                addMatchCard(candidate, score);

                shown++;
            }
        }

        if (shown == 0) {

            addSpace(20);

            TextView empty = bodyText(
                    "No strong matches found yet.\n\n" +
                    "Try widening your preferred age range or country."
            );

            empty.setGravity(Gravity.CENTER);

            root.addView(empty);
        }

        addSpace(25);

        Button refresh = secondaryButton(
                "Refresh Recommendations"
        );

        root.addView(
                refresh,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        refresh.setOnClickListener(v -> showMatches());

        addSpace(10);

        Button back = secondaryButton("Back to Home");

        root.addView(
                back,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        back.setOnClickListener(v -> showHome());
    }

    // =========================================================
    // MATCH CARD
    // =========================================================

    private void addMatchCard(
            MatchCandidate candidate,
            int score
    ) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(18, 18, 18, 18);
        card.setBackgroundColor(white);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 16);

        root.addView(card, cardParams);

        // Name
        TextView name = new TextView(this);

        String verified =
                candidate.verified
                        ? "  ✓ Verified"
                        : "";

        name.setText(
                candidate.name +
                        " • " +
                        candidate.age +
                        verified
        );

        name.setTextSize(21);
        name.setTextColor(dark);
        name.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        name.setPadding(5, 5, 5, 10);

        card.addView(name);

        // Country
        TextView country = bodyText(
                "Country: " + candidate.country
        );

        card.addView(country);

        // Intention
        TextView intention = bodyText(
                "Marriage intention: " +
                        candidate.intention
        );

        card.addView(intention);

        // Compatibility
        TextView compatibility = new TextView(this);

        compatibility.setText(
                "Compatibility: " +
                        score +
                        "%"
        );

        compatibility.setTextSize(20);
        compatibility.setTextColor(blue);
        compatibility.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        compatibility.setGravity(Gravity.CENTER);
        compatibility.setPadding(8, 12, 8, 12);

        card.addView(compatibility);

        // Reasons
        TextView reasonsTitle = new TextView(this);

        reasonsTitle.setText("Why this may be a good match");

        reasonsTitle.setTextSize(16);
        reasonsTitle.setTextColor(dark);
        reasonsTitle.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        reasonsTitle.setPadding(8, 8, 8, 4);

        card.addView(reasonsTitle);

        TextView reasons = new TextView(this);

        reasons.setText(
                getMatchReasons(candidate)
        );

        reasons.setTextSize(15);
        reasons.setTextColor(gray);
        reasons.setPadding(8, 4, 8, 12);

        card.addView(reasons);

        // About
        TextView about = bodyText(
                candidate.about
        );

        card.addView(about);

        addSpaceToCard(card, 6);

        // View profile button
        Button viewProfile =
                secondaryButton("View Profile");

        card.addView(
                viewProfile,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        55
                )
        );

        viewProfile.setOnClickListener(
                v -> showProfileDetails(candidate, score)
        );

        addSpaceToCard(card, 8);

        // Interest button
        Button interest =
                appButton("Express Interest");

        card.addView(
                interest,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        58
                )
        );

        interest.setOnClickListener(
                v -> expressInterest(candidate)
        );
    }

    private void addSpaceToCard(
            LinearLayout card,
            int height
    ) {

        Space space = new Space(this);

        card.addView(
                space,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        height
                )
        );
    }

    // =========================================================
    // PROFILE DETAILS
    // =========================================================

    private void showProfileDetails(
            MatchCandidate candidate,
            int score
    ) {

        setupRoot();

        root.setGravity(Gravity.TOP);

        root.addView(
                title(candidate.name, 30),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        TextView verified = bodyText(
                candidate.verified
                        ? "✓ Verified Profile"
                        : "Profile verification pending"
        );

        verified.setGravity(Gravity.CENTER);

        root.addView(verified);

        addSpace(10);

        root.addView(
                bodyText(
                        "Age: " +
                                candidate.age +
                                "\n\nCountry: " +
                                candidate.country +
                                "\n\nMarriage intention: " +
                                candidate.intention +
                                "\n\nCompatibility: " +
                                score +
                                "%"
                )
        );

        addSpace(10);

        TextView about = bodyText(
                "About\n\n" +
                        candidate.about
        );

        root.addView(about);

        addSpace(15);

        root.addView(
                bodyText(
                        "Why this match\n\n" +
                                getMatchReasons(candidate)
                )
        );

        addSpace(20);

        Button interest =
                appButton("Express Interest");

        root.addView(
                interest,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        65
                )
        );

        interest.setOnClickListener(
                v -> expressInterest(candidate)
        );

        addSpace(10);

        Button back =
                secondaryButton("Back to Matches");

        root.addView(
                back,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        back.setOnClickListener(v -> showMatches());
    }

    // =========================================================
    // EXPRESS INTEREST
    // =========================================================

    private void expressInterest(
            MatchCandidate candidate
    ) {

        Set<String> interests =
                new HashSet<>(
                        prefs.getStringSet(
                                "interests",
                                new HashSet<>()
                        )
                );

        if (interests.contains(candidate.name)) {

            Toast.makeText(
                    this,
                    "Interest already sent to " +
                            candidate.name,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        interests.add(candidate.name);

        prefs.edit()
                .putStringSet("interests", interests)
                .apply();

        Toast.makeText(
                this,
                "Interest sent safely to " +
                        candidate.name +
                        ". Wait for mutual acceptance.",
                Toast.LENGTH_LONG
        ).show();
    }
                         }
