package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/**
 * Best Nikah Bridge — Trust Passport.
 * Shows only evidence present in the real Firebase account. No invented trust score or badge.
 */
public class TrustPassportActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout root;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){ super.onCreate(b); auth=FirebaseAuth.getInstance(); db=FirebaseFirestore.getInstance(); render(); }
    private void base(){ ScrollView s=new ScrollView(this); s.setFillViewport(true); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,22,20,30); root.setBackgroundColor(light); s.addView(root); setContentView(s); }
    private TextView txt(String x,int z,boolean bold){ TextView t=new TextView(this); t.setText(x); t.setTextSize(z); t.setTextColor(bold?dark:gray); t.setPadding(6,8,6,10); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private Button btn(String x,boolean fill){ Button b=new Button(this); b.setText(x); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(fill?Color.WHITE:green); GradientDrawable g=new GradientDrawable(); g.setColor(fill?green:Color.WHITE); g.setCornerRadius(18); if(!fill)g.setStroke(2,green); b.setBackground(g); root.addView(b,new LinearLayout.LayoutParams(-1,62)); return b; }
    private EditText input(String h){ EditText e=new EditText(this); e.setHint(h); e.setTextSize(16); root.addView(e,new LinearLayout.LayoutParams(-1,62)); return e; }
    private String val(DocumentSnapshot d,String k){ Object v=d.get(k); return v==null?"":String.valueOf(v); }
    private boolean bool(DocumentSnapshot d,String k){ return Boolean.TRUE.equals(d.getBoolean(k)); }
    private void render(){
        base(); root.addView(txt("Trust Passport",27,true));
        root.addView(txt("A transparent evidence card built from real account and safety records. No hidden personality judgment, no fabricated verification, and no fake trust percentage.",15,false));
        EditText uid=input("Real member Firebase UID"); Button check=btn("View Real Trust Passport",true); check.setOnClickListener(v->load(uid.getText().toString().trim()));
        Button back=btn("Back",false); back.setOnClickListener(v->finish());
    }
    private void load(String uid){
        if(auth.getCurrentUser()==null||uid.isEmpty()){toast("Enter a real member UID.");return;}
        db.collection("users").document(uid).get().addOnSuccessListener(profile->{
            if(!profile.exists()||!bool(profile,"profileActive")||!bool(profile,"discoverable")){toast("That real profile is unavailable for Trust Passport viewing.");return;}
            showProfile(profile);
            db.collection("verifications").document(uid).get().addOnSuccessListener(v->{
                String status=val(v,"status");
                addEvidence("Identity verification", "verified".equalsIgnoreCase(status)?"VERIFIED — confirmed by the verification workflow":"NOT VERIFIED — no verified status recorded", "verified".equalsIgnoreCase(status));
            }).addOnFailureListener(e->addEvidence("Identity verification","UNAVAILABLE — verification record could not be read",false));
        }).addOnFailureListener(e->toast("Could not load the real member record."));
    }
    private void showProfile(DocumentSnapshot p){
        root.addView(txt("Real member",20,true));
        root.addView(txt(val(p,"name")+" • Age "+val(p,"age")+" • "+val(p,"country"),17,true));
        String intent=val(p,"marriageIntent");
        addEvidence("Marriage intent", intent.isEmpty()?"NOT STATED":intent, !intent.isEmpty());
        addEvidence("Intent confirmation", bool(p,"intentConfirmed")?"CONFIRMED":"NOT CONFIRMED", bool(p,"intentConfirmed"));
        String photo=val(p,"photoUrl");
        addEvidence("Profile photo", photo.isEmpty()?"NOT PROVIDED":"PRESENT IN REAL PROFILE", !photo.isEmpty());
        int complete=0; String[] fields={"name","age","country","city","about","marriageIntent","partnerPreference","deenPriorities"}; for(String f:fields)if(!val(p,f).isEmpty())complete++;
        addEvidence("Profile completeness",complete+"/"+fields.length+" core fields",complete==fields.length);
        addEvidence("Discoverability",bool(p,"discoverable")?"ACTIVE":"NOT ACTIVE",bool(p,"discoverable"));
        addEvidence("Safety limitation","Trust Passport never overrides blocks, reports, verification, or privacy controls.",true);
        root.addView(txt("Trust Passport is evidence, not a guarantee of character, honesty, compatibility, or marriage outcome. Members should verify important information independently and involve family/Wali where appropriate.",14,false));
    }
    private void addEvidence(String label,String value,boolean positive){ TextView t=txt((positive?"✓ ":"• ")+label+"\n"+value,16,true); t.setPadding(10,12,10,12); root.addView(t); }
    private void toast(String x){Toast.makeText(this,x,Toast.LENGTH_LONG).show();}
}
