package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class FamilyBridge2Activity extends Activity{
 LinearLayout root,ownedLinksBox,pendingWaliBox;
 EditText name,email,phone;
 TextView status;

 public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();loadAll();}

 int dp(int v){return Premium2030Ui.dp(this,v);}
 EditText input(String h){
   EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);
   e.setPadding(dp(14),0,dp(14),0);
   e.setBackground(Premium2030Ui.outlined(this,android.graphics.Color.WHITE,Premium2030Ui.GOLD_SOFT,16));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.setMargins(0,dp(4),0,dp(4));root.addView(e,lp);return e;
 }
 Button btn(String s,boolean primary){Button b=primary?Premium2030Ui.primary(this,s):Premium2030Ui.secondary(this,s);Premium2030Ui.addButton(root,b);return b;}
 Button boxButton(LinearLayout box,String label,boolean primary){
   Button b=primary?Premium2030Ui.primary(this,label):Premium2030Ui.secondary(this,label);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(4),0,dp(6));box.addView(b,lp);return b;
 }

 void render(){
   ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(Premium2030Ui.CREAM);
   root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));s.addView(root);setContentView(s);
   androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(s,(v,insets)->{
     androidx.core.graphics.Insets bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
     v.setPadding(bars.left,bars.top,bars.right,0);
     root.setPadding(dp(18),dp(20),dp(18),dp(30)+bars.bottom);
     return insets;
   });
   androidx.core.view.ViewCompat.requestApplyInsets(s);

   root.addView(Premium2030Ui.title(this,"Family / Wali Connect"));
   root.addView(Premium2030Ui.subtitle(this,"Because family matters in a halal journey."));
   root.addView(Premium2030Ui.heroLine(this,"Build with blessings • Keep your family informed"));

   Button circle=btn("Open Best Nikah Family Circle",true);
   circle.setOnClickListener(v->startActivity(new Intent(this,FamilyCircleActivity.class)));

   root.addView(Premium2030Ui.section(this,"Invite Your Wali"));
   name=input("Wali name");
   email=input("Wali email (optional)");
   phone=input("Wali phone with country code (optional)");
   Button req=btn("Request Wali Connection",true);req.setOnClickListener(v->create());

   root.addView(Premium2030Ui.section(this,"My Wali Connections"));
   ownedLinksBox=new LinearLayout(this);ownedLinksBox.setOrientation(LinearLayout.VERTICAL);root.addView(ownedLinksBox);

   root.addView(Premium2030Ui.section(this,"Wali Requests Sent to Me"));
   pendingWaliBox=new LinearLayout(this);pendingWaliBox.setOrientation(LinearLayout.VERTICAL);root.addView(pendingWaliBox);

   status=Premium2030Ui.subtitle(this,"Status: loading secure Azure Family/Wali data…");
   status.setGravity(android.view.Gravity.START);root.addView(status);

   Button refresh=btn("Refresh Family / Wali",false);refresh.setOnClickListener(v->loadAll());
   Button back=btn("Back",false);back.setOnClickListener(v->finish());
 }

 void loadAll(){
   if(!AzureAuthManager.hasAccount(this)){status.setText("Status: Azure sign in required");return;}
   status.setText("Status: loading secure Azure Family/Wali data…");
   loadOwnedLinks();
   loadPendingWaliRequests();
 }

 void loadOwnedLinks(){
   AzureApiClient.get("/family-links",new AzureApiClient.Callback(){
     public void ok(int c,String body){runOnUiThread(()->{
       ownedLinksBox.removeAllViews();
       try{
         JSONArray a=new JSONObject(body).optJSONArray("familyLinks");
         if(a==null||a.length()==0){
           ownedLinksBox.addView(Premium2030Ui.subtitle(FamilyBridge2Activity.this,"No active Family/Wali connection yet."));
         }else{
           for(int i=0;i<a.length();i++){
             JSONObject x=a.optJSONObject(i);if(x==null)continue;
             final String id=x.optString("id","").trim();
             String waliName=x.optString("wali_name","Wali").trim();
             String state=x.optString("status","pending").trim();
             String waliEmail=x.optString("wali_email","").trim();
             String waliPhone=x.optString("wali_phone_e164","").trim();
             StringBuilder label=new StringBuilder();
             label.append(waliName.isEmpty()?"Wali":waliName).append("\nStatus: ").append(state);
             if(!waliEmail.isEmpty())label.append("\nEmail: ").append(waliEmail);
             if(!waliPhone.isEmpty())label.append("\nPhone: ").append(waliPhone);
             ownedLinksBox.addView(Premium2030Ui.subtitle(FamilyBridge2Activity.this,label.toString()));
             if(!id.isEmpty()&&!"revoked".equalsIgnoreCase(state)){
               Button revoke=boxButton(ownedLinksBox,"Revoke This Wali Connection",false);
               revoke.setOnClickListener(v->confirmRevoke(id));
             }
           }
         }
         status.setText("Status: Azure Family/Wali connections loaded");
       }catch(Exception e){
         ownedLinksBox.addView(Premium2030Ui.subtitle(FamilyBridge2Activity.this,"Family/Wali connections could not be displayed."));
         status.setText("Status: Azure Family/Wali response could not be read");
       }
     });}
     public void err(String e){runOnUiThread(()->{
       ownedLinksBox.removeAllViews();
       ownedLinksBox.addView(Premium2030Ui.subtitle(FamilyBridge2Activity.this,"Family/Wali connections are temporarily unavailable."));
       status.setText("Status: Azure Family/Wali service unavailable");
     });}
   });
 }

 void loadPendingWaliRequests(){
   AzureApiClient.get("/family-links/pending-for-wali",new AzureApiClient.Callback(){
     public void ok(int c,String body){runOnUiThread(()->{
       pendingWaliBox.removeAllViews();
       try{
         JSONArray a=new JSONObject(body).optJSONArray("familyLinks");
         if(a==null||a.length()==0){
           pendingWaliBox.addView(Premium2030Ui.subtitle(FamilyBridge2Activity.this,"No Wali verification requests are waiting for this signed-in account."));
           return;
         }
         for(int i=0;i<a.length();i++){
           JSONObject x=a.optJSONObject(i);if(x==null)continue;
           final String id=x.optString("id","").trim();if(id.isEmpty())continue;
           String ownerName=x.optString("owner_name","Member").trim();
           String ownerEmail=x.optString("owner_email","").trim();
           String label="Request from: "+(ownerName.isEmpty()?"Member":ownerName);
           if(!ownerEmail.isEmpty())label+="\nAccount: "+ownerEmail;
           pendingWaliBox.addView(Premium2030Ui.subtitle(FamilyBridge2Activity.this,label));
           Button verify=boxButton(pendingWaliBox,"Verify This Wali Request",true);
           verify.setOnClickListener(v->verifyPending(id));
         }
       }catch(Exception e){
         pendingWaliBox.addView(Premium2030Ui.subtitle(FamilyBridge2Activity.this,"Wali requests could not be displayed."));
       }
     });}
     public void err(String e){runOnUiThread(()->{
       pendingWaliBox.removeAllViews();
       pendingWaliBox.addView(Premium2030Ui.subtitle(FamilyBridge2Activity.this,"Wali requests are temporarily unavailable."));
     });}
   });
 }

 void create(){
   try{
     String waliName=name.getText().toString().trim();
     String waliEmail=email.getText().toString().trim();
     String waliPhone=phone.getText().toString().trim();
     if(waliName.length()<2){LanguageManager.setError(name,"Enter Wali name");name.requestFocus();return;}
     if(waliEmail.isEmpty()&&waliPhone.isEmpty()){LanguageManager.setError(email,"Enter Wali email or phone number");email.requestFocus();return;}
     if(!waliPhone.isEmpty()&&!waliPhone.matches("^\\+[1-9]\\d{7,14}$")){
       LanguageManager.setError(phone,"Include country code, for example +9665XXXXXXXX");phone.requestFocus();return;
     }
     JSONObject b=new JSONObject().put("waliName",waliName).put("waliEmail",waliEmail).put("waliPhoneE164",waliPhone);
     status.setText("Status: sending secure Wali request…");
     AzureApiClient.post("/family-links",b.toString(),new AzureApiClient.Callback(){
       public void ok(int c,String s){runOnUiThread(()->{
         status.setText("Status: Wali connection request stored securely in Azure");
         name.setText("");email.setText("");phone.setText("");
         loadAll();
       });}
       public void err(String e){runOnUiThread(()->status.setText("Status: Wali connection request could not be saved")); }
     });
   }catch(Exception e){status.setText("Status: request error");}
 }

 void verifyPending(String id){
   status.setText("Status: verifying Wali request…");
   AzureApiClient.post("/family-links/"+id+"/verify","{}",new AzureApiClient.Callback(){
     public void ok(int c,String s){runOnUiThread(()->{
       status.setText("Status: Wali identity verified securely in Azure");
       loadAll();
     });}
     public void err(String e){runOnUiThread(()->status.setText("Status: Wali verification failed")); }
   });
 }

 void confirmRevoke(String id){
   LanguageManager.dialog(this)
     .setTitle("Revoke Wali connection?")
     .setMessage("This removes the selected Family/Wali connection from your active Azure records. It does not delete the Wali account.")
     .setNegativeButton("Cancel",null)
     .setPositiveButton("Revoke",(d,w)->revoke(id))
     .show();
 }

 void revoke(String id){
   status.setText("Status: revoking Wali connection…");
   AzureApiClient.delete("/family-links/"+id,"{}",new AzureApiClient.Callback(){
     public void ok(int c,String s){runOnUiThread(()->{
       status.setText("Status: Wali connection revoked in Azure");
       loadAll();
     });}
     public void err(String e){runOnUiThread(()->status.setText("Status: Wali connection could not be revoked")); }
   });
 }
}
