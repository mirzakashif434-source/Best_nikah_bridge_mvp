package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.*;

/** Real Family Bridge 2.0: invite, accept/reject, consent and secure family questions. */
public class FamilyBridge2Activity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFunctions fn;
    private LinearLayout root;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();fn=FirebaseFunctions.getInstance();render();}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(6,8,6,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,62));return b;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);root.addView(e,new LinearLayout.LayoutParams(-1,62));return e;}
    private void call(String name,Map<String,Object> data,String ok){fn.getHttpsCallable(name).call(data).addOnSuccessListener(r->toast(ok)).addOnFailureListener(e->toast("Action blocked: backend safety/consent rules did not allow it."));}
    private void render(){
        base();root.addView(txt("Family Bridge 2.0",27,true));root.addView(txt("Bring family/Wali into a real mutual connection with explicit invitation, accept/reject and consent. No family accounts are invented.",15,false));
        EditText connection=input("Real mutual connection ID");EditText family=input("Family/Wali Firebase UID");Button invite=btn("Send Real Family Invitation",true);invite.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("connectionId",connection.getText().toString().trim());x.put("familyUid",family.getText().toString().trim());call("createFamilyBridgeV2",x,"Real Family Bridge invitation created.");});
        EditText bridge=input("Family Bridge ID");Button accept=btn("Accept Invitation",true);accept.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("bridgeId",bridge.getText().toString().trim());x.put("decision","accept");call("respondFamilyBridgeV2",x,"Family Bridge accepted.");});Button reject=btn("Reject Invitation",false);reject.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("bridgeId",bridge.getText().toString().trim());x.put("decision","reject");call("respondFamilyBridgeV2",x,"Family Bridge rejected.");});
        Button list=btn("Load My Real Family Bridges",false);list.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();fn.getHttpsCallable("listMyFamilyBridgesV2").call(x).addOnSuccessListener(r->toast("Real Family Bridge records loaded. Open Firebase-backed bridge IDs from your account flow." )).addOnFailureListener(e->toast("Could not load Family Bridges."));});
        Button consent=btn("Enable My Family Bridge Consent",true);consent.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("bridgeId",bridge.getText().toString().trim());x.put("enabled",true);call("setFamilyBridgeConsentV2",x,"Your real Family Bridge consent is enabled.");});Button pause=btn("Pause My Consent",false);pause.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("bridgeId",bridge.getText().toString().trim());x.put("enabled",false);call("setFamilyBridgeConsentV2",x,"Your Family Bridge consent is paused.");});
        EditText question=input("Question for family/Wali");Button send=btn("Send Secure Family Question",true);send.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("bridgeId",bridge.getText().toString().trim());x.put("text",question.getText().toString().trim());call("sendFamilyQuestionV2",x,"Question sent through the real Family Bridge.");});
        root.addView(txt("Privacy: only participants accepted into the real bridge can use its consent and question actions. This feature does not claim that family participation guarantees a successful marriage.",14,false));
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }
    private void toast(String x){Toast.makeText(this,x,Toast.LENGTH_LONG).show();}
}
