package com.nikahbridge;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private LinearLayout root;
    private final int green = Color.rgb(24, 107, 87);
    private final int dark = Color.rgb(24, 50, 44);
    private final int gray = Color.rgb(100, 117, 111);
    private final int light = Color.rgb(247, 250, 249);
    private final int white = Color.WHITE;
    private final Set<String> sentInterests = new HashSet<>();
    private final Set<String> blockedProfiles = new HashSet<>();
    private SharedPreferences prefs;

    private static final String PREFS_NAME = "best_nikah_bridge";
    private static final String KEY_PROFILE_SAVED = "profile_saved";
    private static final String KEY_NAME = "profile_name";
    private static final String KEY_AGE = "profile_age";
    private static final String KEY_COUNTRY = "profile_country";
    private static final String KEY_CITY = "profile_city";
    private static final String KEY_GENDER = "profile_gender";
    private static final String KEY_MARITAL = "profile_marital";
    private static final String KEY_INTENTION = "profile_intention";
    private static final String KEY_PRACTICE = "profile_practice";
    private static final String KEY_ABOUT = "profile_about";
    private static final String KEY_PREFERENCE = "profile_preference";
    private static final String KEY_LANGUAGE = "language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        showHome();
    }

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
        t.setTextSize(16);
        t.setTextColor(gray);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 8, 12, 20);
        return t;
    }

    private Button appButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(white);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(58);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(green);
        bg.setCornerRadius(18);
        b.setBackground(bg);
        return b;
    }

    private Button outlineButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTextColor(green);
        b.setMinHeight(54);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(white);
        bg.setStroke(2, green);
        bg.setCornerRadius(18);
        b.setBackground(bg);
        return b;
    }

    private void setupRoot() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(light);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(22, 28, 22, 28);

        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT
        ));
        setContentView(scroll);
    }

    private void addSpace(int height) {
        View space = new View(this);
        root.addView(space, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height
        ));
    }

    private void addFullButton(Button button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 62
        );
        params.setMargins(0, 8, 0, 8);
        root.addView(button, params);
    }

    private TextView section(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(18);
        t.setTextColor(dark);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(4, 18, 4, 6);
        root.addView(t);
        return t;
    }

    private void addInput(EditText input, int height) {
        input.setTextSize(16);
        input.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height
        );
        params.setMargins(0, 0, 0, 8);
        root.addView(input, params);
    }

    private Spinner addSpinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        root.addView(spinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 58
        ));
        return spinner;
    }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) return i;
        }
        return 0;
    }

    private void showHome() {
        setupRoot();
        addSpace(14);
        root.addView(title("Best Nikah Bridge", 32));
        root.addView(subtitle("Trusted • Simple • Safe • Serious Nikah"));

        if (prefs.getBoolean(KEY_PROFILE_SAVED, false)) {
            TextView status = new TextView(this);
            status.setText("✓ Profile saved • " +
                    prefs.getString(KEY_NAME, "") +
                    " • " + profileCompletion() + "% complete");
            status.setTextSize(15);
            status.setTextColor(green);
            status.setGravity(Gravity.CENTER);
            status.setPadding(8, 8, 8, 14);
            root.addView(status);
        }

        Button profile = appButton("Create / Edit Profile");
        Button matches = appButton("Recommended Matches");
        Button interests = appButton("My Interests");
        Button safety = outlineButton("Safety & Privacy");
        Button help = outlineButton("Help & Nikah Guidance");
        Button language = outlineButton(
                "Urdu".equals(prefs.getString(KEY_LANGUAGE, "English"))
                        ? "Language: Urdu" : "Language: English"
        );

        addFullButton(profile);
        addFullButton(matches);
        addFullButton(interests);
        addFullButton(safety);
        addFullButton(help);
        addFullButton(language);

        profile.setOnClickListener(v -> showCreateProfile());
        matches.setOnClickListener(v -> showMatches());
        interests.setOnClickListener(v -> showInterests());
        safety.setOnClickListener(v -> showSafety());
        help.setOnClickListener(v -> showHelp());
        language.setOnClickListener(v -> {
            String current = prefs.getString(KEY_LANGUAGE, "English");
            prefs.edit().putString(KEY_LANGUAGE,
                    "English".equals(current) ? "Urdu" : "English").apply();
            showHome();
        });

        addSpace(18);
        root.addView(subtitle(
                "A serious Muslim matrimonial platform for respectful Nikah connections.\n" +
                "No dating • Privacy first • Mutual connection before safe communication"
        ));
    }

    private int profileCompletion() {
        String[] keys = {KEY_NAME, KEY_AGE, KEY_COUNTRY, KEY_CITY, KEY_GENDER,
                KEY_MARITAL, KEY_INTENTION, KEY_PRACTICE, KEY_ABOUT, KEY_PREFERENCE};
        int done = 0;
        for (String key : keys) {
            if (!prefs.getString(key, "").trim().isEmpty()) done++;
        }
        return done * 10;
    }

    private void showCreateProfile() {
        setupRoot();
        root.setGravity(Gravity.TOP);
        root.addView(title("Create Your Nikah Profile", 28));
        root.addView(subtitle("Complete your profile to improve trust and matching."));

        EditText name = new EditText(this);
        name.setHint("Full name");
        addInput(name, 60);

        EditText age = new EditText(this);
        age.setHint("Age (18+)");
        age.setInputType(InputType.TYPE_CLASS_NUMBER);
        addInput(age, 60);

        section("Basic Information");

        String[] genders = {"Select gender", "Male", "Female"};
        Spinner gender = addSpinner(genders);

        String[] marital = {"Select marital status", "Never married", "Divorced", "Widowed"};
        Spinner maritalStatus = addSpinner(marital);

        EditText country = new EditText(this);
        country.setHint("Country");
        addInput(country, 60);

        EditText city = new EditText(this);
        city.setHint("City");
        addInput(city, 60);

        section("Nikah & Values");

        String[] intentions = {
                "Select marriage intention", "Serious Nikah", "Ready for marriage", "Exploring marriage"
        };
        Spinner intention = addSpinner(intentions);

        String[] practice = {
                "Select religious practice", "Practicing Muslim", "Moderately practicing", "Prefer not to say"
        };
        Spinner practiceSpinner = addSpinner(practice);

        EditText about = new EditText(this);
        about.setHint("About yourself, family values and what you are looking for");
        about.setGravity(Gravity.TOP);
        about.setMinLines(4);
        about.setPadding(16, 16, 16, 16);
        root.addView(about, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140
        ));

        section("Partner Preferences");

        EditText preference = new EditText(this);
        preference.setHint("Preferred age, country/city, values and other important preferences");
        preference.setGravity(Gravity.TOP);
        preference.setMinLines(3);
        preference.setPadding(16, 16, 16, 16);
        root.addView(preference, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 120
        ));

        section("Verification");

        TextView verify = new TextView(this);
        verify.setText("✓ Verification can be requested after your profile is complete.\n" +
                "For safety, never share passwords, OTPs or private documents in chat.");
        verify.setTextSize(15);
        verify.setTextColor(gray);
        verify.setPadding(4, 4, 4, 12);
        root.addView(verify);

        if (prefs.getBoolean(KEY_PROFILE_SAVED, false)) {
            name.setText(prefs.getString(KEY_NAME, ""));
            age.setText(prefs.getString(KEY_AGE, ""));
            country.setText(prefs.getString(KEY_COUNTRY, ""));
            city.setText(prefs.getString(KEY_CITY, ""));
            about.setText(prefs.getString(KEY_ABOUT, ""));
            preference.setText(prefs.getString(KEY_PREFERENCE, ""));
            gender.setSelection(indexOf(genders, prefs.getString(KEY_GENDER, "")));
            maritalStatus.setSelection(indexOf(marital, prefs.getString(KEY_MARITAL, "")));
            intention.setSelection(indexOf(intentions, prefs.getString(KEY_INTENTION, "")));
            practiceSpinner.setSelection(indexOf(practice, prefs.getString(KEY_PRACTICE, "")));
        }

        Button save = appButton("Save Profile");
        Button back = outlineButton("Back");
        addFullButton(save);
        addFullButton(back);

        save.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String a = age.getText().toString().trim();
            String c = country.getText().toString().trim();

            if (n.isEmpty()) {
                name.setError("Enter your name");
                name.requestFocus();
                return;
            }

            int numericAge;
            try {
                numericAge = Integer.parseInt(a);
            } catch (Exception e) {
                age.setError("Enter a valid age");
                age.requestFocus();
                return;
            }

            if (numericAge < 18 || numericAge > 100) {
                age.setError("Age must be between 18 and 100");
                age.requestFocus();
                return;
            }

            if (c.isEmpty()) {
                country.setError("Enter your country");
                country.requestFocus();
                return;
            }

            prefs.edit()
                    .putBoolean(KEY_PROFILE_SAVED, true)
                    .putString(KEY_NAME, n)
                    .putString(KEY_AGE, a)
                    .putString(KEY_COUNTRY, c)
                    .putString(KEY_CITY, city.getText().toString().trim())
                    .putString(KEY_GENDER, gender.getSelectedItem().toString())
                    .putString(KEY_MARITAL, maritalStatus.getSelectedItem().toString())
                    .putString(KEY_INTENTION, intention.getSelectedItem().toString())
                    .putString(KEY_PRACTICE, practiceSpinner.getSelectedItem().toString())
                    .putString(KEY_ABOUT, about.getText().toString().trim())
                    .putString(KEY_PREFERENCE, preference.getText().toString().trim())
                    .apply();

            Toast.makeText(this,
                    "Profile saved • " + profileCompletion() + "% complete",
                    Toast.LENGTH_LONG).show();
            showHome();
        });

        back.setOnClickListener(v -> showHome());
    }

    private void showMatches() {
        setupRoot();
        root.setGravity(Gravity.TOP);
        root.addView(title("Recommended Matches", 28));
        root.addView(subtitle(
                "Review serious Nikah profiles and connect respectfully.\n" +
                "Compatibility is an example score in this local build."
        ));

        addMatchCard("Ayesha", "27", "Pakistan", "92%",
                "Serious Nikah • Family-oriented • Verified",
                "Values family, respect and a serious path toward Nikah.");

        addMatchCard("Fatima", "29", "Saudi Arabia", "89%",
                "Serious Nikah • Respectful • Verified",
                "Looking for a sincere, responsible and respectful spouse.");

        addMatchCard("Maryam", "26", "Germany", "86%",
                "Serious Nikah • Educated • Verified",
                "Interested in a serious Muslim marriage with mutual respect.");

        Button back = outlineButton("Back");
        addFullButton(back);
        back.setOnClickListener(v -> showHome());
    }

    private void addMatchCard(String name, String age, String country,
                              String compatibility, String reasons, String about) {
        if (blockedProfiles.contains(name)) return;

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 18, 20, 18);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(white);
        bg.setCornerRadius(22);
        bg.setStroke(1, Color.rgb(220, 230, 226));
        card.setBackground(bg);

        TextView person = new TextView(this);
        person.setText(name + " • " + age + "  ✓ Verified");
        person.setTextSize(21);
        person.setTextColor(dark);
        person.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView location = new TextView(this);
        location.setText(country);
        location.setTextSize(16);
        location.setTextColor(gray);

        TextView score = new TextView(this);
        score.setText("Compatibility: " + compatibility);
        score.setTextSize(18);
        score.setTextColor(green);
        score.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        score.setPadding(0, 8, 0, 4);

        TextView reason = new TextView(this);
        reason.setText("✓ " + reasons);
        reason.setTextSize(15);
        reason.setTextColor(dark);

        TextView aboutView = new TextView(this);
        aboutView.setText(about);
        aboutView.setTextSize(15);
        aboutView.setTextColor(gray);
        aboutView.setPadding(0, 6, 0, 10);

        Button view = outlineButton("View Profile");
        String key = name + "|" + age + "|" + country;
        Button interest = appButton(sentInterests.contains(key)
                ? "Interest Sent ✓" : "Express Interest");
        Button report = outlineButton("Report / Block");

        if (sentInterests.contains(key)) {
            interest.setEnabled(false);
            interest.setAlpha(0.65f);
        }

        card.addView(person);
        card.addView(location);
        card.addView(score);
        card.addView(reason);
        card.addView(aboutView);

        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 56);
        bp.setMargins(0, 5, 0, 5);
        card.addView(view, bp);
        card.addView(interest, bp);
        card.addView(report, bp);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, 16);
        root.addView(card, cp);

        view.setOnClickListener(v ->
                showProfileDetails(name, age, country, compatibility, reasons, about));

        interest.setOnClickListener(v -> {
            if (sentInterests.contains(key)) return;
            sentInterests.add(key);
            Toast.makeText(this,
                    "Interest sent to " + name + ". Mutual acceptance is required before safe chat.",
                    Toast.LENGTH_LONG).show();
            showMatches();
        });

        report.setOnClickListener(v -> {
            blockedProfiles.add(name);
            Toast.makeText(this,
                    name + " has been blocked locally. You can report profiles to admin in the production backend.",
                    Toast.LENGTH_LONG).show();
            showMatches();
        });
    }

    private void showProfileDetails(String name, String age, String country,
                                    String compatibility, String reasons, String about) {
        setupRoot();
        root.setGravity(Gravity.TOP);
        root.addView(title(name + " • " + age + " ✓ Verified", 28));

        TextView profile = new TextView(this);
        profile.setText(
                "Country: " + country + "\n\n" +
                "Compatibility: " + compatibility + "\n\n" +
                "Why this match:\n✓ " + reasons + "\n\n" +
                "About:\n" + about + "\n\n" +
                "Safety: Contact details stay private until a mutual connection."
        );
        profile.setTextSize(17);
        profile.setTextColor(dark);
        profile.setPadding(8, 12, 8, 20);
        root.addView(profile);

        String key = name + "|" + age + "|" + country;
        Button interest = appButton(sentInterests.contains(key)
                ? "Interest Sent ✓" : "Express Interest");
        Button chat = outlineButton("Safe Chat (Mutual Only)");
        Button report = outlineButton("Report / Block");
        Button back = outlineButton("Back to Matches");

        addFullButton(interest);
        addFullButton(chat);
        addFullButton(report);
        addFullButton(back);

        if (sentInterests.contains(key)) {
            interest.setEnabled(false);
            interest.setAlpha(0.65f);
        }

        interest.setOnClickListener(v -> {
            sentInterests.add(key);
            Toast.makeText(this,
                    "Interest sent. Chat opens only after mutual acceptance.",
                    Toast.LENGTH_LONG).show();
            showProfileDetails(name, age, country, compatibility, reasons, about);
        });

        chat.setOnClickListener(v -> Toast.makeText(this,
                "Safe Chat is locked until both people accept the connection.",
                Toast.LENGTH_LONG).show());

        report.setOnClickListener(v -> {
            blockedProfiles.add(name);
            Toast.makeText(this,
                    name + " blocked locally.",
                    Toast.LENGTH_SHORT).show();
            showMatches();
        });

        back.setOnClickListener(v -> showMatches());
    }

    private void showInterests() {
        setupRoot();
        root.setGravity(Gravity.TOP);
        root.addView(title("My Interests", 28));

        if (sentInterests.isEmpty()) {
            root.addView(subtitle(
                    "No interests sent yet.\nYour connections should remain respectful and mutual."
            ));
        } else {
            for (String key : sentInterests) {
                TextView item = new TextView(this);
                item.setText("✓ Interest sent: " + key.replace("|", " • "));
                item.setTextSize(16);
                item.setTextColor(dark);
                item.setPadding(8, 12, 8, 12);
                root.addView(item);
            }
        }

        Button back = outlineButton("Back");
        addFullButton(back);
        back.setOnClickListener(v -> showHome());
    }

    private void showSafety() {
        setupRoot();
        root.setGravity(Gravity.TOP);
        root.addView(title("Safety & Privacy", 28));
        root.addView(subtitle(
                "Best Nikah Bridge is designed for serious matrimonial connections."
        ));

        TextView safety = new TextView(this);
        safety.setText(
                "✓ Never share your password or OTP.\n\n" +
                "✓ Keep phone number and contact details private.\n\n" +
                "✓ Use Express Interest before communication.\n\n" +
                "✓ Safe Chat should be available only after mutual acceptance.\n\n" +
                "✓ Report or block suspicious or disrespectful profiles.\n\n" +
                "✓ Verification should be handled securely by the platform/admin.\n\n" +
                "✓ Real verification, authentication, backend security and moderation require server-side implementation."
        );
        safety.setTextSize(16);
        safety.setTextColor(dark);
        safety.setPadding(8, 10, 8, 20);
        root.addView(safety);

        Button back = outlineButton("Back");
        addFullButton(back);
        back.setOnClickListener(v -> showHome());
    }

    private void showHelp() {
        setupRoot();
        root.setGravity(Gravity.TOP);
        root.addView(title("Help & Nikah Guidance", 28));
        root.addView(subtitle(
                "Simple guidance for a respectful Muslim marriage journey."
        ));

        TextView help = new TextView(this);
        help.setText(
                "1. Complete your profile honestly.\n\n" +
                "2. Set clear partner preferences.\n\n" +
                "3. Review compatibility and profile information.\n\n" +
                "4. Express Interest respectfully.\n\n" +
                "5. Wait for mutual acceptance before chat.\n\n" +
                "6. Report/block anything unsafe or inappropriate.\n\n" +
                "7. For religious questions, seek guidance from a trusted qualified scholar."
        );
        help.setTextSize(16);
        help.setTextColor(dark);
        help.setPadding(8, 10, 8, 20);
        root.addView(help);

        Button back = outlineButton("Back");
        addFullButton(back);
        back.setOnClickListener(v -> showHome());
    }
}

