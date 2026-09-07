package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Best Nikah Bridge - real Global Community Chat.
 * Messages are written only by the authenticated production callable backend;
 * this Activity never performs client-side message writes.
 */
public class CommunityChatActivity extends Activity {
    private final int green = Color.rgb(18,103,82);
    private final int dark = Color.rgb(30,45,41);
    private final int gray = Color.rgb(95,108,103);
    private final int light = Color.rgb(247,250,249);

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;
    private ListenerRegistration listener;
    private LinearLayout messages;
    private EditText composer;
    private ScrollView scroll;
    private final Set<String> mutedUids = new HashSet<>();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance();
        if (auth.getCurrentUser() == null) { finish(); return; }
        build();
        loadMuted();
        listen();
    }

    @Override protected void onDestroy() {
        if (listener != null) listener.remove();
        super.onDestroy();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(bold ? dark : gray);
        t.setPadding(dp(8), dp(5), dp(8), dp(5));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String label, boolean filled) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(filled ? Color.WHITE : green);
        GradientDrawable g = new GradientDrawable();
        g.setColor(filled ? green : Color.WHITE);
        g.setCornerRadius(dp(18));
        if (!filled) g.setStroke(dp(1), green);
        b.setBackground(g);
        b.setPadding(dp(8), 0, dp(8), 0);
        return b;
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(12));
        root.setBackgroundColor(light);
        TextView title = text("Global Community Chat", 27, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(52)));
        TextView info = text("FREE • All countries • Nikah-focused community\nNo phone numbers, passwords, OTPs, money requests or private contact details.", 14, false);
        root.addView(info);
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, dp(8), 0, dp(10));
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        LinearLayout composerRow = new LinearLayout(this);
        composerRow.setGravity(Gravity.CENTER_VERTICAL);
        composer = new EditText(this);
        composer.setHint("Write a respectful Nikah-community message…");
        composer.setTextSize(15);
        composer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        composer.setMaxLines(4);
        composer.setPadding(dp(12), dp(8), dp(12), dp(8));
        composerRow.addView(composer, new LinearLayout.LayoutParams(0, dp(62), 1f));
        Button send = button("Send", true);
        composerRow.addView(send, new LinearLayout.LayoutParams(dp(92), dp(58)));
        root.addView(composerRow);
        Button back = button("Back", false);
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(-1, dp(52));
        backLp.setMargins(0, dp(6), 0, 0);
        root.addView(back, backLp);
        setContentView(root);
        send.setOnClickListener(v -> sendMessage());
        composer.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && !event.isShiftPressed()) { sendMessage(); return true; }
            return false;
        });
        back.setOnClickListener(v -> finish());
    }

    private void loadMuted() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) return;
        db.collection("users").document(u.getUid()).collection("communityMutes").get()
                .addOnSuccessListener(s -> { for (DocumentSnapshot d : s) mutedUids.add(d.getId()); listen(); });
    }

    private void listen() {
        if (listener != null) listener.remove();
        listener = db.collection("communityMessages").orderBy("createdAt", Query.Direction.ASCENDING).limitToLast(100)
                .addSnapshotListener((snap, error) -> {
                    if (error != null || snap == null) { toast("Community chat could not be loaded."); return; }
                    messages.removeAllViews();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String authorUid = d.getString("authorUid");
                        if (authorUid != null && mutedUids.contains(authorUid)) continue;
                        addMessageCard(d);
                    }
                    scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
                });
    }

    private void addMessageCard(DocumentSnapshot d) {
        String author = d.getString("authorName");
        String country = d.getString("country");
        String body = d.getString("text");
        String uid = d.getString("authorUid");
        if (author == null) author = "Member";
        if (country == null) country = "";
        if (body == null) body = "";
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(16)); card.setBackground(bg);
        card.addView(text(author + (country.isEmpty() ? "" : " • " + country), 15, true));
        card.addView(text(body, 16, false));
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.END);
        Button report = button("Report", false), mute = button("Mute", false), block = button("Block", false);
        actions.addView(report, new LinearLayout.LayoutParams(dp(86), dp(44)));
        actions.addView(mute, new LinearLayout.LayoutParams(dp(78), dp(44)));
        actions.addView(block, new LinearLayout.LayoutParams(dp(82), dp(44)));
        card.addView(actions);
        final String finalAuthor = author;
        report.setOnClickListener(v -> reportMessage(d));
        mute.setOnClickListener(v -> muteUser(uid, finalAuthor));
        block.setOnClickListener(v -> blockUser(uid, finalAuthor));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(5), 0, dp(5)); messages.addView(card, lp);
    }

    private void sendMessage() {
        String value = composer.getText().toString().trim();
        if (value.isEmpty()) { composer.setError("Write a message first"); return; }
        if (value.length() > 500) { composer.setError("Maximum 500 characters"); return; }
        Map<String, Object> data = new HashMap<>(); data.put("text", value);
        functions.getHttpsCallable("sendCommunityMessage").call(data)
                .addOnSuccessListener(r -> { composer.setText(""); toast("Message sent to the real community."); })
                .addOnFailureListener(e -> toast(e.getMessage() == null ? "Message could not be sent." : e.getMessage()));
    }

    private void reportMessage(DocumentSnapshot d) {
        FirebaseUser u = auth.getCurrentUser(); if (u == null) return;
        String reportedUid = d.getString("authorUid");
        if (reportedUid == null || reportedUid.equals(u.getUid())) { toast("You cannot report your own message."); return; }
        final String[] reasons = {"Spam / repeated promotion", "Scam / money request", "Harassment / abuse", "Inappropriate content", "Private contact details", "Other"};
        new AlertDialog.Builder(this).setTitle("Report community message").setItems(reasons, (dialog, which) -> {
            Map<String,Object> r = new HashMap<>(); r.put("reporterUid", u.getUid()); r.put("reportedUid", reportedUid); r.put("reason", reasons[which]); r.put("targetType", "communityMessage"); r.put("targetMessageId", d.getId()); r.put("status", "pending"); r.put("createdAt", FieldValue.serverTimestamp());
            db.collection("reports").add(r).addOnSuccessListener(x -> toast("Report sent securely to moderation.")).addOnFailureListener(x -> toast("Report could not be submitted."));
        }).show();
    }

    private void muteUser(String uid, String name) {
        FirebaseUser u = auth.getCurrentUser(); if (u == null || uid == null || uid.equals(u.getUid())) return;
        Map<String,Object> data = new HashMap<>(); data.put("targetUid", uid);
        functions.getHttpsCallable("muteCommunityUser").call(data)
                .addOnSuccessListener(x -> { mutedUids.add(uid); listen(); toast(name + " muted in Community Chat."); })
                .addOnFailureListener(x -> toast("Mute could not be saved."));
    }

    private void blockUser(String uid, String name) {
        FirebaseUser u = auth.getCurrentUser(); if (u == null || uid == null || uid.equals(u.getUid())) return;
        new AlertDialog.Builder(this).setTitle("Block " + name + "?").setMessage("Blocking is a real safety action. It will also close an active mutual connection if one exists.")
                .setNegativeButton("Cancel", null).setPositiveButton("Block", (dialog, which) -> {
                    Map<String,Object> data = new HashMap<>(); data.put("blockedUid", uid);
                    functions.getHttpsCallable("blockUser").call(data).addOnSuccessListener(x -> { mutedUids.add(uid); listen(); toast("Member blocked securely."); }).addOnFailureListener(x -> toast("Block failed."));
                }).show();
    }

    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }
}
