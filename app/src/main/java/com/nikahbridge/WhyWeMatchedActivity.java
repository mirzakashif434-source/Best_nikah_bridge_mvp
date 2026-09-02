package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class WhyWeMatchedActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db; private LinearLayout root;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();show();}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(6,8,6,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private void title(String x){TextView t=txt(x,27,true);t.setGravity(Gravity.CENTER);root.addView(t);}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,62));return b;}
    private String val(DocumentSnapshot d,String k){Object v=d.get(k);return v==null?"":String.valueOf(v);}
    private int num(DocumentSnapshot d,String k,int f){Long v=d.getLong(k);return v==null?f:v.intValue();}
    private String norm(String x){return x==null?"":x.toLowerCase(Locale.US).replace("/"," ").replace(","," ").replace("-"," ").trim();}
    private void show(){
        base();title("Why We Matched");root.addView(txt("Actual profile factors explain why a real member qualifies. No AI-generated people or reasons are used.",15,false));
        Button traffic=btn("Compatibility Traffic Light",true);traffic.setOnClickListener(v->startActivity(new Intent(this,CompatibilityTrafficLightActivity.class)));
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(me->{if(!Boolean.TRUE.equals(me.getBoolean("profileActive"))){root.addView(txt("Complete your real profile first.",16,true));return;}db.collection("users").whereEqualTo("profileActive",true).whereEqualTo("discoverable",true).limit(100).get().addOnSuccessListener(q->{boolean any=false;for(DocumentSnapshot d:q){if(d.getId().equals(auth.getUid()))continue;int sc=score(me,d);if(sc<45)continue;any=true;addCard(me,d,sc);}if(!any)root.addView(txt("No qualifying real matches are available yet.",16,false));}).addOnFailureListener(e->root.addView(txt("Could not load real matches. No fallback data is shown.",16,false)));}).addOnFailureListener(e->root.addView(txt("Could not load your real profile.",16,false)));
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }
    private void addCard(DocumentSnapshot me,DocumentSnapshot d,int sc){String name=val(d,"name");if(name.isEmpty())name="Member";root.addView(txt(name+" • "+val(d,"age")+" • "+sc+"%",21,true));StringBuilder r=new StringBuilder();int myAge=num(me,"age",0),theirAge=num(d,"age",0),myMin=num(me,"preferredMinAge",0),myMax=num(me,"preferredMaxAge",101),theirMin=num(d,"preferredMinAge",0),theirMax=num(d,"preferredMaxAge",101);if(theirAge>=myMin&&theirAge<=myMax&&myAge>=theirMin&&myAge<=theirMax)r.append("✓ Mutual age preference range\n");String mg=norm(val(me,"gender")),tg=norm(val(d,"gender")),ml=norm(val(me,"lookingFor")),tl=norm(val(d,"lookingFor"));if(compatible(ml,tg)&&compatible(tl,mg))r.append("✓ Mutual gender/preference alignment\n");if(equal(me,d,"marriageIntent"))r.append("✓ Same stated marriage intention\n");if(equal(me,d,"marriageTimeline"))r.append("✓ Similar stated marriage timeline\n");if(equal(me,d,"country"))r.append("✓ Same country\n");if(equal(me,d,"city"))r.append("✓ Same city\n");if(overlap(val(me,"partnerPreference"),val(d,"partnerPreference")))r.append("✓ Shared preference keywords\n");if(!conflict(val(me,"dealbreakers"),val(d,"partnerPreference"))&&!conflict(val(d,"dealbreakers"),val(me,"partnerPreference")))r.append("✓ No detected deal-breaker keyword conflict\n");if("verified".equalsIgnoreCase(val(d,"verificationStatus")))r.append("✓ Verification status: verified\n");if(Boolean.TRUE.equals(d.getBoolean("intentConfirmed")))r.append("✓ Genuine marriage-intent confirmation\n");root.addView(txt(r.length()==0?"No specific positive factors could be displayed safely.":r.toString(),15,false));root.addView(txt("This explanation uses stated profile data only; it is not a guarantee of compatibility.",14,false));}
    private boolean equal(DocumentSnapshot a,DocumentSnapshot b,String k){String x=norm(val(a,k)),y=norm(val(b,k));return !x.isEmpty()&&!y.isEmpty()&&x.equals(y);}
    private boolean compatible(String looking,String gender){if(looking.isEmpty()||gender.isEmpty())return false;if(looking.contains("any")||looking.contains("no preference"))return true;return looking.contains(gender)||gender.contains(looking);}
    private boolean overlap(String a,String b){String x=norm(a),y=norm(b);if(x.isEmpty()||y.isEmpty())return false;for(String w:x.split("\\s+"))if(w.length()>3&&y.contains(w))return true;return false;}
    private boolean conflict(String a,String b){String x=norm(a),y=norm(b);if(x.isEmpty()||y.isEmpty())return false;for(String w:x.split("\\s+"))if(w.length()>4&&y.contains(w))return true;return false;}
    private int score(DocumentSnapshot me,DocumentSnapshot d){int s=0;int theirAge=num(d,"age",0),myMin=num(me,"preferredMinAge",0),myMax=num(me,"preferredMaxAge",101),theirMin=num(d,"preferredMinAge",0),theirMax=num(d,"preferredMaxAge",101),myAge=num(me,"age",0);if(theirAge>=myMin&&theirAge<=myMax&&myAge>=theirMin&&myAge<=theirMax)s+=22;else return 0;if(compatible(norm(val(me,"lookingFor")),norm(val(d,"gender")))&&compatible(norm(val(d,"lookingFor")),norm(val(me,"gender"))))s+=18;else return 0;if(equal(me,d,"marriageIntent"))s+=15;if(equal(me,d,"marriageTimeline"))s+=12;if(equal(me,d,"country"))s+=8;if(equal(me,d,"city"))s+=6;if(overlap(val(me,"partnerPreference"),val(d,"partnerPreference")))s+=7;if(!conflict(val(me,"dealbreakers"),val(d,"partnerPreference"))&&!conflict(val(d,"dealbreakers"),val(me,"partnerPreference")))s+=7;if("verified".equalsIgnoreCase(val(d,"verificationStatus")))s+=3;if(Boolean.TRUE.equals(d.getBoolean("intentConfirmed")))s+=2;return Math.min(100,s);}
}
