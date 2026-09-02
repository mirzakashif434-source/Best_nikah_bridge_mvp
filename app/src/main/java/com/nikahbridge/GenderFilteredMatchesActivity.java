package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

/**
 * Additive real gender-filtered matching view.
 * A female member is shown only male profiles; a male member is shown only
 * female profiles. It uses authenticated Firestore data and the existing
 * reciprocal preference fields. No demo members or fabricated matches.
 */
public class GenderFilteredMatchesActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout root;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        show();
    }

    private void show() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 22, 20, 30);
        root.setBackgroundColor(Color.rgb(247,250,249));
        setContentView(root);
        TextView title = text("Gender-Filtered Nikah Matches", 27, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        root.addView(text("Real profiles only. Women see eligible men; men see eligible women. Existing compatibility and privacy rules still apply.", 15, false));

        if (auth.getCurrentUser() == null) {
            root.addView(text("Please sign in again.", 16, true));
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(me -> {
            if (!me.exists() || !Boolean.TRUE.equals(me.getBoolean("profileActive")) || !Boolean.TRUE.equals(me.getBoolean("discoverable"))) {
                root.addView(text("Complete and activate your real profile first.", 16, true));
                return;
            }
            String myGender = normalized(me.getString("gender"));
            if (!isMale(myGender) && !isFemale(myGender)) {
                root.addView(text("Set your gender as male or female in your real profile before viewing gender-filtered matches.", 16, true));
                return;
            }
            final boolean lookingForMale = isFemale(myGender);
            db.collection("users").whereEqualTo("profileActive", true).whereEqualTo("discoverable", true).limit(100).get()
                .addOnSuccessListener(q -> {
                    int count = 0;
                    for (DocumentSnapshot d : q) {
                        if (d.getId().equals(uid)) continue;
                        String candidateGender = normalized(d.getString("gender"));
                        if (lookingForMale ? !isMale(candidateGender) : !isFemale(candidateGender)) continue;
                        String lookingFor = normalized(d.getString("lookingFor"));
                        if (!lookingFor.isEmpty() && !preferenceAccepts(lookingFor, myGender)) continue;
                        addCandidate(d);
                        count++;
                    }
                    if (count == 0) root.addView(text("No eligible real profiles are available yet. This list will change as genuine members join and activate profiles.", 16, false));
                })
                .addOnFailureListener(e -> root.addView(text("Could not load real matches. Please try again.", 16, false)));
        }).addOnFailureListener(e -> root.addView(text("Could not load your real profile.", 16, false)));

        Button back = new Button(this);
        back.setText("Back");
        back.setAllCaps(false);
        root.addView(back, new LinearLayout.LayoutParams(-1, 62));
        back.setOnClickListener(v -> finish());
    }

    private void addCandidate(DocumentSnapshot d) {
        String name = value(d, "name");
        String age = value(d, "age");
        String country = value(d, "country");
        TextView card = text(name + " • " + age + " • " + country, 18, true);
        root.addView(card);
    }

    private boolean preferenceAccepts(String preference, String myGender) {
        String p = preference.toLowerCase(Locale.ROOT);
        if (isMale(myGender)) return p.contains("male") || p.contains("man") || p.contains("boy") || p.contains("mard") || p.contains("لڑکا") || p.contains("مرد");
        return p.contains("female") || p.contains("woman") || p.contains("girl") || p.contains("larki") || p.contains("ladki") || p.contains("لڑکی") || p.contains("عورت");
    }

    private boolean isMale(String s) {
        return s.contains("male") || s.equals("m") || s.contains("man") || s.contains("mard") || s.contains("لڑکا") || s.contains("مرد");
    }

    private boolean isFemale(String s) {
        return s.contains("female") || s.equals("f") || s.contains("woman") || s.contains("female") || s.contains("larki") || s.contains("ladki") || s.contains("لڑکی") || s.contains("عورت");
    }

    private String normalized(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT); }
    private String value(DocumentSnapshot d, String key) { Object x = d.get(key); return x == null ? "" : String.valueOf(x); }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(bold ? Color.rgb(30,45,41) : Color.rgb(95,108,103));
        t.setPadding(6, 8, 6, 12);
        return t;
    }
}
