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

/** Real progress tracker derived from the member's saved Firebase profile signals. */
public class NikahJourneyActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db; private LinearLayout root; private TextView summary;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);
    private int done=0;
    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();render();load();}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(6,8,6,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);return b;}
    private void render(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);sc.addView(root);setContentView(sc);
        root.addView(txt("My Nikah Journey",27,true));
        root.addView(txt("A real, privacy-respecting progress path from preparation to serious marriage discussion. Progress is based only on information and actions recorded by Best Nikah Bridge.",15,false));
        summary=txt("Checking your real progress…",18,true);root.addView(summary);
        Button refresh=btn("Refresh My Journey",true);root.addView(refresh,new LinearLayout.LayoutParams(-1,62));refresh.setOnClickListener(v->load());
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,62));back.setOnClickListener(v->finish());
    }
    private String s(DocumentSnapshot d,String k){Object v=d.get(k);return v==null?"":String.valueOf(v).trim();}
    private boolean yes(DocumentSnapshot d,String k){return Boolean.TRUE.equals(d.getBoolean(k));}
    private void load(){
        if(auth.getCurrentUser()==null){summary.setText("Sign in is required.");return;}String uid=auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(u->{
            db.collection("livingCompatibility").document(uid).get().addOnSuccessListener(l->{
                root.removeViews(3,Math.max(0,root.getChildCount()-5));done=0;
                String name=s(u,"name"),country=s(u,"country"),city=s(u,"city"),intent=s(u,"marriageIntent"),timeline=s(l,"marriageTimeline");
                String family=s(l,"familyInvolvement"),children=s(l,"childrenExpectation"),living=s(l,"livingPlan"),career=s(l,"careerPlan"),deen=s(l,"deenPriorities");
                String verification=s(u,"verificationStatus");boolean intentConfirmed=yes(u,"intentConfirmed");boolean profile= !name.isEmpty()&&!country.isEmpty()&&!city.isEmpty()&&!intent.isEmpty();
                addStage("1. Prepare your profile",profile,"Name, country, city and marriage intention are saved.");
                addStage("2. Define your marriage blueprint",!timeline.isEmpty()&&!family.isEmpty()&&!children.isEmpty()&&!living.isEmpty()&&!career.isEmpty()&&!deen.isEmpty(),"Timeline, family, children, living, career and Deen priorities are recorded.");
                addStage("3. Build trust",verification.equalsIgnoreCase("verified")||intentConfirmed, "Verification or genuine marriage-intent confirmation is recorded.");
                addStage("4. Review real matches",Boolean.TRUE.equals(u.getBoolean("profileActive"))&&Boolean.TRUE.equals(u.getBoolean("discoverable")),"Your profile is active and discoverable for the matching system.");
                addStage("5. Have serious conversations",intentConfirmed,"Your marriage intent is confirmed; use mutual-only communication and serious questions when a connection is mutual.");
                addStage("6. Involve family / Wali",!family.isEmpty(),"Your stated family/Wali involvement preference is recorded. Any actual Wali connection must be completed through the platform flow.");
                addStage("7. Prepare for Nikah",false,"This stage is intentionally not marked complete automatically. It requires real-life mutual agreement and appropriate family/legal/religious steps.");
                int total=7;summary.setText("Journey progress: "+done+" / "+total+" stages recorded as complete\n\nThis is a progress aid, not a prediction or guarantee of marriage.");
            }).addOnFailureListener(e->summary.setText("Could not load your real marriage blueprint."));
        }).addOnFailureListener(e->summary.setText("Could not load your real profile."));
    }
    private void addStage(String title,boolean complete,String detail){if(complete)done++;root.addView(txt((complete?"✓ ":"○ ")+title+"\n"+detail,16,complete));}
}
