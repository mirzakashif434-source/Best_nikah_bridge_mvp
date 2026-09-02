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

    private void add(String text, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(bold ? Color.rgb(30,45,41) : Color.rgb(95,108,103));
        t.setPadding(8, 10, 8, 10);
        if (bold) t.setTypeface(null, 1);
        root.addView(t);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        root.addView(b, new LinearLayout.LayoutParams(-1, 62));
        return b;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        functions = FirebaseFunctions.getInstance();
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 24, 22, 30);
        scroll.addView(root);
        setContentView(scroll);

        add("Best Nikah Bridge — Owner Earnings", 25, true);
        add("Admin-only production ledger. Verified sales are never treated as cash until real settlement is reconciled.", 14, false);
        summary = new TextView(this);
        summary.setTextSize(18);
        summary.setPadding(8, 16, 8, 16);
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

    private void load() {
        functions.getHttpsCallable("getOwnerEarnings").call(new HashMap<>())
            .addOnSuccessListener(r -> summary.setText(String.valueOf(r.getData())))
            .addOnFailureListener(e -> summary.setText("Owner earnings unavailable: " + e.getMessage()));
    }

    private void destinationDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText country = new EditText(this); country.setHint("Country");
        EditText currency = new EditText(this); currency.setHint("Currency: SAR / PKR / USDT");
        EditText destination = new EditText(this); destination.setHint("Bank account/IBAN or USDT wallet address");
        EditText label = new EditText(this); label.setHint("Label (e.g. Saudi bank)");
        box.addView(country); box.addView(currency); box.addView(destination); box.addView(label);
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
