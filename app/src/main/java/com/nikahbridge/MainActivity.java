package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    LinearLayout root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private TextView title(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(Color.BLACK);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(10, 20, 10, 20);
        return t;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setPadding(20, 15, 20, 15);
        return b;
    }

    private void setupRoot() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);
        root.setBackgroundColor(Color.WHITE);
        setContentView(root);
    }

    private void showHome() {
        setupRoot();

        root.addView(title("Best Nikah Bridge", 30));

        TextView subtitle = new TextView(this);
        subtitle.setText("Trusted • Simple • Safe");
        subtitle.setTextSize(20);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(10, 10, 10, 50);
        root.addView(subtitle);

        Button createProfile = button("Create Profile");
        Button viewMatches = button("View Matches");

        root.addView(createProfile,
                new LinearLayout.LayoutParams(-1, 70));
        root.addView(viewMatches,
                new LinearLayout.LayoutParams(-1, 70));

        createProfile.setOnClickListener(v -> showCreateProfile());
        viewMatches.setOnClickListener(v -> showMatches());
    }

    private void showCreateProfile() {
        setupRoot();

        root.setGravity(Gravity.TOP);

        root.addView(title("Create Your Profile", 28));

        EditText name = new EditText(this);
        name.setHint("Name");
        name.setTextSize(17);
        root.addView(name,
                new LinearLayout.LayoutParams(-1, 60));

        EditText age = new EditText(this);
        age.setHint("Age");
        age.setInputType(2);
        age.setTextSize(17);
        root.addView(age,
                new LinearLayout.LayoutParams(-1, 60));

        EditText country = new EditText(this);
        country.setHint("Country");
        country.setTextSize(17);
        root.addView(country,
                new LinearLayout.LayoutParams(-1, 60));

        EditText about = new EditText(this);
        about.setHint("About yourself");
        about.setTextSize(17);
        about.setGravity(Gravity.TOP);
        root.addView(about,
                new LinearLayout.LayoutParams(-1, 120));

        Button save = button("Save Profile");
        Button back = button("Back");

        root.addView(save,
                new LinearLayout.LayoutParams(-1, 70));
        root.addView(back,
                new LinearLayout.LayoutParams(-1, 70));

        save.setOnClickListener(v -> {
            String n = name.getText().toString().trim();

            if (n.isEmpty()) {
                name.setError("Please enter your name");
                return;
            }

            Toast.makeText(
                    this,
                    "Profile saved successfully",
                    Toast.LENGTH_LONG
            ).show();
        });

        back.setOnClickListener(v -> showHome());
    }

    private void showMatches() {
        setupRoot();

        root.setGravity(Gravity.TOP);

        root.addView(title("Recommended Matches", 28));

        addMatch(
                "Ayesha • 27",
                "Pakistan",
                "Marriage intention: Serious"
        );

        addMatch(
                "Fatima • 29",
                "Saudi Arabia",
                "Marriage intention: Serious"
        );

        addMatch(
                "Maryam • 26",
                "Germany",
                "Marriage intention: Serious"
        );

        Button back = button("Back");
        root.addView(back,
                new LinearLayout.LayoutParams(-1, 70));

        back.setOnClickListener(v -> showHome());
    }

    private void addMatch(
            String name,
            String country,
            String intention) {

        TextView match = new TextView(this);

        match.setText(
                name + "\n" +
                country + "\n" +
                intention + "\n" +
                "Verified profile • View Profile"
        );

        match.setTextSize(17);
        match.setTextColor(Color.BLACK);
        match.setPadding(20, 20, 20, 20);

        root.addView(match,
                new LinearLayout.LayoutParams(-1, 130));
    }
}
