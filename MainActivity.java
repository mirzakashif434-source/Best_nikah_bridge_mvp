package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView screen = new TextView(this);
        screen.setText("Best Nikah Bridge\n\nTrusted • Simple • Easy");
        screen.setTextSize(24);
        screen.setTextColor(Color.BLACK);
        screen.setGravity(Gravity.CENTER);
        screen.setPadding(40, 40, 40, 40);

        setContentView(screen);
    }
}
