package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;

/** Real Azure AI Nikah Mediator with private Azure session storage. */
public class NikahMediatorActivity extends Activity {
    private EditText mySide,otherSide;private CheckBox consent;private TextView result;private Button run;
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;

    @Override protected void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(8),dp(4),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private EditText area(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setGravity(Gravity.TOP|Gravity.START);e.setMinHeight(dp(130));e.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(14));g.setStroke(dp(1),Color.rgb(205,215,211));e.setBackground(g);return e;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}

    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(light);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));sc.addView(root);setContentView(sc);
        root.addView(txt("🧠 AI Nikah Mediator",28,true));root.addView(txt("Real Azure AI discussion support. It does not decide who is right or replace family/Wali/professional help.",15,false));
        root.addView(txt("Your perspective",17,true));mySide=area("Explain your concern or expectation.");root.addView(mySide,new LinearLayout.LayoutParams(-1,dp(140)));
        root.addView(txt("Other person's perspective",17,true));otherSide=area("Enter the other person's words only with permission.");root.addView(otherSide,new LinearLayout.LayoutParams(-1,dp(140)));
        consent=new CheckBox(this);consent.setText("I have permission to use the other person's information.");root.addView(consent);
        run=btn("Run Real AI Mediation",true);root.addView(run,new LinearLayout.LayoutParams(-1,dp(62)));run.setOnClickListener(v->mediate());
        result=txt("The mediation result will appear here after a real Azure AI response.",15,false);root.addView(result);
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }

    private void mediate(){
        if(!AzureAuthManager.hasAccount(this)){LanguageManager.toast(this,"Azure sign in required.",Toast.LENGTH_LONG).show();return;}
        String a=mySide.getText().toString().trim(),b=otherSide.getText().toString().trim();
        if(a.length()<20||b.length()<20){LanguageManager.toast(this,"Please provide both perspectives with enough detail.",Toast.LENGTH_LONG).show();return;}
        if(!consent.isChecked()){LanguageManager.toast(this,"Permission is required.",Toast.LENGTH_LONG).show();return;}
        run.setEnabled(false);result.setText("Azure AI is reviewing both perspectives…");
        String prompt="Treat both perspectives fairly. Return neutral summaries, shared ground, unresolved issues, five practical questions and a calm next step. Do not declare a winner or invent facts.\n\nPerspective A:\n"+a+"\n\nPerspective B:\n"+b;
        try{
            JSONObject body=new JSONObject().put("prompt",prompt);
            AzureApiClient.post("/ai/nikah-mediator",body.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String response){runOnUiThread(()->{try{String out=new JSONObject(response).getJSONObject("assistant").getString("content");result.setText(out);save(a,b,out);}catch(Exception e){result.setText("Azure AI returned an invalid response.");}run.setEnabled(true);});}
                public void err(String message){runOnUiThread(()->{result.setText("Azure AI mediation is temporarily unavailable.");run.setEnabled(true);});}
            });
        }catch(Exception e){result.setText("Could not prepare the Azure AI request.");run.setEnabled(true);}
    }

    private void save(String a,String b,String out){
        try{
            JSONObject v=new JSONObject().put("perspectiveA",a).put("perspectiveB",b).put("lastResult",out);
            AzureApiClient.put("/settings/nikah_mediator_last",v.toString(),new AzureApiClient.Callback(){public void ok(int c,String x){}public void err(String m){runOnUiThread(()->LanguageManager.toast(NikahMediatorActivity.this,"Mediation completed, but private Azure history could not be saved.",Toast.LENGTH_LONG).show());}});
        }catch(Exception ignored){}
    }
}
