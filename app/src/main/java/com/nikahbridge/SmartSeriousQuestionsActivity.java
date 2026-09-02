package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/**
 * Real discussion-question generator from two members' stated profile differences.
 * Questions are prompts for respectful conversation, not fabricated answers or scores.
 */
public class SmartSeriousQuestionsActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db; private LinearLayout root; private EditText uidInput;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();render();}
    private TextView txt(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(6,8,6,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);return b;}
    private void render(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);sc.addView(root);setContentView(sc);
        TextView title=txt("Smart Serious Questions",27,true);title.setGravity(Gravity.CENTER);root.addView(title);
        root.addView(txt("Turn real differences in two profiles into respectful Nikah discussion questions. Nothing is invented and no answer is assumed.",15,false));
        uidInput=new EditText(this);uidInput.setHint("Enter the real member Firebase UID");uidInput.setSingleLine(true);root.addView(uidInput,new LinearLayout.LayoutParams(-1,62));
        Button generate=btn("Generate Questions",true);root.addView(generate,new LinearLayout.LayoutParams(-1,62));generate.setOnClickListener(v->generate());
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,62));back.setOnClickListener(v->finish());
    }
    private String s(DocumentSnapshot d,String k){Object v=d.get(k);return v==null?"":String.valueOf(v).trim();}
    private String norm(String x){return x.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+"," ").trim();}
    private boolean differs(String a,String b){return !norm(a).isEmpty()&&!norm(b).isEmpty()&&!norm(a).equals(norm(b));}
    private void generate(){
        if(auth.getCurrentUser()==null){Toast.makeText(this,"Sign in is required.",Toast.LENGTH_LONG).show();return;}
        String me=auth.getCurrentUser().getUid(), other=uidInput.getText().toString().trim();
        if(other.isEmpty()||other.equals(me)){Toast.makeText(this,"Enter a different real member UID.",Toast.LENGTH_LONG).show();return;}
        db.collection("users").document(other).get().addOnSuccessListener(target->{
            if(!target.exists()||!Boolean.TRUE.equals(target.getBoolean("profileActive"))||!Boolean.TRUE.equals(target.getBoolean("discoverable"))){Toast.makeText(this,"That member is not currently discoverable.",Toast.LENGTH_LONG).show();return;}
            db.collection("livingCompatibility").document(me).get().addOnSuccessListener(my->{
                db.collection("livingCompatibility").document(other).get().addOnSuccessListener(theirs->{
                    root.addView(txt("Questions based on real stated profile information",19,true));
                    addQuestion("Marriage timeline",s(my,"marriageTimeline"),s(theirs,"marriageTimeline"),"What timeline would feel comfortable for both of us, and what needs to happen before Nikah?");
                    addQuestion("Children",s(my,"childrenExpectation"),s(theirs,"childrenExpectation"),"How do we view children, timing, and responsibilities after marriage?");
                    addQuestion("Living plan",s(my,"livingPlan"),s(theirs,"livingPlan"),"Where would we realistically live after marriage, and how would we handle a change in location?");
                    addQuestion("Family / Wali",s(my,"familyInvolvement"),s(theirs,"familyInvolvement"),"How would we involve our families or Wali respectfully while protecting both people's consent and privacy?");
                    addQuestion("Career",s(my,"careerPlan"),s(theirs,"careerPlan"),"What are our expectations around work, study, finances, and responsibilities after marriage?");
                    addQuestion("Deen priorities",s(my,"deenPriorities"),s(theirs,"deenPriorities"),"Which religious priorities matter most to each of us, and how can we support them in daily life?");
                    addQuestion("Deal-breakers",s(my,"dealbreakers"),s(theirs,"dealbreakers"),"Are there any non-negotiables we should clarify openly before becoming emotionally invested?");
                    addQuestion("Partner preferences",s(my,"partnerPreference"),s(theirs,"partnerPreference"),"Which expectations should we discuss now so neither person assumes the other will change later?");
                    root.addView(txt("These are conversation prompts only. They do not determine character, religious standing, safety, or whether a marriage will succeed.",14,false));
                }).addOnFailureListener(e->toast("Could not load the real member's compatibility data."));
            }).addOnFailureListener(e->toast("Could not load your real profile data."));
        }).addOnFailureListener(e->toast("Could not load the real member profile."));
    }
    private void addQuestion(String label,String a,String b,String q){if(!differs(a,b)&&a.isEmpty()&&b.isEmpty())return;String detail="Your: "+(a.isEmpty()?"Not stated":a)+"\nTheir: "+(b.isEmpty()?"Not stated":b);root.addView(txt(label+"\n"+detail+"\nDiscussion question: "+q,16,true));}
    private void toast(String x){Toast.makeText(this,x,Toast.LENGTH_LONG).show();}
}
