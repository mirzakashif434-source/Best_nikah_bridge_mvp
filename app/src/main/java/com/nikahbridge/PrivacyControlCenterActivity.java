package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

/**
 * Production Privacy Control Center.
 * Uses the authenticated user's real Firebase profile; no demo privacy state.
 */
public class PrivacyControlCenterActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout root;
    private Switch discoverable;
    private TextView status;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        render();
        loadRealSettings();
    }

    private TextView txt(String s,int size,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(bold?dark:gray); t.setPadding(6,8,6,10);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }

    private Button btn(String s,boolean fill){
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(fill?Color.WHITE:green);
        GradientDrawable g=new GradientDrawable(); g.setColor(fill?green:Color.WHITE); g.setCornerRadius(18); if(!fill)g.setStroke(2,green); b.setBackground(g);
        root.addView(b,new LinearLayout.LayoutParams(-1,62)); return b;
    }

    private void render(){
        ScrollView sc=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,22,20,30); root.setBackgroundColor(light); sc.addView(root); setContentView(sc);
        root.addView(txt("Privacy Control Center",27,true));
        root.addView(txt("Control how your real Nikah profile is discoverable. These settings change your Firebase profile; nothing is simulated.",15,false));

        discoverable=new Switch(this);
        discoverable.setText("Show my profile in new matches");
        discoverable.setTextSize(17);
        discoverable.setTextColor(dark);
        discoverable.setPadding(6,12,6,12);
        root.addView(discoverable,new LinearLayout.LayoutParams(-1,70));

        root.addView(txt("When disabled, your profile is removed from the discoverable match pool. Existing mutual connections are not automatically deleted by this setting.",14,false));
        Button save=btn("Save Privacy Settings",true);
        save.setOnClickListener(v->saveRealSettings());
        Button refresh=btn("Refresh Real Settings",false);
        refresh.setOnClickListener(v->loadRealSettings());
        Button back=btn("Back",false);
        back.setOnClickListener(v->finish());
        status=txt("Status: waiting",15,false); root.addView(status);
    }

    private void loadRealSettings(){
        if(auth.getCurrentUser()==null){status.setText("Status: sign in required");return;}
        status.setText("Status: loading real privacy settings…");
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(d->{
                    if(!d.exists()){status.setText("Status: real profile not found");return;}
                    discoverable.setChecked(Boolean.TRUE.equals(d.getBoolean("discoverable")));
                    status.setText("Status: real privacy settings loaded");
                })
                .addOnFailureListener(e->status.setText("Status: could not load privacy settings"));
    }

    private void saveRealSettings(){
        if(auth.getCurrentUser()==null){status.setText("Status: sign in required");return;}
        boolean visible=discoverable.isChecked();
        status.setText("Status: saving securely…");
        Map<String,Object> update=new HashMap<>();
        update.put("discoverable",visible);
        update.put("privacyUpdatedAt",FieldValue.serverTimestamp());
        db.collection("users").document(auth.getCurrentUser().getUid()).set(update,SetOptions.merge())
                .addOnSuccessListener(v->status.setText("Status: privacy setting saved — profile visibility is now "+(visible?"ON":"OFF")))
                .addOnFailureListener(e->status.setText("Status: privacy setting was not changed"));
    }
}
