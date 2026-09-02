package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Executor;

/** Real AI guidance using Firebase AI Logic. It does not impersonate a scholar or match people. */
public class NikahAssistantActivity extends Activity {
    private EditText question; private TextView answer; private Button ask;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);
    private final Executor mainExecutor=command -> runOnUiThread(command);

    @Override public void onCreate(Bundle b){super.onCreate(b);render();}
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(6,8,6,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);return b;}
    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);
        TextView title=txt("AI Nikah Assistant",27,true);title.setGravity(Gravity.CENTER);root.addView(title);
        root.addView(txt("Ask for practical, respectful Nikah guidance. The assistant supports decisions; it is not a mufti, wali, lawyer, doctor, or matchmaker and should not be treated as a religious ruling.",15,false));
        question=new EditText(this);question.setHint("Ask your Nikah question…");question.setTextSize(16);question.setGravity(Gravity.TOP);root.addView(question,new LinearLayout.LayoutParams(-1,130));
        ask=btn("Ask AI Assistant",true);root.addView(ask,new LinearLayout.LayoutParams(-1,62));
        answer=txt("Your answer will appear here.",16,false);root.addView(answer);
        Button help=btn("24/7 Help Line — AI + Human Support",true);root.addView(help,new LinearLayout.LayoutParams(-1,62));
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,62));back.setOnClickListener(v->finish());
        ask.setOnClickListener(v->askAI());
        help.setOnClickListener(v->startActivity(new android.content.Intent(this,HelpLineActivity.class)));
        setContentView(root);
    }
    private void askAI(){String q=question.getText().toString().trim();if(q.isEmpty()){answer.setText("Please enter a question first.");return;}ask.setEnabled(false);answer.setText("Thinking…");
        GenerativeModel ai=FirebaseAI.getInstance(GenerativeBackend.googleAI()).generativeModel("gemini-3.7-flash");
        GenerativeModelFutures model=GenerativeModelFutures.from(ai);
        String system="You are the Best Nikah Bridge AI Assistant. Give concise, respectful guidance for serious Muslim marriage. Do not facilitate dating or sexual content. Do not issue binding fatwas; for religious rulings recommend a qualified scholar. Never invent people, matches, verification, credentials, laws, or facts. Protect privacy and advise users not to share passwords, financial credentials, or unnecessary personal data. Encourage mutual consent, family/Wali involvement where appropriate, safety, and lawful conduct. User question: "+q;
        Content prompt=new Content.Builder().addText(system).build();ListenableFuture<GenerateContentResponse> f=model.generateContent(prompt);
        Futures.addCallback(f,new FutureCallback<GenerateContentResponse>(){public void onSuccess(GenerateContentResponse r){answer.setText(r.getText()==null?"No answer was returned. Please try again.":r.getText());ask.setEnabled(true);}public void onFailure(Throwable t){answer.setText("AI service is temporarily unavailable. Please try again.");ask.setEnabled(true);}},mainExecutor);
    }
}
