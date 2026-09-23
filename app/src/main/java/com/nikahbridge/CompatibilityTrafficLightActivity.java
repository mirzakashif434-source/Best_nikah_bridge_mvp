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

/** Azure compatibility visualization from the real server-side match engine. */
public class CompatibilityTrafficLightActivity extends Activity {
    private LinearLayout root; private TextView result;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}
    private void render(){
        base();root.addView(txt("Compatibility Traffic Light",27,true));root.addView(txt("Transparent Azure match data only. Green means stronger recorded alignment, yellow means discuss carefully, red means low recorded alignment. It is not a marriage verdict.",15,false));
        EditText uid=new EditText(this);uid.setHint("Real matched member ID");root.addView(uid,new LinearLayout.LayoutParams(-1,dp(62)));
        result=txt("",16,false);root.addView(result);
        Button check=btn("Check Real Compatibility",true);check.setOnClickListener(v->check(uid.getText().toString().trim()));
        Button trust=btn("Open Trust Passport",false);trust.setOnClickListener(v->startActivity(new Intent(this,TrustPassportActivity.class)));
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }
    private void check(String id){
        if(id.isEmpty()){result.setText("Enter a real member ID.");return;}
        result.setText("Loading real Azure compatibility…");
        AzureApiClient.get("/matches/"+id,new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject m=new JSONObject(body).optJSONObject("match");if(m==null){result.setText("Match data unavailable.");return;}
                    int score=m.optInt("compatibilityScore",0);
                    String state=score>=75?"GREEN — stronger recorded alignment":score>=45?"YELLOW — discuss carefully":"RED — low recorded alignment";
                    StringBuilder s=new StringBuilder(state+"\nCompatibility: "+score+"/100\n\n");
                    JSONArray a=m.optJSONArray("whyWeMatched");if(a!=null)for(int i=0;i<a.length();i++)s.append("• ").append(a.optString(i)).append("\n");
                    s.append("\nThis uses stated Azure profile/preferences only.");
                    result.setText(s.toString());
                }catch(Exception e){result.setText("Azure compatibility response could not be read.");}
            });}
            public void err(String message){runOnUiThread(()->result.setText("Compatibility unavailable: "+message));}
        });
    }
}
