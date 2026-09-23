package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenderFilteredMatchesActivity extends Activity {
    private LinearLayout root;
    private int dp(int v){return Premium2030Ui.dp(this,v);}
    @Override protected void onCreate(Bundle state){super.onCreate(state);AzureAuthManager.bindActivity(this);show();}
    private void show(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Premium2030Ui.CREAM);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));sc.addView(root);setContentView(sc);
        root.addView(Premium2030Ui.title(this,"Discover"));
        root.addView(Premium2030Ui.subtitle(this,"Serious people • Real intentions • Verified Azure profiles"));
        root.addView(Premium2030Ui.heroLine(this,"Meaningful matches for a brighter halal future"));
        load();
        Button back=Premium2030Ui.secondary(this,"Back");Premium2030Ui.addButton(root,back);back.setOnClickListener(v->finish());
    }
    private void load(){
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("matches");
                    if(a==null||a.length()==0){root.addView(Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,"No eligible real profiles are available yet."));return;}
                    for(int i=0;i<a.length();i++){
                        JSONObject m=a.optJSONObject(i);if(m==null)continue;
                        LinearLayout card=Premium2030Ui.card(GenderFilteredMatchesActivity.this);
                        card.addView(Premium2030Ui.chip(GenderFilteredMatchesActivity.this,"VERIFIED MATCH"));
                        card.addView(Premium2030Ui.section(GenderFilteredMatchesActivity.this,m.optString("displayName","Member")+" • "+m.optInt("age",0)));
                        TextView d=Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,
                            m.optString("country","")+"\nCompatibility "+m.optInt("compatibilityScore",0)+"/100");
                        d.setGravity(android.view.Gravity.START);d.setPadding(0,0,0,dp(4));card.addView(d);root.addView(card);
                    }
                }catch(Exception e){root.addView(Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,"Azure matches could not be read."));}
            });}
            public void err(String message){runOnUiThread(()->root.addView(Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,"Could not load real Azure matches: "+message)));}
        });
    }
}
