package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Map;

/** Real Help Line: AI answers ordinary app-help questions; sensitive cases go to human support. */
public class HelpLineActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFunctions functions;
    private EditText question;
    private TextView result;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41);

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
        TextView info=txt("AI Help Assistant is available when human support is unavailable.\nFor requests needing human review, our target is a reply within 24 hours.",15,false,dark);
        root.addView(info,new LinearLayout.LayoutParams(-1,-2));

        question=new EditText(this);
        question.setHint("How can we help you?");
        question.setMinLines(5);
        question.setGravity(Gravity.TOP|Gravity.START);
        question.setPadding(18,18,18,18);
        root.addView(question,new LinearLayout.LayoutParams(-1,180));

        Button send=new Button(this);
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
        result.setText("Please wait…");
        Map<String,Object> data=new HashMap<>();
        data.put("question",q);
        functions.getHttpsCallable("helpLineAI").call(data)
            .addOnSuccessListener(r->{
                Map<?,?> d=(Map<?,?>)r.getData();
                String answer=String.valueOf(d.get("answer"));
                boolean human=Boolean.TRUE.equals(d.get("humanRequired"));
                result.setText((human?"Human Support:\n\n":"AI Help Assistant:\n\n")+answer);
            })
            .addOnFailureListener(e->result.setText("Help request could not be sent right now. Please try again."));
    }

    private TextView txt(String s,int size,boolean bold,int color){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
}
