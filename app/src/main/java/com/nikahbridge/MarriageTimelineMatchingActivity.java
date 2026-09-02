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

/** Real timeline matching using stated Firebase marriageTimeline values. */
public class MarriageTimelineMatchingActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db; private LinearLayout root;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();render();}
    private void render(){
        ScrollView s=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);s.addView(root);setContentView(s);
        root.addView(txt("Marriage Timeline Matching",27,true));
        root.addView(txt("Find real discoverable members whose stated marriage timeline is compatible with yours. No invented timelines or predicted readiness.",15,false));
        Button find=btn("Find Real Timeline Matches",true);find.setOnClickListener(v->find());
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }
    private TextView txt(String x,int z,boolean b){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(b?dark:gray);t.setPadding(6,8,6,10);if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,62));return b;}
    private String str(DocumentSnapshot d,String k){Object v=d.get(k);return v==null?"":String.valueOf(v).trim();}
    private String norm(String s){return s.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+"," ").trim();}
    private boolean compatible(String a,String b){String x=norm(a),y=norm(b);if(x.isEmpty()||y.isEmpty())return false;if(x.equals(y))return true;String[] fast={"immediate","now","0 3 months","3 6 months","6 12 months","12 24 months","one year","two years","ready"};int ix=-1,iy=-1;for(int i=0;i<fast.length;i++){if(x.contains(fast[i]))ix=i;if(y.contains(fast[i]))iy=i;}if(ix>=0&&iy>=0)return Math.abs(ix-iy)<=1;return x.contains(y)||y.contains(x);}
    private void find(){
        if(auth.getCurrentUser()==null){toast("Sign in is required.");return;}String me=auth.getCurrentUser().getUid();
        db.collection("livingCompatibility").document(me).get().addOnSuccessListener(my->{String mine=str(my,"marriageTimeline");if(mine.isEmpty()){toast("Complete your real marriage timeline first.");return;}
            db.collection("users").whereEqualTo("profileActive",true).whereEqualTo("discoverable",true).limit(100).get().addOnSuccessListener(q->{root.addView(txt("Your stated timeline: "+mine,18,true));int count=0;for(DocumentSnapshot u:q.getDocuments()){if(u.getId().equals(me))continue;String uid=u.getId();db.collection("livingCompatibility").document(uid).get().addOnSuccessListener(t->{String theirs=str(t,"marriageTimeline");if(compatible(mine,theirs)){String name=str(u,"name");String age=str(u,"age");root.addView(txt("✓ "+(name.isEmpty()?"Member":name)+" • Age "+age+"\nStated timeline: "+theirs+"\nMatch reason: timelines appear compatible based only on stated values.",16,true));}});} }).addOnFailureListener(e->toast("Could not load real discoverable members."));
        }).addOnFailureListener(e->toast("Could not load your real timeline."));
    }
    private void toast(String x){Toast.makeText(this,x,Toast.LENGTH_LONG).show();}
}
