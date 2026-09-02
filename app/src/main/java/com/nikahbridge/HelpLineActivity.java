package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
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

/** Real Help Line: Firebase AI answers ordinary app-help questions; sensitive cases go to human support. */
public class HelpLineActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFunctions functions;
    private EditText question;
    private TextView result;
    private Button send;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41);
    private final Executor mainExecutor=command -> runOnUiThread(command);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth=FirebaseAuth.getInstance();
        functions=FirebaseFunctions.getInstance();
        build();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28,28,28,28);
        root.setBackgroundColor(Color.rgb(247,250,249));
        scroll.addView(root);
        setContentView(scroll);

        TextView title=txt("Help Line",26,true,green);
        title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,70));
        TextView info=txt("AI Help Assistant is available 24/7 for ordinary app questions.\nIf your request needs human review, it is securely queued and our target is a reply within 24 hours.",15,false,dark);
        root.addView(info,new LinearLayout.LayoutParams(-1,-2));

        question=new EditText(this);
        question.setHint("How can we help you?");
        question.setMinLines(5);
        question.setGravity(Gravity.TOP|Gravity.START);
        question.setPadding(18,18,18,18);
        root.addView(question,new LinearLayout.LayoutParams(-1,180));

        send=new Button(this);
        send.setText("ASK HELP ASSISTANT");
        send.setTextColor(Color.WHITE);
        send.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        send.setBackgroundColor(green);
        root.addView(send,new LinearLayout.LayoutParams(-1,64));

        result=txt("",16,false,dark);
        result.setPadding(8,24,8,8);
        root.addView(result,new LinearLayout.LayoutParams(-1,-2));
        send.setOnClickListener(v->ask());
    }

    private void ask(){
        if(auth.getCurrentUser()==null){ result.setText("Please sign in first."); return; }
        String q=question.getText().toString().trim();
        if(q.isEmpty()){ question.setError("Please enter your question."); return; }
        send.setEnabled(false);
        result.setText("Thinking…");

        boolean sensitive=java.util.regex.Pattern.compile("(password|otp|one[- ]time|bank|iban|card|refund|payment dispute|verification decision|ban|blocked|report|legal|police|harass|abuse|self[- ]harm|suicide)",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(q).find();
        if(sensitive){
            queueHuman(q);
            return;
        }

        try {
            GenerativeModel ai=FirebaseAI.getInstance(GenerativeBackend.googleAI()).generativeModel("gemini-3.7-flash");
            GenerativeModelFutures model=GenerativeModelFutures.from(ai);
            String promptText="You are the Best Nikah Bridge Help Assistant. Answer only practical questions about using this serious Muslim matrimonial app. Be concise, respectful, privacy-conscious, and never claim to be human or admin. Never ask for passwords, OTPs, bank/card details, or private secrets. Do not provide binding religious, legal, medical, financial, or safety guarantees. If human support is needed, say so. User question: "+q;
            Content prompt=new Content.Builder().addText(promptText).build();
            ListenableFuture<GenerateContentResponse> future=model.generateContent(prompt);
            Futures.addCallback(future,new FutureCallback<GenerateContentResponse>(){
                @Override public void onSuccess(GenerateContentResponse r){
                    String answer=r.getText();
                    if(answer==null||answer.trim().isEmpty()){queueHuman(q);return;}
                    queueTicket(q,answer,false);
                }
                @Override public void onFailure(Throwable t){queueHuman(q);}
            },mainExecutor);
        } catch(Throwable t){ queueHuman(q); }
    }

    private void queueHuman(String q){
        queueTicket(q,"Aap ki request human support ko bhej di gayi hai. Hamara target hai ke aapko 24 ghanton ke andar reply mile.",true);
    }

    private void queueTicket(String q,String aiAnswer,boolean human){
        Map<String,Object> data=new HashMap<>();
        data.put("question",q);
        data.put("aiAnswer",human?"":aiAnswer);
        data.put("humanRequired",human);
        functions.getHttpsCallable("helpLineAI").call(data)
            .addOnSuccessListener(r->{
                Map<?,?> d=(Map<?,?>)r.getData();
                String answer=String.valueOf(d.get("answer"));
                boolean isHuman=Boolean.TRUE.equals(d.get("humanRequired"));
                result.setText((isHuman?"Human Support:\n\n":"AI Help Assistant:\n\n")+answer);
                send.setEnabled(true);
            })
            .addOnFailureListener(e->{
                result.setText("Your request could not be saved securely right now. Please try again.");
                send.setEnabled(true);
            });
    }

    private TextView txt(String s,int size,boolean bold,int color){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
}
