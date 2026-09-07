package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

/**
 * Best Nikah Bridge - additive Premium Upgrade screen.
 * Existing screens/features are preserved. Payment execution is intentionally
 * kept behind the Google Play Billing integration point so no fake payment is shown.
 */
public class UpgradeActivity extends Activity {
    private final int green = Color.rgb(18,103,82);
    private final int dark = Color.rgb(30,45,41);
    private final int gray = Color.rgb(95,108,103);
    private LinearLayout root;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        build();
    }

    private TextView text(String s, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setTextColor(bold ? dark : gray);
        v.setPadding(8, 8, 8, 12);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private Button button(String s, boolean fill) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(fill ? Color.WHITE : green);
        b.setBackgroundColor(fill ? green : Color.WHITE);
        root.addView(b, new LinearLayout.LayoutParams(-1, 62));
        return b;
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 24, 22, 32);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("Premium Upgrade", 28, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView intro = text("Choose the plan that fits your Nikah journey. Payments are handled securely through Google Play.", 16, false);
        root.addView(intro);

        addPlan("Basic • 20", "Affordable support for serious Nikah seekers", new String[]{
                "More daily profile actions", "Extra connection opportunities", "Privacy & safety controls"
        }, "20");
        addPlan("Premium • 40", "More opportunities and better visibility", new String[]{
                "Everything in Basic", "More daily interests/messages", "Enhanced match discovery", "Priority support"
        }, "40");
        addPlan("VIP • 60", "Maximum support for your Nikah search", new String[]{
                "Everything in Premium", "Highest connection allowance", "Priority profile visibility", "Priority support & guidance"
        }, "60");

        root.addView(text("Secure payment • No fake payment confirmation • Your plan is activated only after Google Play confirms the purchase.", 14, false));
        Button back = button("Back", false);
        back.setOnClickListener(v -> finish());
    }

    private void addPlan(String name, String subtitle, String[] benefits, String productKey) {
        root.addView(text(name, 22, true));
        root.addView(text(subtitle, 15, false));
        StringBuilder sb = new StringBuilder();
        for (String x : benefits) sb.append("✓ ").append(x).append("\n");
        root.addView(text(sb.toString(), 15, false));
        Button upgrade = button("Upgrade • " + name, true);
        upgrade.setOnClickListener(v -> {
            // Payment provider hook: connect this productKey to Google Play Billing.
            // Never report success locally; backend must verify the Play purchase first.
            Toast.makeText(this, "Secure Google Play payment setup is required for plan " + productKey + ".", Toast.LENGTH_LONG).show();
        });
    }
}
