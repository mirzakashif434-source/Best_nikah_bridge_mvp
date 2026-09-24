package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class NikahAssistantActivity extends Activity{
 EditText question;TextView answer;Button ask;final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED;
 private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);PremiumFeatureGate.require(this,"aiNikahAssistant","VIP 60 SAR",this::render);}
 TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);b.setMinHeight(dp(56));return b;}
 void render(){
   ScrollView sc=new ScrollView(this);sc.setFillViewport(true);
   LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));sc.addView(root);setContentView(sc);
   TextView h=txt("AI Nikah Assistant",27,true);h.setGravity(Gravity.CENTER);root.addView(h);
   root.addView(txt("Real Azure AI guidance for respectful marriage preparation, family communication, safety and compatibility. It does not issue religious rulings.",15,false));
   question=new EditText(this);question.setHint("Ask your Nikah question…");question.setMinHeight(dp(120));root.addView(question,new LinearLayout.LayoutParams(-1,-2));
   ask=btn("Ask AI Nikah Assistant",true);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.setMargins(0,dp(8),0,dp(8));root.addView(ask,ap);
   answer=txt("Your Azure AI answer will appear here.",16,false);answer.setTextIsSelectable(true);root.addView(answer);
   Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,-2));back.setOnClickListener(v->finish());ask.setOnClickListener(v->askAI());
 }
 boolean authError(String message){if(message==null)return false;return message.contains("AZURE_SIGN_IN_REQUIRED")||message.contains("AZURE_INTERACTION_REQUIRED")||message.contains("UNAUTHENTICATED")||message.contains("ERR_JWT_EXPIRED")||message.contains("AZURE_AUTH");}
 void showSignInRecovery(){LanguageManager.dialog(this).setTitle("Sign in required").setMessage("Your Azure session needs to be refreshed before using the real AI Nikah Assistant.").setPositiveButton("Sign in",(d,w)->startActivity(new Intent(this,AzureExternalAuthActivity.class))).setNegativeButton("Not now",null).show();}
 void askAI(){String q=question.getText().toString().trim();if(q.isEmpty()){answer.setText("Please enter a question first.");return;}if(q.length()>4000){answer.setText("Please shorten your question to 4000 characters or less.");return;}if(!AzureAuthManager.hasAccount(this)){showSignInRecovery();return;}ask.setEnabled(false);answer.setText("Thinking…");try{JSONObject m=new JSONObject();m.put("role","user");m.put("content",q);JSONArray a=new JSONArray().put(m);JSONObject body=new JSONObject().put("messages",a);AzureApiClient.post("/ai/nikah-assistant",body.toString(),new AzureApiClient.Callback(){public void ok(int c,String s){runOnUiThread(()->{try{answer.setText(new JSONObject(s).getJSONObject("assistant").getString("content"));}catch(Exception e){answer.setText("Azure AI returned an invalid response.");}ask.setEnabled(true);});}public void err(String e){runOnUiThread(()->{if(authError(e)){answer.setText("Sign in with Azure to continue.");showSignInRecovery();}else answer.setText("Azure AI service is temporarily unavailable. Please try again.");ask.setEnabled(true);});}});}catch(Exception e){ask.setEnabled(true);answer.setText("Could not prepare the request.");}}
}
