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
    private LinearLayout root,results,matchChoices;
    private TextView matchStatus;
    private Button generate;
    private String selectedMatchId="";
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        AzureAuthManager.bindActivity(this);
        PremiumFeatureGate.require(this,"paid20Features","20 SAR Basic or higher",()->{
            render();
            recoverAzureSessionAndLoadMatches();
        });
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
        matchStatus=txt("Loading your real Azure matches…",15,false);root.addView(matchStatus);
        matchChoices=new LinearLayout(this);matchChoices.setOrientation(LinearLayout.VERTICAL);root.addView(matchChoices);
        generate=btn("Generate Questions",true);generate.setEnabled(false);root.addView(generate,new LinearLayout.LayoutParams(-1,dp(62)));generate.setOnClickListener(v->generate());
        // Match loading starts only after a real Azure token is confirmed.
        Button health=btn("Conversation Health",true);root.addView(health,new LinearLayout.LayoutParams(-1,dp(62)));health.setOnClickListener(v->startActivity(new Intent(this,ConversationHealthActivity.class)));
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);root.addView(results);
    }

    private void recoverAzureSessionAndLoadMatches(){
        generate.setEnabled(false);
        if(matchStatus!=null)matchStatus.setText("Checking your secure Azure session…");
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){runOnUiThread(()->loadMatchChoices());}
            @Override public void err(String message){runOnUiThread(()->showAuthRecovery());}
        });
    }

    private void showAuthRecovery(){
        generate.setEnabled(false);
        if(matchStatus!=null)matchStatus.setText("Azure sign in is required to choose a real match.");
        matchChoices.removeAllViews();
        Button signIn=btn("Sign in with Azure",true);
        signIn.setOnClickListener(v->{
            signIn.setEnabled(false);
            signIn.setText("Opening Azure Sign In…");
            AzureAuthManager.acquireTokenInteractive(this,new AzureAuthManager.Callback(){
                @Override public void ok(String accessToken){runOnUiThread(()->{
                    root.removeView(signIn);
                    loadMatchChoices();
                });}
                @Override public void err(String message){runOnUiThread(()->{
                    signIn.setEnabled(true);
                    signIn.setText("Sign in with Azure");
                    matchStatus.setText("Azure sign in was not completed. Please try again.");
                });}
            });
        });
        root.addView(signIn,new LinearLayout.LayoutParams(-1,dp(62)));
    }

    private void loadMatchChoices(){
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("matches");
                    matchChoices.removeAllViews();
                    if(a==null||a.length()==0){
                        matchStatus.setText("No real compatible matches are available yet.");
                        generate.setEnabled(false);
                        return;
                    }
                    matchStatus.setText("Choose a real match. Secure member IDs stay hidden.");
                    for(int i=0;i<Math.min(a.length(),50);i++){
                        JSONObject m=a.optJSONObject(i);if(m==null)continue;
                        String id=m.optString("userId","").trim();
                        if(id.isEmpty())continue;
                        String name=m.optString("displayName","Member").trim();
                        final String matchId=id,matchName=name.isEmpty()?"Member":name;
                        Button choose=btn("Choose "+matchName,false);
                        choose.setOnClickListener(v->{
                            selectedMatchId=matchId;
                            matchStatus.setText("Selected: "+matchName);
                            generate.setEnabled(true);
                        });
                        matchChoices.addView(choose,new LinearLayout.LayoutParams(-1,dp(58)));
                    }
                }catch(Exception e){
                    matchStatus.setText("Real Azure matches could not be displayed.");
                    generate.setEnabled(false);
                }
            });}
            public void err(String message){runOnUiThread(()->{
                if(message!=null&&(message.contains("AZURE_SIGN_IN_REQUIRED")||message.contains("AZURE_INTERACTION_REQUIRED")||message.contains("401"))){
                    showAuthRecovery();
                }else if(message!=null&&message.contains("PROFILE_NOT_READY")){
                    matchStatus.setText("Complete your real profile first, then return to Smart Serious Questions.");
                    generate.setEnabled(false);
                }else{
                    matchStatus.setText("Real Azure matches are temporarily unavailable.");
                    generate.setEnabled(false);
                }
            });}
        });
    }

    private void generate(){
        String id=selectedMatchId.trim();if(id.isEmpty()){LanguageManager.toast(this,"Choose a real match first.",Toast.LENGTH_LONG).show();return;}
        generate.setEnabled(false);
        generate.setText("Checking Azure session…");
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){runOnUiThread(()->generateAuthorized(id));}
            @Override public void err(String message){runOnUiThread(()->{
                generate.setEnabled(true);
                generate.setText("Generate Questions");
                showAuthRecovery();
            });}
        });
    }

    private void generateAuthorized(String id){
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
            public void err(String message){runOnUiThread(()->{
                generate.setEnabled(true);
                generate.setText("Generate Questions");
                if(message!=null&&(message.contains("AZURE_SIGN_IN_REQUIRED")||message.contains("AZURE_INTERACTION_REQUIRED")||message.contains("401"))){
                    showAuthRecovery();
                }else if(message!=null&&message.contains("PROFILE_NOT_READY")){
                    toast("Complete your real profile first, then try again.");
                }else{
                    toast("Could not load the real Azure match. Please try again.");
                }
            });}
        });
    }
    private void add(String label,String detail,String q){results.addView(txt(label+"\n"+detail+"\nDiscussion question: "+q,16,true));}
    private void toast(String x){LanguageManager.toast(this,x,Toast.LENGTH_LONG).show();}
}
