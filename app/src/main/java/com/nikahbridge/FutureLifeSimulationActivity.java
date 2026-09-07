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

/** Real Future Life Simulation. No fictional scenarios, members, scores or results are injected. */
public class FutureLifeSimulationActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db; private LinearLayout root; private Button run; private TextView result;
    private final EditText[] my=new EditText[8]; private final EditText[] partner=new EditText[8];
    private final String[] titles={"1. Job loss or financial pressure","2. Caring for parents","3. Children and parenting","4. Relocation or migration","5. Household responsibilities","6. Deen, lifestyle and social boundaries","7. Serious disagreement","8. Privacy and family involvement"};
    private final String[] prompts={"How would you expect both spouses to respond if income suddenly dropped or one lost a job?","How should care, time and financial support for parents be handled?","What are your expectations about children, timing, discipline and shared parenting?","If a genuine opportunity or family need required moving city/country, what would be acceptable?","How should cooking, cleaning, errands and other household responsibilities be shared?","What daily religious, lifestyle, social-media and social-boundary expectations matter to you?","What should happen when a major disagreement cannot be resolved calmly?","What privacy boundaries should exist, and when should Wali/family be involved in serious matters?"};
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);
    private final Executor mainExecutor=c->runOnUiThread(c);
    @Override protected void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();render();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(8),dp(4),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private EditText area(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setGravity(Gravity.TOP|Gravity.START);e.setMinHeight(dp(110));e.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(14));g.setStroke(dp(1),Color.rgb(205,215,211));e.setBackground(g);return e;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(light);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));sc.addView(root);setContentView(sc);
        root.addView(txt("🔥 Future Life Simulation",28,true));
        root.addView(txt("Answer the same real-life marriage scenarios separately. The tool compares both perspectives for agreement, discussion areas and practical questions. It does not predict the future, judge faith, or decide whether two people should marry.",15,false));
        root.addView(txt("Your answers",20,true));
        for(int i=0;i<8;i++){root.addView(txt(titles[i]+" — "+prompts[i],16,true));my[i]=area("Write your honest answer");root.addView(my[i],new LinearLayout.LayoutParams(-1,dp(118)));}
        root.addView(txt("Partner's answers",20,true));
        root.addView(txt("Enter the partner's answers only with their explicit permission, or have the partner complete this section themselves on the same signed-in device. Do not enter unnecessary private data.",14,false));
        for(int i=0;i<8;i++){partner[i]=area("Partner's answer");root.addView(partner[i],new LinearLayout.LayoutParams(-1,dp(118)));}
        CheckBox consent=new CheckBox(this);consent.setText("I have permission to use the partner answers for this comparison and understand the result is discussion support, not a marriage decision.");root.addView(consent);
        run=btn("Run Real Future Life Comparison",true);root.addView(run,new LinearLayout.LayoutParams(-1,dp(62)));
        result=txt("Your real comparison result will appear here after Firebase AI responds.",15,false);root.addView(result);
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
        run.setOnClickListener(v->compare(consent));
    }
    private void load(){if(auth.getCurrentUser()==null)return;db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->{for(int i=0;i<8;i++){String a=d.getString("futureLifeMy"+(i+1));String p=d.getString("futureLifePartner"+(i+1));if(a!=null)my[i].setText(a);if(p!=null)partner[i].setText(p);}});}
    private void compare(CheckBox consent){
        if(auth.getCurrentUser()==null){Toast.makeText(this,"Sign in is required.",Toast.LENGTH_LONG).show();return;}
        if(!consent.isChecked()){Toast.makeText(this,"Permission is required before comparing partner answers.",Toast.LENGTH_LONG).show();return;}
        StringBuilder a=new StringBuilder(),b=new StringBuilder();int count=0;
        for(int i=0;i<8;i++){String av=my[i].getText().toString().trim(),bv=partner[i].getText().toString().trim();if(av.length()<20||bv.length()<20){Toast.makeText(this,"Please answer all 8 scenarios in both sections.",Toast.LENGTH_LONG).show();return;}count++;a.append("Scenario ").append(i+1).append(": ").append(av).append("\n");b.append("Scenario ").append(i+1).append(": ").append(bv).append("\n");}
        run.setEnabled(false);result.setText("Real AI comparison in progress…");
        String prompt="You are the Best Nikah Bridge Future Life Simulation assistant. Compare two people's answers to the same marriage scenarios. Do not calculate a fake compatibility percentage. Do not claim to predict the future. Return: 1) areas of clear agreement, 2) areas where expectations differ, 3) issues that deserve a direct conversation before marriage, 4) five practical questions for both people, 5) a neutral next-step suggestion. Treat all answers fairly. Never judge who is a better Muslim. Do not issue binding Islamic rulings, legal advice, medical advice or a marriage verdict. Do not invent facts. If there are signs of coercion, threats, abuse, fraud or immediate danger, prioritize safety and suggest appropriate human/family/professional help. Encourage consent, dignity, privacy and appropriate Wali/family involvement.\n\nPERSON A:\n"+a+"\nPERSON B:\n"+b;
        GenerativeModel ai=FirebaseAI.getInstance(GenerativeBackend.googleAI()).generativeModel("gemini-3.7-flash");
        GenerativeModelFutures model=GenerativeModelFutures.from(ai);Content content=new Content.Builder().addText(prompt).build();ListenableFuture<GenerateContentResponse> future=model.generateContent(content);
        Futures.addCallback(future,new FutureCallback<GenerateContentResponse>(){
            public void onSuccess(GenerateContentResponse r){String out=r.getText();if(out==null||out.trim().isEmpty()){result.setText("No comparison response was returned. Please try again.");run.setEnabled(true);return;}result.setText(out);save(a.toString(),b.toString(),out);run.setEnabled(true);}
            public void onFailure(Throwable t){result.setText("Future Life Simulation is temporarily unavailable. No result was saved.");run.setEnabled(true);}
        },mainExecutor);
    }
    private void save(String a,String b,String out){Map<String,Object>d=new HashMap<>();d.put("userUid",auth.getUid());d.put("answersA",a);d.put("answersB",b);d.put("aiResult",out);d.put("createdAt",FieldValue.serverTimestamp());for(int i=0;i<8;i++){d.put("futureLifeMy"+(i+1),my[i].getText().toString().trim());d.put("futureLifePartner"+(i+1),partner[i].getText().toString().trim());}db.collection("futureLifeSimulations").add(d).addOnFailureListener(e->Toast.makeText(this,"Comparison completed, but private history could not be saved.",Toast.LENGTH_LONG).show());db.collection("users").document(auth.getUid()).set(d,SetOptions.merge());}
}