/*
 * BEST NIKAH BRIDGE — PRODUCTION READINESS NOTES
 * ------------------------------------------------
 * This MainActivity contains the complete client-side flow currently present in the
 * uploaded build: profile, matching, interests, safety, help and language screens.
 *
 * IMPORTANT: Genuine production services cannot be implemented safely by adding code
 * to this one Activity alone. The following must be implemented in the Android project
 * and backend before a production Play release:
 *
 * 1) Authentication: Firebase Auth (or another real identity provider)
 * 2) Cloud database: Firestore/Realtime Database with security rules
 * 3) Real matching: server-side matching logic; no hard-coded compatibility scores
 * 4) Verification: secure document/selfie upload + admin review + verified status
 * 5) Mutual connections: server-enforced acceptance state
 * 6) Chat: authenticated backend chat with server-side authorization
 * 7) Reports/blocks: persistent backend moderation records + admin queue
 * 8) Ads: Google Mobile Ads SDK + server-controlled daily message entitlement
 * 9) Premium: Google Play Billing; never bypass Play billing for digital features
 * 10) Privacy/Terms/Account deletion: public policy URLs and in-app deletion flow
 * 11) Security: no secrets in source; release signing handled by CI secrets
 * 12) Play compliance: Data safety, content rating, target API, account deletion,
 *     and other current Play Console declarations must be completed separately.
 *
 * Do NOT label static demo profiles as genuinely verified users in production.
 * Do NOT claim a local block or local interest is server-enforced.
 * Do NOT ship payment/ads code without the required Gradle dependencies and configuration.
 */
