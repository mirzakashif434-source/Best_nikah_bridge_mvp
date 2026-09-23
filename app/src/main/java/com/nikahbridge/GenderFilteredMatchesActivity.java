package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real gender-filtered matching view backed by Azure reciprocal match rules. */
public class GenderFilteredMatchesActivity extends Activity {
    private LinearLayout root;
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    @Override protected void onCreate(Bundle state){super.onCreate(state);AzureAuthManager.bindActivity(this);show();}
    private void show(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(Color.rgb(247,250,249));sc.addView(root);setContentView(sc);
        TextView title=text("Gender-Filtered Nikah Matches",27,true);title.setGravity(Gravity.CENTER);root.addView(title);
        root.addView(text("Real Azure profiles only. Reciprocal age, gender/preference, privacy and block rules are enforced by the server.",15,false));
        load();
        Button back=new Button(this);back.setText("Back");back.setAllCaps(false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }
    private void load(){
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("matches");
                    if(a==null||a.length()==0){root.addView(text("No eligible real profiles are available yet.",16,false));return;}
                    for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m==null)continue;root.addView(text(m.optString("displayName","Member")+" • "+m.optInt("age",0)+" • "+m.optString("gender","")+"\n"+m.optString("country","")+" • Compatibility "+m.optInt("compatibilityScore",0)+"/100",18,true));}
                }catch(Exception e){root.addView(text("Azure matches could not be read.",16,false));}
            });}
            public void err(String message){runOnUiThread(()->root.addView(text("Could not load real Azure matches: "+message,16,false)));}
        });
    }
    private TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(bold?Color.rgb(30,45,41):Color.rgb(95,108,103));t.setPadding(dp(6),dp(8),dp(6),dp(12));if(bold)t.setTypeface(null,1);return t;}
}
