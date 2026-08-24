package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(40, 40, 40, 40);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Best Nikah Bridge");
        title.setTextSize(30);
        title.setTextColor(Color.BLACK);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("Trusted • Simple • Safe");
        subtitle.setTextSize(20);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 20, 0, 50);

        Button createProfile = new Button(this);
        createProfile.setText("Create Profile");

        Button viewMatches = new Button(this);
        viewMatches.setText("View Matches");

        createProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(
                    MainActivity.this,
                    "Create Profile coming next",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });

        viewMatches.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(
                    MainActivity.this,
                    "Matches coming next",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });

        root.addView(title);
        root.addView(subtitle);
        root.addView(createProfile);
        root.addView(viewMatches);

        setContentView(root);
    }
}
