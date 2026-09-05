package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;

public class OwnerEarningsActivity extends Activity {
    private LinearLayout root;
    private FirebaseFunctions functions;
    private TextView summary;
    private int dp(int v){ return Math.round(v * getResources().getDisplayMetrics().density); }

    private void add(String text, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(bold ? Color.rgb(30,45,41) : Color.rgb(95,108,103));
        t.setPadding(dp(8), dp(10), dp(8), dp(10));
        if (bold) t.setTypeface(null, 1);
        root.addView(t);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(Color.rgb(18,103,82));
        root.addView(b, new LinearLayout.LayoutParams(-1, dp(62)));
        return b;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        functions = FirebaseFunctions.getInstance();
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(24), dp(22), dp(30));
        scroll.addView(root);
        setContentView(scroll);

        add("Best Nikah Bridge — Owner Earnings", 25, true);
        add("Admin-only production ledger. Verified sales are never treated as cash until real settlement is reconciled.", 14, false);
        summary = new TextView(this);
        summary.setTextSize(18);
        summary.setTextColor(Color.rgb(30,45,41));
        summary.setPadding(dp(8), dp(16), dp(8), dp(16));
        root.addView(summary);

        Button refresh = button("Refresh Owner Earnings");
        Button destinations = button("Set Settlement Destination");
        Button history = button("Sales & Settlement History");
        Button back = button("Back");
        refresh.setOnClickListener(v -> load());
        destinations.setOnClickListener(v -> destinationDialog());
        history.setOnClickListener(v -> history());
        back.setOnClickListener(v -> finish());
        load();
    }

    private String value(Map<?,?> m, String key){ Object v=m.get(key); return v==null?"0":String.valueOf(v); }
    private void load() {
        functions.getHttpsCallable("getOwnerEarningsDashboard").call(new HashMap<>())
            .addOnSuccessListener(r -> {
                Object raw=r.getData();
                if(!(raw instanceof Map)) { summary.setText(String.valueOf(raw)); return; }
                Map<?,?> d=(Map<?,?>)raw;
                StringBuilder s=new StringBuilder();
                s.append("MONTHLY EARNINGS DASHBOARD\n\n");
                s.append("Current month: ").append(value(d,"currentMonth")).append("\n");
                s.append("This month upgrades: ").append(value(d,"currentMonthUpgrades")).append("\n");
                s.append("This month plan value: ").append(value(d,"currentMonthPlanValueSar")).append(" SAR\n\n");
                s.append("Total verified upgrades: ").append(value(d,"totalVerifiedUpgrades")).append("\n");
                s.append("Total verified plan value: ").append(value(d,"totalVerifiedPlanValueSar")).append(" SAR\n\n");
                Object counts=d.get("planCounts");
                if(counts instanceof Map){ Map<?,?> c=(Map<?,?>)counts; s.append("20 SAR upgrades: ").append(value(c,"bnb_plus_20")).append("\n"); s.append("40 SAR upgrades: ").append(value(c,"bnb_plus_40")).append("\n"); s.append("60 SAR upgrades: ").append(value(c,"bnb_plus_60")).append("\n\n"); }
                Object months=d.get("monthly");
                if(months instanceof java.util.List){ s.append("MONTHLY HISTORY\n"); for(Object o:(java.util.List<?>)months){ if(o instanceof Map){ Map<?,?> m=(Map<?,?>)o; s.append(value(m,"month")).append(" — ").append(value(m,"upgrades")).append(" upgrades, ").append(value(m,"planValueSar")).append(" SAR").append("\n"); }} }
                Object users=d.get("users");
                if(users instanceof java.util.List){ s.append("\nRECENT BUYERS\n"); int n=0; for(Object o:(java.util.List<?>)users){ if(!(o instanceof Map))continue; Map<?,?> u=(Map<?,?>)o; String name=value(u,"name"); String email=value(u,"email"); s.append(name.isEmpty()?email:(name+(email.isEmpty()?"":" — "+email))).append(" | ").append(value(u,"upgrades")).append(" upgrade(s) | ").append(value(u,"planValueSar")).append(" SAR | ").append(value(u,"lastUpgradeAt")).append("\n"); if(++n>=20)break; }}
                s.append("\nNote: verified sales are ledger values. Actual Google Play merchant settlement/payout remains controlled by Google Play.");
                summary.setText(s.toString());
            })
            .addOnFailureListener(e -> summary.setText("Owner earnings dashboard unavailable: " + e.getMessage()));
    }

    private void destinationDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText country = new EditText(this); country.setHint("Country");
        EditText currency = new EditText(this); currency.setHint("Currency: SAR / PKR / USDT");
        EditText destination = new EditText(this); destination.setHint("Bank account/IBAN or USDT wallet address");
        EditText label = new EditText(this); label.setHint("Label (e.g. Saudi bank)");
        box.addView(country, new LinearLayout.LayoutParams(-1,dp(56))); box.addView(currency, new LinearLayout.LayoutParams(-1,dp(56))); box.addView(destination, new LinearLayout.LayoutParams(-1,dp(56))); box.addView(label, new LinearLayout.LayoutParams(-1,dp(56)));
        new AlertDialog.Builder(this).setTitle("Owner Settlement Destination").setView(box)
            .setMessage("Only configure a real account or wallet you own and a provider-approved settlement route.")
            .setPositiveButton("Save", (d,w) -> {
                Map<String,Object> m = new HashMap<>();
                m.put("country", country.getText().toString().trim());
                m.put("currency", currency.getText().toString().trim());
                m.put("destination", destination.getText().toString().trim());
                m.put("label", label.getText().toString().trim());
                functions.getHttpsCallable("saveOwnerSettlementProfile").call(m)
                    .addOnSuccessListener(x -> { load(); new AlertDialog.Builder(this).setTitle("Saved").setMessage(String.valueOf(x.getData())).setPositiveButton("OK",null).show(); })
                    .addOnFailureListener(x -> new AlertDialog.Builder(this).setTitle("Not saved").setMessage(x.getMessage()).setPositiveButton("OK",null).show());
            }).setNegativeButton("Cancel", null).show();
    }

    private void history() {
        functions.getHttpsCallable("listOwnerEarnings").call(new HashMap<>())
            .addOnSuccessListener(r -> new AlertDialog.Builder(this).setTitle("Owner Sales & Settlements").setMessage(String.valueOf(r.getData())).setPositiveButton("Close",null).show())
            .addOnFailureListener(e -> new AlertDialog.Builder(this).setTitle("History unavailable").setMessage(e.getMessage()).setPositiveButton("Close",null).show());
    }
}
