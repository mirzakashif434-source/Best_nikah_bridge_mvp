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
 * Real outcome-focused Nikah success planner.
 * Uses only the authenticated member's recorded Firebase signals.
 * It never invents matches, success rates, marriage dates, or outcomes.
 */
public class NikahSuccessPlanActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout root;
    private TextView summary;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        render();
        loadRealOutcomeSignals();
    }

    private TextView txt(String s,int z,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(z); t.setTextColor(bold?dark:gray); t.setPadding(6,8,6,10);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }

    private Button btn(String s,boolean fill){
        Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(fill?Color.WHITE:green);
        GradientDrawable g=new GradientDrawable(); g.setColor(fill?green:Color.WHITE); g.setCornerRadius(18); if(!fill)g.setStroke(2,green); b.setBackground(g);
        return b;
    }

    private void render(){
        ScrollView sc=new ScrollView(this);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,22,20,30); root.setBackgroundColor(light); sc.addView(root); setContentView(sc);
        root.addView(txt("Nikah Success Plan",27,true));
        root.addView(txt("Focus on meaningful progress instead of endless swiping. This plan shows real preparation and platform actions recorded for your account.",15,false));
        summary=txt("Checking your real progress…",18,true); root.addView(summary);
        Button refresh=btn("Refresh Real Progress",true); root.addView(refresh,new LinearLayout.LayoutParams(-1,62)); refresh.setOnClickListener(v->loadRealOutcomeSignals());
        Button journey=btn("Open My Nikah Journey",false); root.addView(journey,new LinearLayout.LayoutParams(-1,62)); journey.setOnClickListener(v->startActivity(new android.content.Intent(this,NikahJourneyActivity.class)));
        Button matches=btn("Open Real Compatibility Matches",false); root.addView(matches,new LinearLayout.LayoutParams(-1,62)); matches.setOnClickListener(v->startActivity(new android.content.Intent(this,MainActivity.class)));
        Button back=btn("Back",false); root.addView(back,new LinearLayout.LayoutParams(-1,62)); back.setOnClickListener(v->finish());
    }

    private String s(DocumentSnapshot d,String k){Object v=d.get(k);return v==null?"":String.valueOf(v).trim();}
    private boolean yes(DocumentSnapshot d,String k){return Boolean.TRUE.equals(d.getBoolean(k));}

    private void loadRealOutcomeSignals(){
        if(auth.getCurrentUser()==null){summary.setText("Sign in is required.");return;}
        String uid=auth.getCurrentUser().getUid();
        summary.setText("Loading real progress…");
        db.collection("users").document(uid).get().addOnSuccessListener(u->{
            if(!u.exists()){summary.setText("Real profile not found.");return;}
            db.collection("livingCompatibility").document(uid).get().addOnSuccessListener(l->{
                db.collection("connections").whereEqualTo("users", Arrays.asList(uid)).get()
                    .addOnSuccessListener(c->{
                        db.collection("interests").whereEqualTo("fromUid",uid).get()
                            .addOnSuccessListener(i->{
                                renderRealPlan(u,l,c.size(),i.size());
                            })
                            .addOnFailureListener(e->renderRealPlan(u,l,c.size(),-1));
                    })
                    .addOnFailureListener(e->renderRealPlan(u,l,-1,-1));
            }).addOnFailureListener(e->renderRealPlan(u,null,-1,-1));
        }).addOnFailureListener(e->summary.setText("Could not load your real profile."));
    }

    private void renderRealPlan(DocumentSnapshot u,DocumentSnapshot l,int connectionCount,int interestCount){
        root.removeViews(3,Math.max(0,root.getChildCount()-7));
        int complete=0,total=6;
        String name=s(u,"name"),country=s(u,"country"),city=s(u,"city"),intent=s(u,"marriageIntent");
        String verification=s(u,"verificationStatus"); boolean intentConfirmed=yes(u,"intentConfirmed");
        boolean profile=!name.isEmpty()&&!country.isEmpty()&&!city.isEmpty()&&!intent.isEmpty();
        boolean blueprint=l!=null && !s(l,"marriageTimeline").isEmpty() && !s(l,"familyInvolvement").isEmpty() && !s(l,"childrenExpectation").isEmpty();
        boolean discoverable=yes(u,"discoverable") && yes(u,"profileActive");
        boolean trust=verification.equalsIgnoreCase("verified") || intentConfirmed;
        boolean connection=connectionCount>0;
        boolean conversation=interestCount>0 || connectionCount>0;
        addStage("1. Complete a truthful profile",profile,"Only your saved profile fields count."); if(profile)complete++;
        addStage("2. Define marriage expectations",blueprint,"Timeline, family involvement and children expectations are checked from your saved blueprint."); if(blueprint)complete++;
        addStage("3. Build platform trust",trust,"Verification or confirmed marriage intent is recorded."); if(trust)complete++;
        addStage("4. Enter the real match pool",discoverable,"Your account is active and discoverable for matching."); if(discoverable)complete++;
        addStage("5. Progress a real connection",connection,"A recorded connection exists for this account."); if(connection)complete++;
        addStage("6. Have serious mutual conversations",conversation,"Recorded interests or connections show real platform activity; this does not mean marriage is guaranteed."); if(conversation)complete++;
        summary.setText("Real progress: "+complete+" / "+total+" outcome stages\n\nConnections recorded: "+(connectionCount<0?"not available":connectionCount)+"\nInterests sent: "+(interestCount<0?"not available":interestCount)+"\n\nNext step: complete the first unfinished stage above. Best Nikah Bridge does not manufacture success, fake matches, or predict a marriage outcome.");
    }

    private void addStage(String title,boolean complete,String detail){root.addView(txt((complete?"✓ ":"○ ")+title+"\n"+detail,16,complete));}
}
