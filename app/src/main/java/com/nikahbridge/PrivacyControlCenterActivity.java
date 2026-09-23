package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONObject;

public class PrivacyControlCenterActivity extends Activity{
 LinearLayout root;Switch discoverable,city,photos;TextView status;
 public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();load();}
 int dp(int v){return Premium2030Ui.dp(this,v);}
 Button btn(String s,boolean primary){Button b=primary?Premium2030Ui.primary(this,s):Premium2030Ui.secondary(this,s);Premium2030Ui.addButton(root,b);return b;}
 void addSwitch(String label,Switch sw){
   LinearLayout card=Premium2030Ui.card(this);
   sw.setText(label);sw.setTextSize(16);sw.setTextColor(Premium2030Ui.TEXT);
   card.addView(sw,new LinearLayout.LayoutParams(-1,dp(54)));root.addView(card);
 }
 void render(){
   ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Premium2030Ui.CREAM);
   root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));sc.addView(root);setContentView(sc);
   root.addView(Premium2030Ui.title(this,"Privacy Control Center"));
   root.addView(Premium2030Ui.subtitle(this,"Your privacy. Your choice. Securely stored in Azure."));
   root.addView(Premium2030Ui.heroLine(this,"Private by design • Serious by intention"));
   discoverable=new Switch(this);city=new Switch(this);photos=new Switch(this);
   addSwitch("Show my profile in matching",discoverable);
   addSwitch("Show my city on my public profile",city);
   addSwitch("Show my approved photos to eligible matches",photos);
   Button save=btn("Save Privacy Controls",true);save.setOnClickListener(v->save());
   Button refresh=btn("Refresh",false);refresh.setOnClickListener(v->load());
   status=Premium2030Ui.subtitle(this,"Status: loading…");status.setGravity(android.view.Gravity.START);root.addView(status);
   Button back=btn("Back",false);back.setOnClickListener(v->finish());
 }
 void load(){AzureApiClient.get("/privacy",new AzureApiClient.Callback(){public void ok(int c,String s){runOnUiThread(()->{try{JSONObject p=new JSONObject(s).getJSONObject("privacy");discoverable.setChecked(p.optBoolean("profile_discoverable",true));city.setChecked(p.optBoolean("show_city",true));photos.setChecked(p.optBoolean("show_photo_to_matches",true));status.setText("Status: Azure privacy loaded");}catch(Exception e){status.setText("Status: Azure response error");}});}public void err(String e){runOnUiThread(()->status.setText("Status: Azure privacy unavailable"));}});}
 void save(){try{JSONObject b=new JSONObject().put("profileDiscoverable",discoverable.isChecked()).put("showCity",city.isChecked()).put("showPhotoToMatches",photos.isChecked());AzureApiClient.patch("/privacy",b.toString(),new AzureApiClient.Callback(){public void ok(int c,String s){runOnUiThread(()->status.setText("Status: saved in Azure"));}public void err(String e){runOnUiThread(()->status.setText("Status: Azure save failed"));}});}catch(Exception e){status.setText("Status: request error");}}
}
