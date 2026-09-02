package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Locked Best Nikah Bridge Welcome experience.
 * All entry actions lead to the real Firebase account/profile flow.
 * No demo users, fake matches, or local-only authentication.
 */
public class WelcomeActivity extends Activity {
    private final int green = Color.rgb(18, 103, 82);
    private final int dark = Color.rgb(25, 55, 45);
    private final int gold = Color.rgb(190, 145, 45);
    private final int cream = Color.rgb(250, 247, 239);
    private LinearLayout root;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        showWelcome();
    }

    private void showWelcome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(cream);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(28, 28, 28, 28);
        scroll.addView(root);
        setContentView(scroll);

        TextView arabic = text("بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ", 30, false, dark);
        arabic.setGravity(Gravity.CENTER);
        root.addView(arabic, params(-1, -2, 0, 8, 0, 4));

        TextView welcome = text("Welcome", 39, true, dark);
        welcome.setGravity(Gravity.CENTER);
        root.addView(welcome, params(-1, -2, 0, 6, 0, 6));

        TextView bridge = text("♡  BEST  NIKAH BRIDGE  ♡", 25, true, green);
        bridge.setGravity(Gravity.CENTER);
        root.addView(bridge, params(-1, -2, 0, 4, 0, 14));

        TextView tagline = text("Meaningful Matches    •    Verified Profiles    •    Private & Safe", 15, true, dark);
        tagline.setGravity(Gravity.CENTER);
        root.addView(tagline, params(-1, -2, 0, 8, 0, 18));

        TextView urdu = text("ایک معتبر مسلم رشتہ پلیٹ فارم\nحلال نکاح • اعتماد • خاندان • رازداری", 17, false, dark);
        urdu.setGravity(Gravity.CENTER);
        root.addView(urdu, params(-1, -2, 0, 8, 0, 20));

        Button getStarted = button("GET STARTED\nآغاز کریں", true);
        Button signIn = button("SIGN IN\nلاگ اِن کریں", false);
        root.addView(getStarted, params(-1, 72, 0, 8, 0, 8));
        root.addView(signIn, params(-1, 72, 0, 8, 0, 8));

        TextView footer = text("Serious Muslim matrimonial platform — not a dating app.", 14, false, dark);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer, params(-1, -2, 0, 18, 0, 8));

        getStarted.setOnClickListener(v -> showAccountDialog(false));
        signIn.setOnClickListener(v -> showAccountDialog(true));
    }

    private Button button(String label, boolean filled) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(filled ? Color.WHITE : green);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(32);
        g.setColor(filled ? green : Color.TRANSPARENT);
        if (!filled) g.setStroke(2, green);
        b.setBackground(g);
        return b;
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams params(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(l, t, r, b);
        return p;
    }

    private void showAccountDialog(boolean signIn) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(8, 4, 8, 0);
        EditText email = new EditText(this);
        email.setHint("Email address");
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password = new EditText(this);
        password.setHint("Password (minimum 8 characters)");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(email, new LinearLayout.LayoutParams(-1, 62));
        box.addView(password, new LinearLayout.LayoutParams(-1, 62));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(signIn ? "Sign in to Best Nikah Bridge" : "Create your real account")
                .setMessage(signIn ? "Use your real Firebase account." : "Create a real account, then verify your email and complete your genuine Nikah profile.")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(signIn ? "SIGN IN" : "GET STARTED", null)
                .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String e = email.getText().toString().trim();
            String p = password.getText().toString();
            if (!e.contains("@")) { email.setError("Valid email required"); return; }
            if (p.length() < 8) { password.setError("Minimum 8 characters"); return; }
            if (signIn) {
                auth.signInWithEmailAndPassword(e, p)
                        .addOnSuccessListener(result -> enterMain(dialog))
                        .addOnFailureListener(err -> toast("Sign in failed. Check your email and password."));
            } else {
                auth.createUserWithEmailAndPassword(e, p)
                        .addOnSuccessListener(result -> {
                            FirebaseUser u = result.getUser();
                            if (u == null) return;
                            u.sendEmailVerification();
                            Map<String,Object> profile = new HashMap<>();
                            profile.put("uid", u.getUid());
                            profile.put("profileActive", false);
                            profile.put("discoverable", false);
                            profile.put("verificationStatus", "unverified");
                            profile.put("termsAccepted", false);
                            profile.put("intentConfirmed", false);
                            profile.put("createdAt", FieldValue.serverTimestamp());
                            db.collection("users").document(u.getUid()).set(profile)
                                    .addOnSuccessListener(ok -> enterMain(dialog))
                                    .addOnFailureListener(err -> toast("Account created, but secure profile setup needs another try."));
                        })
                        .addOnFailureListener(err -> toast("Account creation failed. Email may already be registered."));
            }
        }));
        dialog.show();
    }

    private void enterMain(AlertDialog dialog) {
        dialog.dismiss();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
