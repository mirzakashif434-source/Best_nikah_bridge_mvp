package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real discussion-question generator from safe Azure match facts only. */
public class SmartSeriousQuestionsActivity extends Activity {
    private LinearLayout root;private EditText uidInput;
    private LinearLayout results;
    private Button generate;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        AzureAuthManager.bindActivity(this);
        render();
        if(!AzureAuthManager.hasAccount(this)) showAuthRecovery();
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}

    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);sc.setBackgroundColor(light);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);sc.addView(root);setContentView(sc);
        ViewCompat.setOnApplyWindowInsetsListener(sc,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(20),dp(22),dp(20),dp(30)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(sc);
        TextView title=txt("Smart Serious Questions",27,true);title.setGravity(Gravity.CENTER);root.addView(title);
        root.addView(txt("Questions are generated from real, safe Azure match facts only. Private hidden profile data is not exposed.",15,false));
        uidInput=new EditText(this);uidInput.setHint("Real matched member ID");uidInput.setSingleLine(true);root.addView(uidInput,new LinearLayout.LayoutParams(-1,dp(62)));
        generate=btn("Generate Questions",true);root.addView(generate,new LinearLayout.LayoutParams(-1,dp(62)));generate.setOnClickListener(v->generate());
        Button health=btn("Conversation Health",true);root.addView(health,new LinearLayout.LayoutParams(-1,dp(62)));health.setOnClickListener(v->startActivity(new Intent(this,ConversationHealthActivity.class)));
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);root.addView(results);
    }

    private void showAuthRecovery(){
        uidInput.setEnabled(false);
        generate.setEnabled(false);
        Button signIn=btn("Sign in with Azure",true);
        signIn.setOnClickListener(v->startActivity(new Intent(this,AzureExternalAuthActivity.class)));
        root.addView(signIn,new LinearLayout.LayoutParams(-1,dp(62)));
    }

    private void generate(){
        if(!AzureAuthManager.hasAccount(this)){showAuthRecovery();return;}
        String id=uidInput.getText().toString().trim();if(id.isEmpty()){LanguageManager.toast(this,"Enter a real matched member ID.",Toast.LENGTH_LONG).show();return;}
        results.removeAllViews();
        generate.setEnabled(false);
        generate.setText("Loading real match…");
        AzureApiClient.get("/matches/"+id,new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                generate.setEnabled(true);generate.setText("Generate Questions");
                try{
                    JSONObject m=new JSONObject(body).optJSONObject("match");if(m==null){toast("Azure match data unavailable.");return;}
                    results.addView(txt("Questions based on real stated match information",19,true));
                    String timeline=m.optString("marriageTimeline",""),country=m.optString("country",""),city=m.optString("city",""),intent=m.optString("marriageIntention","");
                    if(!timeline.isEmpty())add("Marriage timeline","Their stated timeline: "+timeline,"What timeline would feel comfortable for both of us, and what needs to happen before Nikah?");
                    if(!intent.isEmpty())add("Marriage intention","Their stated intention: "+intent,"What does a serious path toward marriage mean to each of us in practical terms?");
                    if(!country.isEmpty()||!city.isEmpty())add("Location","Their stated location: "+country+(city.isEmpty()?"":" / "+city),"Where would we realistically live after marriage, and how would relocation be handled?");
                    JSONArray reasons=m.optJSONArray("whyWeMatched");if(reasons!=null)for(int i=0;i<reasons.length();i++){String r=reasons.optString(i,"");if(r.toLowerCase().contains("deal-breaker"))add("Deal-breakers","Azure match engine found no detected keyword conflict.","Which non-negotiables should we still clarify directly before becoming emotionally invested?");}
                    add("Family / Wali","This is not inferred from private data.","How and when should family or Wali be involved while respecting both people's consent and privacy?");
                    add("Finances & responsibilities","This is not inferred from private data.","How do we expect work, budgeting, housing and family responsibilities to be handled after marriage?");
                    results.addView(txt("These are discussion prompts only. They do not determine character, safety, religious standing, or marriage success.",14,false));
                }catch(Exception e){toast("Azure match response could not be read.");}
            });}
            public void err(String message){runOnUiThread(()->{generate.setEnabled(true);generate.setText("Generate Questions");toast("Could not load real Azure match: "+message);});}
        });
    }
    private void add(String label,String detail,String q){results.addView(txt(label+"\n"+detail+"\nDiscussion question: "+q,16,true));}
    private void toast(String x){LanguageManager.toast(this,x,Toast.LENGTH_LONG).show();}
}
