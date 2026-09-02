package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.*;

/** Production Safe Communication center. Sends only through the trusted backend. */
public class SafeCommunicationActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db; private FirebaseFunctions fn; private LinearLayout root;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);
    private EditText toUid, message; private TextView status, history;
    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();fn=FirebaseFunctions.getInstance();render();}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(6,8,6,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,62));return b;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);root.addView(e,new LinearLayout.LayoutParams(-1,62));return e;}
    private void render(){
        base();root.addView(txt("Mutual-Only Safe Communication",27,true));root.addView(txt("Private marriage-focused communication is unlocked only after a real mutual connection. Messages are written through the trusted Firebase backend; there is no demo chat.",15,false));
        toUid=input("Real matched member Firebase UID");message=input("Write a respectful message");
        Button send=btn("Send Secure Message",true);send.setOnClickListener(v->send());
        Button load=btn("Load Recent Messages",false);load.setOnClickListener(v->loadMessages());
        Button report=btn("Report / Safety Help",false);report.setOnClickListener(v->startActivity(new android.content.Intent(this,MainActivity.class)));
        status=txt("Status: waiting",15,false);root.addView(status);history=txt("",15,false);root.addView(history);Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }
    private void send(){
        if(auth.getCurrentUser()==null){status.setText("Status: sign in required");return;}String to=toUid.getText().toString().trim(),text=message.getText().toString().trim();
        if(to.isEmpty()||text.isEmpty()){status.setText("Status: recipient and message are required");return;}
        HashMap<String,Object> data=new HashMap<>();data.put("toUid",to);data.put("text",text);status.setText("Status: sending securely…");
        fn.getHttpsCallable("sendConnectionMessage").call(data).addOnSuccessListener(r->{message.setText("");status.setText("Status: message sent through the trusted backend");loadMessages();}).addOnFailureListener(e->status.setText("Status: not sent — "+e.getMessage()));
    }
    private void loadMessages(){
        if(auth.getCurrentUser()==null)return;String to=toUid.getText().toString().trim();if(to.isEmpty()){status.setText("Status: enter a real matched member UID");return;}
        String a=auth.getCurrentUser().getUid(),id=a.compareTo(to)<0?a+"_"+to:to+"_"+a;
        db.collection("connections").document(id).collection("messages").orderBy("sentAt",Query.Direction.ASCENDING).limitToLast(50).get().addOnSuccessListener(s->{StringBuilder out=new StringBuilder("Recent secure messages:\n\n");if(s.isEmpty())out.append("No messages yet.");for(DocumentSnapshot d:s)out.append(d.getString("fromUid")).append(": ").append(d.getString("text")).append("\n\n");history.setText(out.toString());status.setText("Status: real connection history loaded");}).addOnFailureListener(e->status.setText("Status: history unavailable — "+e.getMessage()));
    }
}
