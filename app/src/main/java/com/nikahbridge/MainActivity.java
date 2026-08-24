package com.nikahbridge;

import android.app.Activity;
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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private LinearLayout root;
    private int blue = Color.rgb(23, 107, 87);
    private int dark = Color.rgb(24, 50, 44);
    private int gray = Color.rgb(100, 117, 111);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        t.setTextSize(19);
        t.setTextColor(gray);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 8, 12, 20);
        return t;
    }

    private Button appButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(17);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(64);
        b.setPadding(20, 10, 20, 10);
        b.setBackgroundColor(blue);
        return b;
    }

    private void setupRoot() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 250, 249));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(24, 40, 24, 40);

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
        SpaceView space = new SpaceView();
        root.addView(space, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
        ));
    }

    private class SpaceView extends View {
        SpaceView() {
            super(MainActivity.this);
        }
    }

    private void showHome() {
        setupRoot();

        addSpace(45);

        root.addView(
                title("Best Nikah Bridge", 32),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        root.addView(
                subtitle("Trusted • Simple • Safe"),
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        addSpace(20);

        Button createProfile = appButton("Create Profile");
        root.addView(
                createProfile,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        70
                )
        );

        addSpace(14);

        Button viewMatches = appButton("View Matches");
        root.addView(
                viewMatches,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        70
                )
        );

        createProfile.setOnClickListener(v -> showCreateProfile());
        viewMatches.setOnClickListener(v -> showMatches());

        addSpace(40);

        TextView info = new TextView(this);
        info.setText("A serious Muslim matrimonial platform\nfor safe and respectful Nikah.");
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

        EditText name = new EditText(this);
        name.setHint("Name");
        name.setTextSize(17);
        addInput(name, 65);

        EditText age = new EditText(this);
        age.setHint("Age");
        age.setTextSize(17);
        age.setInputType(InputType.TYPE_CLASS_NUMBER);
        addInput(age, 65);

        EditText country = new EditText(this);
        country.setHint("Country");
        country.setTextSize(17);
        addInput(country, 65);

        EditText about = new EditText(this);
        about.setHint("About yourself");
        about.setTextSize(17);
        about.setGravity(Gravity.TOP);
        about.setMinLines(4);
        about.setPadding(16, 16, 16, 16);

        root.addView(
                about,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        130
                )
        );

        addSpace(18);

        Button save = appButton("Save Profile");
        root.addView(
                save,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        68
                )
        );

        addSpace(12);

        Button back = appButton("Back");
        root.addView(
                back,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        68
                )
        );

        save.setOnClickListener(v -> {
            String n = name.getText().toString().trim();

            if (n.isEmpty()) {
                name.setError("Please enter your name");
                name.requestFocus();
                return;
            }

            Toast.makeText(
                    MainActivity.this,
                    "Profile saved successfully",
                    Toast.LENGTH_LONG
            ).show();
        });

        back.setOnClickListener(v -> showHome());
    }

    private void addInput(EditText input, int height) {
        input.setPadding(16, 8, 16, 8);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        height
                );

        params.setMargins(0, 8, 0, 8);
        root.addView(input, params);
    }

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

        addSpace(20);

        Button back = appButton("Back");
        root.addView(
                back,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        68
                )
        );

        back.setOnClickListener(v -> showHome());
    }

    private void addMatch(
            String name,
            String country,
            String intention
    ) {
        TextView match = new TextView(this);

        match.setText(
                name + "\n" +
                country + "\n" +
                intention + "\n" +
                "✓ Verified profile • View Profile"
        );

        match.setTextSize(17);
        match.setTextColor(dark);
        match.setGravity(Gravity.CENTER);
        match.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        145
                );

        params.setMargins(0, 8, 0, 8);

        root.addView(match, params);
    }
}
