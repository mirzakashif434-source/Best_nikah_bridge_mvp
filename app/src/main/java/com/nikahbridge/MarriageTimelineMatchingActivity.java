package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real timeline matching using Azure match results and stated marriage timelines. */
public class MarriageTimelineMatchingActivity extends Activity {
    private LinearLayout root;
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;
    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void render(){
        ScrollView s=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);s.addView(root);setContentView(s);
        root.addView(txt("Marriage Timeline Matching",27,true));root.addView(txt("Real Azure matches only. Timelines are stated by members; the app does not predict readiness.",15,false));
        Button find=btn("Find Real Timeline Matches",true);find.setOnClickListener(v->find());
        Button questions=btn("Smart Serious Questions",false);questions.setOnClickListener(v->startActivity(new Intent(this,SmartSeriousQuestionsActivity.class)));
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }
    private TextView txt(String x,int z,boolean b){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(b?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}
    private void find(){
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("matches");boolean any=false;
                    if(a!=null)for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m==null)continue;String tl=m.optString("marriageTimeline","").trim();if(tl.isEmpty())continue;any=true;root.addView(txt("✓ "+m.optString("displayName","Member")+" • Age "+m.optInt("age",0)+"\nStated timeline: "+tl+"\nCompatibility: "+m.optInt("compatibilityScore",0)+"/100",16,true));}
                    if(!any)root.addView(txt("No real Azure matches with a stated marriage timeline are available yet.",16,false));
                }catch(Exception e){root.addView(txt("Azure timeline matches could not be read.",16,false));}
            });}
            public void err(String message){runOnUiThread(()->root.addView(txt("Could not load Azure timeline matches: "+message,16,false)));}
        });
    }
}
