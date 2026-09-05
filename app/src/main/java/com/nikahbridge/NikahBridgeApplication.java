package com.nikahbridge;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Additive navigation helper. It does not replace any Activity layout or flow.
 * Secondary screens get a consistent visible Back button when they do not
 * already provide one. Welcome/Main screens are intentionally left unchanged.
 */
public class NikahBridgeApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final int BACK_TAG = 0x4E42424B;

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    private int dp(Activity a, int value) {
        return Math.round(value * a.getResources().getDisplayMetrics().density);
    }

    private boolean isMainScreen(Activity a) {
        String n = a.getClass().getSimpleName();
        return "WelcomeActivity".equals(n) || "MainActivity".equals(n) || "ProductionMainActivity".equals(n);
    }

    private boolean containsBack(View v) {
        if (v instanceof TextView) {
            CharSequence text = ((TextView) v).getText();
            if (text != null && "Back".equalsIgnoreCase(text.toString().trim())) return true;
            CharSequence desc = v.getContentDescription();
            if (desc != null && "Back".equalsIgnoreCase(desc.toString().trim())) return true;
        }
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) if (containsBack(g.getChildAt(i))) return true;
        }
        return false;
    }

    private void addBack(Activity a) {
        if (a.isFinishing() || isMainScreen(a)) return;
        ViewGroup content = a.findViewById(android.R.id.content);
        if (content == null || content.getTag(BACK_TAG) != null || containsBack(content)) return;
        if (!(content instanceof FrameLayout)) return;

        Button back = new Button(a);
        back.setText("‹  Back");
        back.setAllCaps(false);
        back.setTextSize(15);
        back.setTextColor(Color.rgb(18, 103, 82));
        back.setContentDescription("Back");
        back.setElevation(dp(a, 5));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(a, 22));
        bg.setStroke(dp(a, 1), Color.rgb(18, 103, 82));
        back.setBackground(bg);
        back.setPadding(dp(a, 8), 0, dp(a, 10), 0);
        back.setOnClickListener(v -> a.finish());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(a, 104), dp(a, 48), Gravity.TOP | Gravity.START);
        lp.leftMargin = dp(a, 12);
        lp.topMargin = dp(a, 10);
        content.addView(back, lp);
        content.setTag(BACK_TAG, Boolean.TRUE);
    }

    @Override public void onActivityResumed(Activity activity) { addBack(activity); }
    @Override public void onActivityCreated(Activity activity, Bundle state) { }
    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
    @Override public void onActivityDestroyed(Activity activity) { }
}
