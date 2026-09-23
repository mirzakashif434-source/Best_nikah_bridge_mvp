package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explains real Azure match-engine reasons. No fabricated members or reasons. */
public class WhyWeMatchedActivity extends Activity {
    private LinearLayout root;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);show();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}
    private void show(){
        base();TextView title=txt("Why We Matched",27,true);title.setGravity(Gravity.CENTER);root.addView(title);root.addView(txt("Actual Azure profile factors explain each real match. No AI-generated people or reasons are used.",15,false));
        Button traffic=btn("Compatibility Traffic Light",true);traffic.setOnClickListener(v->startActivity(new Intent(this,CompatibilityTrafficLightActivity.class)));
        load();
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }
    private void load(){
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("matches");
                    if(a==null||a.length()==0){root.addView(txt("No qualifying real Azure matches are available yet.",16,false));return;}
                    for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m==null)continue;StringBuilder reasons=new StringBuilder();JSONArray r=m.optJSONArray("whyWeMatched");if(r!=null)for(int j=0;j<r.length();j++)reasons.append("✓ ").append(r.optString(j)).append("\n");root.addView(txt(m.optString("displayName","Member")+" • "+m.optInt("age",0)+" • "+m.optInt("compatibilityScore",0)+"/100",21,true));root.addView(txt(reasons.length()==0?"No safe reason details were returned.":reasons.toString(),15,false));root.addView(txt("This explanation uses stated Azure profile data only; it is not a guarantee of compatibility.",14,false));}
                }catch(Exception e){root.addView(txt("Azure match explanation could not be read.",16,false));}
            });}
            public void err(String message){runOnUiThread(()->root.addView(txt("Could not load Azure matches: "+message,16,false)));}
        });
    }
}
