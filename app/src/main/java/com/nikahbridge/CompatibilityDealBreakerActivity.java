package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real deal-breaker view using the Azure match engine and PostgreSQL profile data. */
public class CompatibilityDealBreakerActivity extends Activity {
    private LinearLayout root,matchChoices; private TextView result,matchStatus; private String selectedMatchId="";
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;

    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);PremiumFeatureGate.require(this,"advancedMatching","Plus 40 SAR or VIP 60 SAR",this::render);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(light);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);
        s.addView(root);setContentView(s);
        ViewCompat.setOnApplyWindowInsetsListener(s,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(20),dp(22),dp(20),dp(30)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(s);
    }
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}
    private Button choiceBtn(String x){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(green);GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(18));g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private void render(){base();root.addView(txt("Compatibility Deal-Breaker Engine",27,true));root.addView(txt("Checks real Azure match data. It does not invent hidden traits or label a person as good or bad.",15,false));matchStatus=txt("Loading your real Azure matches…",15,false);root.addView(matchStatus);matchChoices=new LinearLayout(this);matchChoices.setOrientation(LinearLayout.VERTICAL);root.addView(matchChoices);Button check=btn("Check Selected Deal-Breakers",true);check.setOnClickListener(v->check());result=txt("",16,false);root.addView(result);Button back=btn("Back",false);back.setOnClickListener(v->finish());loadMatches();}
    private void loadMatches(){
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("matches");
                    matchChoices.removeAllViews();
                    if(a==null||a.length()==0){matchStatus.setText("No real compatible matches are available yet.");return;}
                    matchStatus.setText("Choose a real match. Secure member IDs stay hidden.");
                    for(int i=0;i<Math.min(a.length(),50);i++){
                        JSONObject m=a.optJSONObject(i);if(m==null)continue;
                        String id=m.optString("userId","").trim();if(id.isEmpty())continue;
                        String name=m.optString("displayName","Member").trim();
                        final String matchId=id,matchName=name.isEmpty()?"Member":name;
                        Button choose=choiceBtn("Choose "+matchName);
                        matchChoices.addView(choose,new LinearLayout.LayoutParams(-1,dp(56)));
                        choose.setOnClickListener(v->{selectedMatchId=matchId;matchStatus.setText("Selected: "+matchName);});
                    }
                }catch(Exception e){matchStatus.setText("Real Azure matches could not be displayed.");}
            });}
            public void err(String message){runOnUiThread(()->matchStatus.setText("Real Azure matches are temporarily unavailable."));}
        });
    }

    private void check(){
        String other=selectedMatchId.trim();
        if(!AzureAuthManager.hasAccount(this)){
            LanguageManager.dialog(this)
                .setTitle("Sign in required")
                .setMessage("Sign in with Azure to check real compatibility deal-breakers.")
                .setPositiveButton("Sign in",(d,w)->startActivity(new Intent(this,AzureExternalAuthActivity.class)))
                .setNegativeButton("Not now",null)
                .show();
            return;
        }
        if(other.isEmpty()){result.setText("Choose a real match first.");return;}
        result.setText("Checking real Azure match data…");
        AzureApiClient.get("/matches/"+other,new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject m=new JSONObject(body).optJSONObject("match");
                    if(m==null){result.setText("Match data unavailable.");return;}
                    JSONArray reasons=m.optJSONArray("whyWeMatched");
                    boolean noConflict=false;
                    StringBuilder reasonText=new StringBuilder();
                    if(reasons!=null)for(int i=0;i<reasons.length();i++){String r=reasons.optString(i,"");if(r.toLowerCase().contains("deal-breaker"))noConflict=true;if(!r.isEmpty())reasonText.append("• ").append(r).append("\n");}
                    result.setText((noConflict?"NO DETECTED DEAL-BREAKER CONFLICT":"DEAL-BREAKER CLEARANCE NOT CONFIRMED")
                            +"\n\nCompatibility: "+m.optInt("compatibilityScore",0)+"/100\n"
                            +"Member: "+m.optString("displayName","Member")+"\n\n"
                            +(reasonText.length()==0?"No safe match reasons were returned.":reasonText.toString())
                            +"\nThis uses recorded Azure profile/preferences only and is not a guarantee of compatibility.");
                }catch(Exception e){result.setText("Azure match response could not be read.");}
            });}
            @Override public void err(String message){runOnUiThread(()->result.setText("Deal-breaker check unavailable: "+message));}
        });
    }
}
