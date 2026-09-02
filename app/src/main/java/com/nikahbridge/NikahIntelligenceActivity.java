package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.*;

/**
 * Best Nikah Bridge — Nikah Intelligence Center.
 * Uses the authenticated member's real Firebase profile/preferences only.
 * No demo scores, fake matches, fabricated people, or local-only persistence.
 */
public class NikahIntelligenceActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db; private FirebaseFunctions fn;
    private LinearLayout root;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);
    private EditText country, city, timeline, family, children, career, living, deen, dealbreakers;
    private TextView score;

    @Override public void onCreate(Bundle b){ super.onCreate(b); auth=FirebaseAuth.getInstance(); db=FirebaseFirestore.getInstance(); fn=FirebaseFunctions.getInstance(); render(); load(); }
    private void base(){ ScrollView s=new ScrollView(this); s.setFillViewport(true); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,22,20,30); root.setBackgroundColor(light); s.addView(root); setContentView(s); }
    private TextView txt(String x,int size,boolean bold){ TextView t=new TextView(this); t.setText(x); t.setTextSize(size); t.setTextColor(bold?dark:gray); t.setPadding(6,8,6,10); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private void title(String x){ TextView t=txt(x,27,true); t.setGravity(Gravity.CENTER); root.addView(t); }
    private Button btn(String x,boolean fill){ Button b=new Button(this); b.setText(x); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(fill?Color.WHITE:green); GradientDrawable g=new GradientDrawable(); g.setColor(fill?green:Color.WHITE); g.setCornerRadius(18); if(!fill)g.setStroke(2,green); b.setBackground(g); root.addView(b,new LinearLayout.LayoutParams(-1,62)); return b; }
    private EditText input(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); root.addView(e,new LinearLayout.LayoutParams(-1,62)); return e; }
    private void render(){
        base(); title("Nikah Intelligence Center");
        root.addView(txt("Your Marriage Blueprint — real preferences saved to your Firebase account. This is decision support, not a prediction.",15,false));
        score=txt("Nikah Readiness: calculating…",19,true); root.addView(score);
        country=input("Preferred country / country flexibility"); city=input("Preferred city / relocation flexibility"); timeline=input("Marriage timeline (e.g. 3–6 months, 6–12 months)"); family=input("Family / Wali involvement preference"); children=input("Children expectation"); career=input("Career / work expectation"); living=input("Living arrangement after Nikah"); deen=input("Deen / values priorities"); dealbreakers=input("Important deal-breakers (one per phrase)");
        Button save=btn("Save My Marriage Blueprint",true); save.setOnClickListener(v->save());
        Button scenarios=btn("Future Scenario Simulator",true); scenarios.setOnClickListener(v->scenario());
        Button why=btn("Why We Matched",true); why.setOnClickListener(v->startActivity(new Intent(this,WhyWeMatchedActivity.class)));
        Button journey=btn("My Nikah Journey",false); journey.setOnClickListener(v->journey());
        Button back=btn("Back",false); back.setOnClickListener(v->finish());
        Button refresh=btn("Recalculate Readiness",false); refresh.setOnClickListener(v->calculate());
    }
    private void load(){ if(auth.getCurrentUser()==null)return; String uid=auth.getCurrentUser().getUid(); db.collection("livingCompatibility").document(uid).get().addOnSuccessListener(d->{country.setText(s(d,"country"));city.setText(s(d,"city"));timeline.setText(s(d,"marriageTimeline"));family.setText(s(d,"familyInvolvement"));children.setText(s(d,"childrenExpectation"));career.setText(s(d,"careerPlan"));living.setText(s(d,"livingPlan"));calculate();}); db.collection("users").document(uid).get().addOnSuccessListener(d->{deen.setText(s(d,"deenPriorities"));dealbreakers.setText(s(d,"dealbreakers"));calculate();}); }
    private String s(com.google.firebase.firestore.DocumentSnapshot d,String k){ Object v=d.get(k); return v==null?"":String.valueOf(v); }
    private void save(){
        Map<String,Object> x=new HashMap<>(); x.put("country",v(country)); x.put("city",v(city)); x.put("marriageTimeline",v(timeline)); x.put("familyInvolvement",v(family)); x.put("childrenExpectation",v(children)); x.put("careerPlan",v(career)); x.put("livingPlan",v(living));
        Map<String,Object> profile=new HashMap<>(); profile.put("deenPriorities",v(deen)); profile.put("dealbreakers",v(dealbreakers)); profile.put("updatedAt",FieldValue.serverTimestamp());
        fn.getHttpsCallable("updateLivingCompatibility").call(x).addOnSuccessListener(r->db.collection("users").document(auth.getUid()).set(profile,SetOptions.merge()).addOnSuccessListener(z->{toast("Marriage Blueprint saved securely.");calculate();}).addOnFailureListener(e->toast("Blueprint core saved, but profile preferences could not be saved."))).addOnFailureListener(e->toast("Could not save Blueprint. No local-only fake save was used."));
    }
    private String v(EditText e){ return e.getText().toString().trim(); }
    private void calculate(){ int filled=0,total=9; EditText[] a={country,city,timeline,family,children,career,living,deen,dealbreakers}; for(EditText e:a)if(!v(e).isEmpty())filled++; int pct=Math.round(filled*100f/total); score.setText("Nikah Readiness: "+pct+"/100 — "+(pct>=80?"Strong foundation":pct>=50?"A few important areas remain":"Start by completing your Marriage Blueprint")); }
    private void scenario(){
        final String[] items={"If relocation becomes necessary after Nikah","If one spouse continues working after Nikah","If parents need support or temporary shared living","If children are planned later rather than immediately","If marriage timing changes by several months"};
        new AlertDialog.Builder(this).setTitle("Future Scenario Simulator").setItems(items,(d,w)->{String q=items[w]; new AlertDialog.Builder(this).setTitle(q).setMessage("Discuss this topic openly with a serious mutual connection and your family/Wali where appropriate. Best Nikah Bridge uses your saved Marriage Blueprint as the source of your stated preferences; it does not invent a compatibility prediction.").setPositiveButton("Understood",null).show();}).setNegativeButton("Cancel",null).show();
    }
    private void journey(){ new AlertDialog.Builder(this).setTitle("Nikah Journey").setMessage("Profile → Preferences → Verification → Meaningful Match → Mutual Interest → Safe Communication → Serious Discussion → Family/Wali → Nikah Preparation\n\nComplete your Blueprint first. Future journey stages are only marked by real backend events; this screen never creates fake progress.").setPositiveButton("OK",null).show(); }
    private void toast(String x){ Toast.makeText(this,x,Toast.LENGTH_LONG).show(); }
}
