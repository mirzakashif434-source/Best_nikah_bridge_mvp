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
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/** Real AI Nikah Mediator. Authenticated users submit two perspectives for neutral discussion support. */
public class NikahMediatorActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db;
    private EditText mySide, otherSide; private CheckBox consent; private TextView result; private Button run;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);
    private final Executor mainExecutor=command -> runOnUiThread(command);

    @Override protected void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(8),dp(4),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private EditText area(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setGravity(Gravity.TOP|Gravity.START);e.setMinHeight(dp(130));e.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(14));g.setStroke(dp(1),Color.rgb(205,215,211));e.setBackground(g);return e;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(light);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));sc.addView(root);setContentView(sc);
        root.addView(txt("🧠 AI Nikah Mediator",28,true));
        root.addView(txt("A neutral discussion tool for serious marriage conversations. It summarizes both perspectives, identifies shared ground and unresolved issues, and creates practical discussion questions. It does not decide who is right, issue fatwas, replace a Wali/family member, or guarantee compatibility.",15,false));
        root.addView(txt("Your perspective",17,true));mySide=area("Explain your concern, expectation or disagreement in your own words.");root.addView(mySide,new LinearLayout.LayoutParams(-1,dp(140)));
        root.addView(txt("Other person's perspective",17,true));otherSide=area("Enter the other person's words only with their permission.");root.addView(otherSide,new LinearLayout.LayoutParams(-1,dp(140)));
        consent=new CheckBox(this);consent.setText("I have permission to use the other person's information for this discussion, and I have removed unnecessary private details.");root.addView(consent);
        run=btn("Run Real AI Mediation",true);root.addView(run,new LinearLayout.LayoutParams(-1,dp(62)));run.setOnClickListener(v->mediate());
        result=txt("The mediation result will appear here after a real Firebase AI response.",15,false);root.addView(result);
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }
    private void mediate(){
        if(auth.getCurrentUser()==null){Toast.makeText(this,"Sign in is required.",Toast.LENGTH_LONG).show();return;}
        String a=mySide.getText().toString().trim(),b=otherSide.getText().toString().trim();
        if(a.length()<20||b.length()<20){Toast.makeText(this,"Please provide both perspectives with enough detail.",Toast.LENGTH_LONG).show();return;}
        if(!consent.isChecked()){Toast.makeText(this,"Permission is required before using the other person's information.",Toast.LENGTH_LONG).show();return;}
        run.setEnabled(false);result.setText("AI is reviewing both perspectives…");
        String prompt="You are the Best Nikah Bridge AI Nikah Mediator. Support a respectful, serious Muslim marriage discussion. Treat both perspectives fairly and never declare a winner. Return these sections: 1) Neutral summary of each perspective, 2) Shared ground, 3) Key differences or unresolved issues, 4) 5 practical questions both people should discuss, 5) A calm next-step suggestion. If the issue involves possible abuse, threats, coercion, fraud, immediate danger, or serious safety risk, clearly advise seeking appropriate human/family/professional help and prioritizing safety. Do not issue binding Islamic rulings or pretend to be a scholar, lawyer, therapist, wali, or matchmaker. Do not invent facts. Do not expose or request passwords, financial credentials, phone numbers, addresses, or unnecessary personal data. Encourage mutual consent, dignity, privacy and Wali/family involvement where appropriate.\n\nPerspective A:\n"+a+"\n\nPerspective B:\n"+b;
        GenerativeModel ai=FirebaseAI.getInstance(GenerativeBackend.googleAI()).generativeModel("gemini-3.7-flash");
        GenerativeModelFutures model=GenerativeModelFutures.from(ai);Content content=new Content.Builder().addText(prompt).build();ListenableFuture<GenerateContentResponse> future=model.generateContent(content);
        Futures.addCallback(future,new FutureCallback<GenerateContentResponse>(){
            public void onSuccess(GenerateContentResponse r){String out=r.getText();if(out==null||out.trim().isEmpty()){result.setText("No mediation response was returned. Please try again.");run.setEnabled(true);return;}result.setText(out);saveSession(a,b,out);run.setEnabled(true);}
            public void onFailure(Throwable t){result.setText("AI mediation is temporarily unavailable. No result was saved. Please try again.");run.setEnabled(true);}
        },mainExecutor);
    }
    private void saveSession(String a,String b,String out){
        Map<String,Object> data=new HashMap<>();data.put("userUid",auth.getUid());data.put("perspectiveA",a);data.put("perspectiveB",b);data.put("result",out);data.put("createdAt",FieldValue.serverTimestamp());
        db.collection("aiMediatorSessions").add(data).addOnFailureListener(e->Toast.makeText(this,"Mediation was completed, but the private history could not be saved.",Toast.LENGTH_LONG).show());
    }
}
