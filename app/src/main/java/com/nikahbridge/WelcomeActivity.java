package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) { openApp(); return; }
        getWindow().setStatusBarColor(Color.rgb(246,242,231));
        getWindow().setNavigationBarColor(Color.rgb(246,242,231));
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(246,242,231));
        ImageView image = new ImageView(this);
        image.setImageResource(R.drawable.welcome);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(image, new FrameLayout.LayoutParams(-1,-1));
        Button start = hit(78.7f,6.3f);
        Button sign = hit(85.8f,6.0f);
        root.addView(start);
        root.addView(sign);
        start.setOnClickListener(v -> openApp());
        sign.setOnClickListener(v -> openApp());
        setContentView(root);
    }
    private Button hit(float top, float height) {
        Button b = new Button(this);
        b.setText("");
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setAlpha(0.02f);
        b.setContentDescription("Navigation button");
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(-1,0);
        p.height = (int)(getResources().getDisplayMetrics().heightPixels*height/100f);
        p.topMargin = (int)(getResources().getDisplayMetrics().heightPixels*top/100f);
        p.leftMargin = (int)(getResources().getDisplayMetrics().widthPixels*0.13f);
        p.rightMargin = p.leftMargin;
        b.setLayoutParams(p);
        return b;
    }
    private void openApp() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
