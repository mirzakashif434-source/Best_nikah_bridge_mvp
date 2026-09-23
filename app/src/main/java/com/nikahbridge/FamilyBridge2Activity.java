package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import org.json.JSONObject;

public class FamilyBridge2Activity extends Activity{
 LinearLayout root;EditText name,email,phone,linkId;TextView status;
 public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();load();}
 int dp(int v){return Premium2030Ui.dp(this,v);}
 EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);e.setPadding(dp(14),0,dp(14),0);e.setBackground(Premium2030Ui.outlined(this,android.graphics.Color.WHITE,Premium2030Ui.GOLD_SOFT,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.setMargins(0,dp(4),0,dp(4));root.addView(e,lp);return e;}
 Button btn(String s,boolean primary){Button b=primary?Premium2030Ui.primary(this,s):Premium2030Ui.secondary(this,s);Premium2030Ui.addButton(root,b);return b;}
 void render(){
   ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(Premium2030Ui.CREAM);
   root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));s.addView(root);setContentView(s);
   root.addView(Premium2030Ui.title(this,"Family / Wali Connect"));
   root.addView(Premium2030Ui.subtitle(this,"Because family matters in a halal journey."));
   root.addView(Premium2030Ui.heroLine(this,"Build with blessings • Keep your family informed"));
   Button circle=btn("Open Best Nikah Family Circle",true);
   circle.setOnClickListener(v->startActivity(new Intent(this,FamilyCircleActivity.class)));
   root.addView(Premium2030Ui.section(this,"Invite Your Wali"));
   name=input("Wali name");email=input("Wali email");phone=input("Wali phone (E.164)");
   Button req=btn("Request Wali Connection",true);req.setOnClickListener(v->create());
   root.addView(Premium2030Ui.section(this,"Manage Existing Connection"));
   linkId=input("Family link ID");
   Button verify=btn("Verify Wali Connection",false);verify.setOnClickListener(v->verify());
   Button revoke=btn("Revoke Connection",false);revoke.setOnClickListener(v->revoke());
   status=Premium2030Ui.subtitle(this,"Status: loading…");status.setGravity(android.view.Gravity.START);root.addView(status);
   Button back=btn("Back",false);back.setOnClickListener(v->finish());
 }
 void load(){AzureApiClient.get("/family-links",new AzureApiClient.Callback(){public void ok(int c,String s){runOnUiThread(()->status.setText("Status: Azure family links loaded"));}public void err(String e){runOnUiThread(()->status.setText("Status: Azure family links unavailable"));}});}
 void create(){try{JSONObject b=new JSONObject().put("waliName",name.getText().toString().trim()).put("waliEmail",email.getText().toString().trim()).put("waliPhoneE164",phone.getText().toString().trim());AzureApiClient.post("/family-links",b.toString(),cb("Wali connection request stored in Azure."));}catch(Exception e){status.setText("Status: request error");}}
 void verify(){AzureApiClient.post("/family-links/"+linkId.getText().toString().trim()+"/verify","{}",cb("Wali connection verified in Azure."));}
 void revoke(){AzureApiClient.delete("/family-links/"+linkId.getText().toString().trim(),"{}",cb("Wali connection revoked in Azure."));}
 AzureApiClient.Callback cb(String ok){return new AzureApiClient.Callback(){public void ok(int c,String s){runOnUiThread(()->status.setText(ok));}public void err(String e){runOnUiThread(()->status.setText("Status: Azure action failed"));}};}
}
