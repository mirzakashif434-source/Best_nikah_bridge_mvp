package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONObject;

/**
 * Production Help Line.
 * Ordinary app-help questions use Azure AI. Sensitive or failed AI cases are
 * stored in Azure PostgreSQL for human review. No Firebase or demo fallback.
 */
public class HelpLineActivity extends Activity {
    private EditText question;
    private TextView result;
    private Button send;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(95,108,103);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        build();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int size,boolean bold,int color){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(6),dp(8),dp(6),dp(10));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(28),dp(28),dp(28),dp(28));root.setBackgroundColor(Color.rgb(247,250,249));
        scroll.addView(root);setContentView(scroll);

        TextView title=txt("Help Line",26,true,green);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,dp(70)));
        root.addView(txt("Azure AI Help Assistant is available for ordinary app questions. Sensitive matters are securely queued for human review.",15,false,dark));

        question=new EditText(this);question.setHint("How can we help you?");question.setMinLines(5);question.setGravity(Gravity.TOP|Gravity.START);question.setPadding(dp(18),dp(18),dp(18),dp(18));
        root.addView(question,new LinearLayout.LayoutParams(-1,dp(180)));

        send=new Button(this);send.setText("ASK HELP ASSISTANT");send.setTextColor(Color.WHITE);send.setTypeface(Typeface.DEFAULT,Typeface.BOLD);send.setBackgroundColor(green);
        root.addView(send,new LinearLayout.LayoutParams(-1,dp(64)));

        result=txt("",16,false,dark);result.setPadding(dp(8),dp(24),dp(8),dp(8));root.addView(result);
        Button back=new Button(this);back.setText("Back");back.setAllCaps(false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(60)));
        back.setOnClickListener(v->finish());
        send.setOnClickListener(v->ask());
    }

    private void ask(){
        if(!AzureAuthManager.hasAccount(this)){result.setText("Please sign in with Azure first.");return;}
        String q=question.getText().toString().trim();
        if(q.isEmpty()){question.setError("Please enter your question.");question.requestFocus();return;}
        send.setEnabled(false);result.setText("Connecting to secure Azure Help Assistant…");
        try{
            JSONObject body=new JSONObject().put("question",q);
            AzureApiClient.post("/help/ask",body.toString(),new AzureApiClient.Callback(){
                @Override public void ok(int code,String response){
                    runOnUiThread(()->{
                        try{
                            JSONObject d=new JSONObject(response);
                            boolean human=d.optBoolean("humanRequired",false);
                            String answer=d.optString("answer","");
                            result.setText((human?"Human Support:\n\n":"AI Help Assistant:\n\n")+answer);
                        }catch(Exception e){result.setText("Help request was saved, but the response could not be displayed.");}
                        send.setEnabled(true);
                    });
                }
                @Override public void err(String message){
                    runOnUiThread(()->{result.setText("Azure Help Assistant is temporarily unavailable. Please try again.");send.setEnabled(true);});
                }
            });
        }catch(Exception e){result.setText("Unable to send help request.");send.setEnabled(true);}
    }
}
