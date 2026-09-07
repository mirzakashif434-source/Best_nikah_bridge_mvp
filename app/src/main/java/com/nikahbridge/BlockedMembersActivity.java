package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

/** Real Block + Unblock management. No demo/local-only block state. */
public class BlockedMembersActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;
    private LinearLayout list;
    private TextView status;
    private final int green = Color.rgb(18,103,82);
    private final int dark = Color.rgb(30,45,41);
    private final int gray = Color.rgb(85,100,95);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance();
        build();
        loadBlocked();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(bold ? dark : gray);
        t.setPadding(dp(8), dp(7), dp(8), dp(7));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String label, boolean filled) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(15);
        b.setTextColor(filled ? Color.WHITE : green);
        GradientDrawable g = new GradientDrawable(); g.setColor(filled ? green : Color.WHITE); g.setCornerRadius(dp(18));
        if (!filled) g.setStroke(dp(1), green);
        b.setBackground(g); return b;
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(28)); root.setBackgroundColor(Color.rgb(247,250,249));
        scroll.addView(root); setContentView(scroll);
        TextView title = text("Blocked Members", 27, true); title.setGravity(Gravity.CENTER); root.addView(title, new LinearLayout.LayoutParams(-1, dp(55)));
        root.addView(text("Manage the members you have blocked. Unblock is a real server-side safety action and only you can remove a block that you created.", 15, false));
        status = text("Status: loading…", 14, false); root.addView(status);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        Button refresh = button("Refresh", false); refresh.setOnClickListener(v -> loadBlocked()); root.addView(refresh, new LinearLayout.LayoutParams(-1, dp(54)));
        Button back = button("Back", false); back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(54)); bp.setMargins(0, dp(8), 0, 0); root.addView(back, bp);
    }

    private void loadBlocked() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) { status.setText("Status: sign in required"); return; }
        status.setText("Status: loading real blocked members…"); list.removeAllViews();
        db.collection("blocks").whereEqualTo("blockerUid", u.getUid()).whereEqualTo("active", true).orderBy("createdAt", Query.Direction.DESCENDING).limit(100).get()
                .addOnSuccessListener(snap -> {
                    list.removeAllViews();
                    if (snap.isEmpty()) { list.addView(text("No members are currently blocked by you.", 15, false)); status.setText("Status: real block list loaded"); return; }
                    for (DocumentSnapshot d : snap.getDocuments()) addRow(d);
                    status.setText("Status: real block list loaded");
                })
                .addOnFailureListener(e -> { status.setText("Status: could not load blocked members"); toast("Blocked list could not be loaded. If Firestore asks for an index, create the suggested index."); });
    }

    private void addRow(DocumentSnapshot d) {
        String blockedUid = d.getString("blockedUid");
        if (blockedUid == null || blockedUid.isEmpty()) return;
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL); row.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(14)); row.setBackground(bg);
        TextView who = text("Blocked member", 16, true); row.addView(who);
        row.addView(text("Member ID: " + blockedUid, 12, false));
        Button unblock = button("Unblock", true); row.addView(unblock, new LinearLayout.LayoutParams(-1, dp(50)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(5), 0, dp(5)); list.addView(row, lp);
        unblock.setOnClickListener(v -> unblock(blockedUid, row, unblock));
    }

    private void unblock(String blockedUid, LinearLayout row, Button button) {
        button.setEnabled(false); button.setText("Unblocking…");
        Map<String,Object> data = new HashMap<>(); data.put("blockedUid", blockedUid);
        functions.getHttpsCallable("unblockUser").call(data)
                .addOnSuccessListener(r -> { list.removeView(row); toast("Member unblocked securely."); status.setText("Status: real block removed"); })
                .addOnFailureListener(e -> { button.setEnabled(true); button.setText("Unblock"); toast(e.getMessage() == null ? "Unblock failed." : e.getMessage()); });
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}
