package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private LinearLayout root;

    private final int BLUE = Color.rgb(43, 107, 87);
    private final int DARK = Color.rgb(24, 50, 44);
    private final int GRAY = Color.rgb(100, 117, 111);
    private final int LIGHT = Color.rgb(247, 250, 249);
    private final int WHITE = Color.WHITE;
    private final int GOLD = Color.rgb(190, 145, 45);

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
        t.setTextColor(DARK);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 12, 12, 12);

        return t;
    }

    private TextView subtitle(String text) {
        TextView t = new TextView(this);

        t.setText(text);
        t.setTextSize(17);
        t.setTextColor(GRAY);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 8, 12, 20);

        return t;
    }

    private Button appButton(String text) {
        Button b = new Button(this);

        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(WHITE);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(60);
        b.setPadding(16, 8, 16, 8);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(BLUE);
        bg.setCornerRadius(18);

        b.setBackground(bg);

        return b;
    }

    private TextView smallText(String text) {
        TextView t = new TextView(this);

        t.setText(text);
        t.setTextSize(15);
        t.setTextColor(GRAY);
        t.setPadding(12, 8, 12, 8);

        return t;
    }

    private TextView matchText(String text) {
        TextView t = new TextView(this);

        t.setText(text);
        t.setTextSize(17);
        t.setTextColor(DARK);
        t.setPadding(18, 8, 18, 8);

        return t;
    }

    private void setupRoot() {

        ScrollView scroll = new ScrollView(this);

        scroll.setFillViewport(true);
        scroll.setBackgroundColor(LIGHT);

        root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(24, 30, 24, 30);

        scroll.addView(
                root,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(scroll);
    }

    private void addSpace(int height) {

        View space = new View(this);

        root.addView(
                space,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        height
                )
        );
    }

    private void addButton(Button button) {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        64
                );

        params.setMargins(0, 8, 0, 8);

        root.addView(button, params);
    }

    private void addInput(EditText input) {

        input.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        65
                );

        params.setMargins(0, 0, 0, 8);

        root.addView(input, params);
    }

    private Spinner createSpinner(String[] items) {

        Spinner spinner = new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        items
                );

        spinner.setAdapter(adapter);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        60
                );

        params.setMargins(0, 0, 0, 8);

        root.addView(spinner, params);

        return spinner;
    }

    // =========================================================
    // HOME
    // =========================================================

    private void showHome() {

        setupRoot();

        addSpace(25);

        root.addView(
                title("Best Nikah Bridge", 32),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                subtitle("Trusted • Simple • Safe"),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        addSpace(15);

        Button createProfile =
                appButton("Create / Update Profile");

        Button viewMatches =
                appButton("View Recommended Matches");

        addButton(createProfile);

        addButton(viewMatches);

        addSpace(30);

        TextView info =
                new TextView(this);

        info.setText(
                "A serious Muslim matrimonial platform\n" +
                "for safe and respectful Nikah.\n\n" +
                "Your profile is used to calculate better matches."
        );

        info.setTextSize(16);
        info.setTextColor(GRAY);
        info.setGravity(Gravity.CENTER);
        info.setPadding(10, 10, 10, 10);

        root.addView(
                info,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        createProfile.setOnClickListener(
                v -> showCreateProfile()
        );

        viewMatches.setOnClickListener(
                v -> showMatches()
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
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                subtitle("Complete your profile for better matches"),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        EditText name = new EditText(this);
        name.setHint("Full name");
        name.setTextSize(17);

        if (prefs.contains("name")) {
            name.setText(prefs.getString("name", ""));
        }

        addInput(name);

        EditText age = new EditText(this);
        age.setHint("Age");
        age.setTextSize(17);
        age.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER
        );

        if (prefs.contains("age")) {
            age.setText(prefs.getString("age", ""));
        }

        addInput(age);

        EditText country = new EditText(this);
        country.setHint("Country");
        country.setTextSize(17);

        if (prefs.contains("country")) {
            country.setText(
                    prefs.getString("country", "")
            );
        }

        addInput(country);

        TextView genderLabel =
                smallText("Gender");

        root.addView(genderLabel);

        String[] genders = {
                "Select gender",
                "Male",
                "Female"
        };

        Spinner gender =
                createSpinner(genders);

        String savedGender =
                prefs.getString("gender", "");

        setSpinnerValue(gender, genders, savedGender);

        TextView seekingLabel =
                smallText("Looking for");

        root.addView(seekingLabel);

        String[] seekingOptions = {
                "Select",
                "Male",
                "Female"
        };

        Spinner seeking =
                createSpinner(seekingOptions);

        String savedSeeking =
                prefs.getString("seeking", "");

        setSpinnerValue(
                seeking,
                seekingOptions,
                savedSeeking
        );

        TextView intentionLabel =
                smallText("Marriage intention");

        root.addView(intentionLabel);

        String[] intentions = {
                "Serious / Ready for Nikah",
                "Serious / Soon",
                "Exploring with marriage intention"
        };

        Spinner intention =
                createSpinner(intentions);

        String savedIntention =
                prefs.getString(
                        "intention",
                        "Serious / Ready for Nikah"
                );

        setSpinnerValue(
                intention,
                intentions,
                savedIntention
        );

        EditText about = new EditText(this);

        about.setHint("About yourself");
        about.setTextSize(17);
        about.setGravity(Gravity.TOP);
        about.setMinLines(4);
        about.setPadding(16, 16, 16, 16);

        if (prefs.contains("about")) {
            about.setText(
                    prefs.getString("about", "")
            );
        }

        root.addView(
                about,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        130
                )
        );

        addSpace(15);

        Button save =
                appButton("Save Profile");

        Button back =
                appButton("Back");

        addButton(save);

        addButton(back);

        save.setOnClickListener(v -> {

            String n =
                    name.getText().toString().trim();

            String a =
                    age.getText().toString().trim();

            String c =
                    country.getText().toString().trim();

            String g =
                    gender.getSelectedItem().toString();

            String s =
                    seeking.getSelectedItem().toString();

            String i =
                    intention.getSelectedItem().toString();

            String ab =
                    about.getText().toString().trim();

            if (n.isEmpty()) {

                name.setError(
                        "Please enter your name"
                );

                name.requestFocus();

                return;
            }

            if (a.isEmpty()) {

                age.setError(
                        "Please enter your age"
                );

                age.requestFocus();

                return;
            }

            int ageNumber;

            try {

                ageNumber =
                        Integer.parseInt(a);

            } catch (Exception e) {

                age.setError(
                        "Please enter a valid age"
                );

                return;
            }

            if (ageNumber < 18 ||
                    ageNumber > 80) {

                age.setError(
                        "Age must be between 18 and 80"
                );

                return;
            }

            if (c.isEmpty()) {

                country.setError(
                        "Please enter your country"
                );

                country.requestFocus();

                return;
            }

            if (g.equals("Select gender")) {

                Toast.makeText(
                        MainActivity.this,
                        "Please select your gender",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (s.equals("Select")) {

                Toast.makeText(
                        MainActivity.this,
                        "Please select who you are looking for",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            prefs.edit()
                    .putString("name", n)
                    .putString("age", a)
                    .putString("country", c)
                    .putString("gender", g)
                    .putString("seeking", s)
                    .putString("intention", i)
                    .putString("about", ab)
                    .putBoolean("profile_complete", true)
                    .apply();

            Toast.makeText(
                    MainActivity.this,
                    "Profile saved successfully",
                    Toast.LENGTH_LONG
            ).show();

            showHome();
        });

        back.setOnClickListener(
                v -> showHome()
        );
    }

    private void setSpinnerValue(
            Spinner spinner,
            String[] values,
            String value
    ) {

        if (value == null || value.isEmpty()) {
            return;
        }

        for (int x = 0; x < values.length; x++) {

            if (values[x].equalsIgnoreCase(value)) {

                spinner.setSelection(x);

                return;
            }
        }
    }

    // =========================================================
    // MATCH MODEL
    // =========================================================

    private static class Match {

        String name;
        int age;
        String country;
        String gender;
        String intention;
        String about;

        Match(
                String name,
                int age,
                String country,
                String gender,
                String intention,
                String about
        ) {

            this.name = name;
            this.age = age;
            this.country = country;
            this.gender = gender;
            this.intention = intention;
            this.about = about;
        }
    }

    // =========================================================
    // MATCHES
    // =========================================================

    private void showMatches() {

        if (!prefs.getBoolean(
                "profile_complete",
                false
        )) {

            Toast.makeText(
                    this,
                    "Please create your profile first",
                    Toast.LENGTH_LONG
            ).show();

            showCreateProfile();

            return;
        }

        setupRoot();

        root.setGravity(Gravity.TOP);

        root.addView(
                title("Recommended Matches", 28),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        String userName =
                prefs.getString("name", "");

        String userCountry =
                prefs.getString("country", "");

        String userGender =
                prefs.getString("gender", "");

        String userSeeking =
                prefs.getString("seeking", "");

        String userIntention =
                prefs.getString(
                        "intention",
                        ""
                );

        int userAge =
                getSavedAge();

        TextView intro =
                new TextView(this);

        intro.setText(
                "Matches selected for " +
                userName +
                "\nBased on age, country and Nikah intention."
        );

        intro.setTextSize(16);
        intro.setTextColor(GRAY);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(10, 8, 10, 18);

        root.addView(
                intro,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // -----------------------------------------------------
        // SAMPLE PROFILES
        // -----------------------------------------------------

        List<Match> candidates =
                new ArrayList<>();

        candidates.add(
                new Match(
                        "Ayesha",
                        27,
                        "Pakistan",
                        "Female",
                        "Serious / Ready for Nikah",
                        "Family-oriented, respectful and serious about Nikah."
                )
        );

        candidates.add(
                new Match(
                        "Fatima",
                        29,
                        "Saudi Arabia",
                        "Female",
                        "Serious / Soon",
                        "Values faith, family and a peaceful married life."
                )
        );

        candidates.add(
                new Match(
                        "Maryam",
                        26,
                        "Germany",
                        "Female",
                        "Serious / Ready for Nikah",
                        "Simple, respectful and looking for a serious Muslim marriage."
                )
        );

        candidates.add(
                new Match(
                        "Zainab",
                        31,
                        "United Kingdom",
                        "Female",
                        "Serious / Soon",
                        "Family values, honesty and long-term commitment."
                )
        );

        candidates.add(
                new Match(
                        "Hira",
                        25,
                        "Pakistan",
                        "Female",
                        "Serious / Ready for Nikah",
                        "Interested in building a respectful and halal family life."
                )
        );

        candidates.add(
                new Match(
                        "Sana",
                        28,
                        "United Arab Emirates",
                        "Female",
                        "Serious / Soon",
                        "Looking for compatibility, respect and serious commitment."
                )
        );

        // -----------------------------------------------------
        // FILTER + SORT BY COMPATIBILITY
        // -----------------------------------------------------

        List<MatchScore> results =
                new ArrayList<>();

        for (Match candidate : candidates) {

            // If user selected a gender preference,
            // only show that gender.
            if (!userSeeking.isEmpty()
                    && !userSeeking.equals("Select")
                    && !candidate.gender.equalsIgnoreCase(userSeeking)) {

                continue;
            }

            int score =
                    calculateCompatibility(
                            userAge,
                            userCountry,
                            userIntention,
                            candidate
                    );

            results.add(
                    new MatchScore(
                            candidate,
                            score
                    )
            );
        }

        // Sort highest score first
        for (int i = 0; i < results.size(); i++) {

            for (int j = i + 1; j < results.size(); j++) {

                if (results.get(j).score >
                        results.get(i).score) {

                    MatchScore temp =
                            results.get(i);

                    results.set(
                            i,
                            results.get(j)
                    );

                    results.set(
                            j,
                            temp
                    );
                }
            }
        }

        // -----------------------------------------------------
        // SHOW RESULTS
        // -----------------------------------------------------

        if (results.isEmpty()) {

            TextView empty =
                    matchText(
                            "No matches found with your current preferences.\n\n" +
                            "Try updating your profile or Looking For preference."
                    );

            empty.setGravity(Gravity.CENTER);

            root.addView(
                    empty,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

        } else {

            for (MatchScore result : results) {

                addMatchCard(result);
            }
        }

        addSpace(20);

        Button update =
                appButton("Update Profile");

        Button back =
                appButton("Back");

        addButton(update);

        addButton(back);

        update.setOnClickListener(
                v -> showCreateProfile()
        );

        back.setOnClickListener(
                v -> showHome()
        );
    }

    // =========================================================
    // MATCH SCORE
    // =========================================================

    private static class MatchScore {

        Match match;
        int score;

        MatchScore(
                Match match,
                int score
        ) {

            this.match = match;
            this.score = score;
        }
    }

    private int calculateCompatibility(
            int userAge,
            String userCountry,
            String userIntention,
            Match candidate
    ) {

        int score = 50;

        // -----------------------------------------------------
        // AGE COMPATIBILITY
        // -----------------------------------------------------

        int ageDifference =
                Math.abs(
                        userAge -
                        candidate.age
                );

        if (ageDifference == 0) {

            score += 20;

        } else if (ageDifference <= 2) {

            score += 18;

        } else if (ageDifference <= 4) {

            score += 14;

        } else if (ageDifference <= 6) {

            score += 10;

        } else if (ageDifference <= 9) {

            score += 5;

        } else {

            score -= 5;
        }

        // -----------------------------------------------------
        // COUNTRY COMPATIBILITY
        // -----------------------------------------------------

        if (!userCountry.isEmpty()
                && userCountry.equalsIgnoreCase(
                        candidate.country
                )) {

            score += 15;

        } else if (
                candidate.country.equalsIgnoreCase(
                        "Saudi Arabia"
                )
                || candidate.country.equalsIgnoreCase(
                        "Pakistan"
                )
                || candidate.country.equalsIgnoreCase(
                        "United Arab Emirates"
                )
        ) {

            score += 7;
        }

        // -----------------------------------------------------
        // MARRIAGE INTENTION
        // -----------------------------------------------------

        if (!userIntention.isEmpty()
                && candidate.intention.equalsIgnoreCase(
                        userIntention
                )) {

            score += 15;

        } else if (
                candidate.intention.contains("Serious")
        ) {

            score += 8;
        }

        // Keep score between 1 and 99
        if (score > 99) {
            score = 99;
        }

        if (score < 1) {
            score = 1;
        }

        return score;
    }

    private int getSavedAge() {

        try {

            return Integer.parseInt(
                    prefs.getString("age", "25")
            );

        } catch (Exception e) {

            return 25;
        }
    }

    // =========================================================
    // MATCH CARD
    // =========================================================

    private void addMatchCard(
            MatchScore result
    ) {

        Match match =
                result.match;

        int score =
                result.score;

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.VERTICAL
        );

        card.setPadding(
                18,
                18,
                18,
                18
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.WHITE
        );

        background.setCornerRadius(22);

        background.setStroke(
                2,
                Color.rgb(225, 233, 229)
        );

        card.setBackground(background);

        TextView name =
                new TextView(this);

        name.setText(
                match.name +
                " • " +
                match.age
        );

        name.setTextSize(22);
        name.setTextColor(DARK);
        name.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        name.setGravity(Gravity.CENTER);

        card.addView(
                name,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView compatibility =
                new TextView(this);

        compatibility.setText(
                "Compatibility: " +
                score +
                "%"
        );

        compatibility.setTextSize(19);
        compatibility.setTextColor(
                score >= 80
                        ? BLUE
                        : GOLD
        );

        compatibility.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        compatibility.setGravity(
                Gravity.CENTER
        );

        compatibility.setPadding(
                8,
                8,
                8,
                8
        );

        card.addView(compatibility);

        TextView details =
                new TextView(this);

        details.setText(
                "📍 " +
                match.country +
                "\n" +
                "💍 " +
                match.intention +
                "\n\n" +
                match.about
        );

        details.setTextSize(16);
        details.setTextColor(GRAY);
        details.setPadding(
                8,
                8,
                8,
                8
        );

        card.addView(details);

        TextView reasons =
                new TextView(this);

        reasons.setText(
                buildMatchReasons(match)
        );

        reasons.setTextSize(15);
        reasons.setTextColor(DARK);
        reasons.setPadding(
                8,
                8,
                8,
                8
        );

        card.addView(reasons);

        Button viewProfile =
                appButton("View Profile");

        Button interest =
                appButton(
                        "Express Interest"
                );

        card.addView(
                viewProfile,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        card.addView(
                interest,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        60
                )
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                0,
                0,
                18
        );

        root.addView(
                card,
                cardParams
        );

        viewProfile.setOnClickListener(
                v -> showFullProfile(match, score)
        );

        updateInterestButton(
                interest,
                match.name
        );

        interest.setOnClickListener(v -> {

            expressInterest(
                    match.name
            );

            updateInterestButton(
                    interest,
                    match.name
            );
        });
    }

    // =========================================================
    // MATCH REASONS
    // =========================================================

    private String buildMatchReasons(
            Match candidate
    ) {

        int userAge =
                getSavedAge();

        String userCountry =
                prefs.getString(
                        "country",
                        ""
                );

        String userIntention =
                prefs.getString(
                        "intention",
                        ""
                );

        List<String> reasons =
                new ArrayList<>();

        int ageDifference =
                Math.abs(
                        userAge -
                        candidate.age
                );

        if (ageDifference <= 4) {

            reasons.add(
                    "✓ Similar age range"
            );
        }

        if (!userCountry.isEmpty()
                && userCountry.equalsIgnoreCase(
                        candidate.country
                )) {

            reasons.add(
                    "✓ Same country"
            );
        }

        if (!userIntention.isEmpty()
                && candidate.intention.equalsIgnoreCase(
                        userIntention
                )) {

            reasons.add(
                    "✓ Same marriage intention"
            );
        }

        if (reasons.isEmpty()) {

            reasons.add(
                    "✓ Serious Nikah profile"
            );
        }

        StringBuilder result =
                new StringBuilder();

        result.append(
                "Why this match:\n"
        );

        for (String reason : reasons) {

            result.append(reason)
                    .append("\n");
        }

        return result.toString();
    }

    // =========================================================
    // FULL PROFILE
    // =========================================================

    private void showFullProfile(
            Match match,
            int score
    ) {

        setupRoot();

        root.setGravity(Gravity.TOP);

        root.addView(
                title(
                        match.name +
                        " • " +
                        match.age,
                        28
                ),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView verified =
                new TextView(this);

        verified.setText(
                "✓ Verified-style demo profile"
        );

        verified.setTextSize(16);
        verified.setTextColor(BLUE);
        verified.setGravity(Gravity.CENTER);
        verified.setPadding(
                8,
                8,
                8,
                12
        );

        root.addView(
                verified,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        TextView profile =
                new TextView(this);

        profile.setText(
                "Compatibility: " +
                score +
                "%\n\n" +

                "Country: " +
                match.country +
                "\n\n" +

                "Marriage intention:\n" +
                match.intention +
                "\n\n" +

                "About:\n" +
                match.about +
                "\n\n" +

                buildMatchReasons(match)
        );

        profile.setTextSize(17);
        profile.setTextColor(DARK);
        profile.setPadding(
                12,
                12,
                12,
                20
        );

        root.addView(
                profile,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        Button interest =
                appButton(
                        "Express Interest"
                );

        Button back =
                appButton("Back to Matches");

        addButton(interest);

        addButton(back);

        updateInterestButton(
                interest,
                match.name
        );

        interest.setOnClickListener(v -> {

            expressInterest(
                    match.name
            );

            updateInterestButton(
                    interest,
                    match.name
            );
        });

        back.setOnClickListener(
                v -> showMatches()
        );
    }

    // =========================================================
    // EXPRESS INTEREST
    // =========================================================

    private void expressInterest(
            String name
    ) {

        Set<String> interests =
                new HashSet<>(
                        prefs.getStringSet(
                                "interests",
                                new HashSet<>()
                        )
                );

        if (interests.contains(name)) {

            Toast.makeText(
                    this,
                    "Interest already sent to " +
                    name,
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        interests.add(name);

        prefs.edit()
                .putStringSet(
                        "interests",
                        interests
                )
                .apply();

        Toast.makeText(
                this,
                "Interest sent successfully to " +
                name,
                Toast.LENGTH_LONG
        ).show();
    }

    private boolean hasInterest(
            String name
    ) {

        Set<String> interests =
                prefs.getStringSet(
                        "interests",
                        new HashSet<>()
                );

        return interests.contains(name);
    }

    private void updateInterestButton(
            Button button,
            String name
    ) {

        if (hasInterest(name)) {

            button.setText(
                    "✓ Interest Sent"
            );

            button.setEnabled(false);

            button.setAlpha(0.65f);

        } else {

            button.setText(
                    "Express Interest"
            );

            button.setEnabled(true);

            button.setAlpha(1.0f);
        }
    }
}
