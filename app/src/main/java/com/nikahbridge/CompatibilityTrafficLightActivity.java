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

/** Real compatibility visualization from two authenticated profile records. No demo data. */
public class CompatibilityTrafficLightActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db; private LinearLayout root;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();render();}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(6,8,6,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,62));return b;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);root.addView(e,new LinearLayout.LayoutParams(-1,62));return e;}
    private String val(DocumentSnapshot d,String k){Object v=d.get(k);return v==null?"":String.valueOf(v);}
    private int num(DocumentSnapshot d,String k,int f){Long v=d.getLong(k);return v==null?f:v.intValue();}
    private String norm(String x){return x==null?"":x.toLowerCase(Locale.US).trim();}
    private void render(){
        base();root.addView(txt("Compatibility Traffic Light",27,true));root.addView(txt("A transparent view of real profile alignment. Green means aligned, yellow means discuss carefully, red means a possible conflict. It is not a judgment or guarantee.",15,false));
        EditText uid=input("Enter the real member Firebase UID");Button check=btn("Check Real Compatibility",true);check.setOnClickListener(v->check(uid.getText().toString().trim()));
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }
    private void check(String other){if(other.isEmpty()||auth.getCurrentUser()==null){toast("Enter a real member UID.");return;}if(other.equals(auth.getUid())){toast("Choose another member.");return;}db.collection("users").document(auth.getUid()).get().addOnSuccessListener(me->db.collection("users").document(other).get().addOnSuccessListener(them->{if(!them.exists()||!Boolean.TRUE.equals(them.getBoolean("profileActive"))||!Boolean.TRUE.equals(them.getBoolean("discoverable"))){toast("That real profile is unavailable for matching.");return;}showResult(me,them);}).addOnFailureListener(e->toast("Could not load the real member profile."))).addOnFailureListener(e->toast("Could not load your real profile."));}
    private void showResult(DocumentSnapshot a,DocumentSnapshot b){int age=22,timeline=20,location=18,values=20,trust=20;int ageScore=age(a,b),timelineScore=equal(a,b,"marriageTimeline")?20:0,locationScore=equal(a,b,"country")?10:0;if(equal(a,b,"city"))locationScore=18;int valuesScore=overlap(val(a,"partnerPreference"),val(b,"partnerPreference"))?20:8;int trustScore=("verified".equalsIgnoreCase(val(a,"verificationStatus"))?10:0)+("verified".equalsIgnoreCase(val(b,"verificationStatus"))?10:0);addLight("Age & mutual range",ageScore,22);addLight("Marriage timeline",timelineScore,20);addLight("Location",locationScore,18);addLight("Shared preferences",valuesScore,20);addLight("Trust signals",trustScore,20);root.addView(txt("Important: deal-breakers and family expectations should be discussed directly. This screen does not infer hidden traits.",14,false));}
    private int age(DocumentSnapshot a,DocumentSnapshot b){int aa=num(a,"age",0),bb=num(b,"age",0),amin=num(a,"preferredMinAge",18),amax=num(a,"preferredMaxAge",100),bmin=num(b,"preferredMinAge",18),bmax=num(b,"preferredMaxAge",100);return aa>=bmin&&aa<=bmax&&bb>=amin&&bb<=amax?22:0;}
    private boolean equal(DocumentSnapshot a,DocumentSnapshot b,String k){String x=norm(val(a,k)),y=norm(val(b,k));return !x.isEmpty()&&!y.isEmpty()&&x.equals(y);}
    private boolean overlap(String a,String b){String x=norm(a),y=norm(b);if(x.isEmpty()||y.isEmpty())return false;for(String w:x.split("\\s+"))if(w.length()>3&&y.contains(w))return true;return false;}
    private void addLight(String label,int points,int max){String state=points>=max*.75?"GREEN — aligned":points>=max*.4?"YELLOW — discuss": "RED — possible conflict";root.addView(txt(label+": "+state+" ("+points+"/"+max+")",18,true));}
    private void toast(String x){Toast.makeText(this,x,Toast.LENGTH_LONG).show();}
}
