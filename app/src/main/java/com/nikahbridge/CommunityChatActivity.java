package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Best Nikah Bridge - production Global Community Chat. */
public class CommunityChatActivity extends Activity {
    private final int green = Color.rgb(18,103,82), dark = Color.rgb(30,45,41), gray = Color.rgb(95,108,103), light = Color.rgb(247,250,249);
    private FirebaseAuth auth; private FirebaseFirestore db; private FirebaseFunctions functions; private ListenerRegistration listener;
    private LinearLayout messages; private EditText composer; private ScrollView scroll; private final Set<String> mutedUids = new HashSet<>();
    private final Handler azureHandler = new Handler(Looper.getMainLooper());
    private final Runnable azurePoll = new Runnable(){ @Override public void run(){ loadAzureCommunity(); azureHandler.postDelayed(this,5000); } };
    @Override protected void onCreate(Bundle state) { super.onCreate(state); if(!AzureAuthManager.hasAccount(this)){finish();return;} build(); loadAzureMutes(); loadAzureCommunity(); azureHandler.postDelayed(azurePoll,5000); }
    @Override protected void onDestroy(){ azureHandler.removeCallbacksAndMessages(null); if(listener!=null) listener.remove(); super.onDestroy(); }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(dp(8),dp(5),dp(8),dp(5));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,boolean filled){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(filled?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(filled?green:Color.WHITE);g.setCornerRadius(dp(18));if(!filled)g.setStroke(dp(1),green);b.setBackground(g);b.setPadding(dp(8),0,dp(8),0);return b;}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(18),dp(16),dp(12));root.setBackgroundColor(light);
        TextView title=text("Global Community Chat",27,true);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,dp(52)));
        root.addView(text("FREE • All countries • Nikah-focused community\nNo phone numbers, passwords, OTPs, money requests or private contact details.",14,false));
        scroll=new ScrollView(this);scroll.setFillViewport(true);messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);messages.setPadding(0,dp(8),0,dp(10));scroll.addView(messages);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);composer=new EditText(this);composer.setHint("Write a respectful Nikah-community message…");composer.setTextSize(15);composer.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE);composer.setMaxLines(4);composer.setPadding(dp(12),dp(8),dp(12),dp(8));row.addView(composer,new LinearLayout.LayoutParams(0,dp(62),1f));Button send=button("Send",true);row.addView(send,new LinearLayout.LayoutParams(dp(92),dp(58)));root.addView(row);
        Button safety=button("My Chat Safety",false);LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(52));slp.setMargins(0,dp(6),0,0);root.addView(safety,slp);
        Button back=button("Back",false);LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,dp(52));blp.setMargins(0,dp(6),0,0);root.addView(back,blp);setContentView(root);
        send.setOnClickListener(v->sendMessageAzure());safety.setOnClickListener(v->showSafetyAzure());back.setOnClickListener(v->finish());
    }
    /** Primary production path: Azure External ID -> Azure Function -> PostgreSQL. Firebase chat remains below for rollback. */
    private void loadAzureMutes(){
        if(!AzureAuthManager.hasAccount(this)) return;
        AzureApiClient.get("/community/mutes",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){try{JSONArray a=new JSONObject(body).optJSONArray("mutedUids");mutedUids.clear();if(a!=null)for(int i=0;i<a.length();i++)mutedUids.add(a.getString(i));loadAzureCommunity();}catch(Exception e){toast("Azure safety settings could not be read.");}}
            @Override public void err(String message){toast("Azure safety settings failed.");}
        });
    }
    private void loadAzureCommunity(){
        if(!AzureAuthManager.hasAccount(this)) return;
        AzureApiClient.get("/community/messages",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){try{JSONArray a=new JSONObject(body).optJSONArray("messages");messages.removeAllViews();if(a!=null)for(int i=0;i<a.length();i++)addAzureMessageCard(a.getJSONObject(i));scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}catch(Exception e){toast("Community data could not be read.");}}
            @Override public void err(String message){if(messages.getChildCount()==0)toast("Azure Community Chat could not be loaded.");}
        });
    }
    private void addAzureMessageCard(JSONObject d){
        String author=d.optString("author_name","Member"),country=d.optString("country",""),body=d.optString("text",""),uid=d.optString("author_uid","");
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(16));card.setBackground(bg);
        card.addView(text(author+(country.isEmpty()?"":" • "+country),15,true));card.addView(text(body,16,false));
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.END);Button report=button("Report",false),mute=button("Mute",false),block=button("Block",false);actions.addView(report,new LinearLayout.LayoutParams(dp(86),dp(44)));actions.addView(mute,new LinearLayout.LayoutParams(dp(78),dp(44)));actions.addView(block,new LinearLayout.LayoutParams(dp(82),dp(44)));card.addView(actions);
        report.setOnClickListener(v->reportAzureMessage(d));mute.setOnClickListener(v->muteAzureUser(uid,author));block.setOnClickListener(v->blockAzureUser(uid,author));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(5));messages.addView(card,lp);
    }
    private void sendMessageAzure(){
        String value=composer.getText().toString().trim();if(value.isEmpty()){composer.setError("Write a message first");return;}if(value.length()>500){composer.setError("Maximum 500 characters");return;}
        JSONObject b=new JSONObject();try{b.put("text",value);}catch(Exception ignored){}
        AzureApiClient.post("/community/messages",b.toString(),new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){runOnUiThread(()->{composer.setText("");toast("Message sent to the real Azure community.");loadAzureCommunity();});}
            @Override public void err(String message){runOnUiThread(()->toast("Azure message failed: "+message));}
        });
    }
    private void reportAzureMessage(JSONObject d){
        final String[] reasons={"Spam / repeated promotion","Scam / money request","Harassment / abuse","Inappropriate content","Private contact details","Other"};
        new AlertDialog.Builder(this).setTitle("Report community message").setItems(reasons,(dialog,which)->{
            try{JSONObject b=new JSONObject();b.put("messageId",d.optString("id"));b.put("reportedUid",d.optString("author_uid"));b.put("reason",reasons[which]);AzureApiClient.post("/community/reports",b.toString(),new AzureApiClient.Callback(){public void ok(int code,String body){toast("Report sent securely to Azure moderation.");}public void err(String message){toast("Report could not be submitted.");}});}catch(Exception e){toast("Report could not be submitted.");}
        }).show();
    }
    private void muteAzureUser(String uid,String name){
        if(uid.isEmpty())return;try{JSONObject b=new JSONObject();b.put("userId",uid);AzureApiClient.post("/community/mutes",b.toString(),new AzureApiClient.Callback(){public void ok(int code,String body){mutedUids.add(uid);loadAzureCommunity();toast(name+" muted in Azure Community Chat.");}public void err(String message){toast("Mute could not be saved.");}});}catch(Exception e){toast("Mute could not be saved.");}
    }
    private void blockAzureUser(String uid,String name){
        if(uid.isEmpty())return;new AlertDialog.Builder(this).setTitle("Block "+name+"?").setMessage("This is a real Azure safety action and blocks the member.").setNegativeButton("Cancel",null).setPositiveButton("Block",(dialog,which)->{try{JSONObject b=new JSONObject();b.put("userId",uid);AzureApiClient.post("/blocks",b.toString(),new AzureApiClient.Callback(){public void ok(int code,String body){mutedUids.add(uid);loadAzureCommunity();toast("Member blocked securely in Azure.");}public void err(String message){toast("Block failed: "+message);}});}catch(Exception e){toast("Block failed.");}}).show();
    }
    private void showSafetyAzure(){new AlertDialog.Builder(this).setTitle("My Chat Safety").setItems(new String[]{"Refresh my safety settings","Unmute a member"},(d,w)->{if(w==0){loadAzureMutes();toast("Azure safety settings refreshed.");}else showAzureUnmute();}).show();}
    private void showAzureUnmute(){
        if(mutedUids.isEmpty()){toast("No muted members.");return;}String[] ids=mutedUids.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Select member to unmute").setItems(ids,(d,w)->{String id=ids[w];AzureApiClient.delete("/community/mutes/"+id,"",new AzureApiClient.Callback(){public void ok(int code,String body){mutedUids.remove(id);loadAzureCommunity();toast("Member unmuted in Azure.");}public void err(String message){toast("Unmute failed.");}});}).show();
    }

    private void loadMuted(){FirebaseUser u=auth.getCurrentUser();if(u==null)return;functions.getHttpsCallable("listMyCommunityMutes").call(new HashMap<>()).addOnSuccessListener(r->{Object raw=r.getData();if(raw instanceof Map){Object list=((Map<?,?>)raw).get("mutedUids");if(list instanceof java.util.List)for(Object id:(java.util.List<?>)list)mutedUids.add(String.valueOf(id));listen();}});}
    private void listen(){if(listener!=null)listener.remove();listener=db.collection("communityMessages").orderBy("createdAt",Query.Direction.ASCENDING).limitToLast(100).addSnapshotListener((snap,error)->{if(error!=null||snap==null){toast("Community chat could not be loaded.");return;}messages.removeAllViews();for(DocumentSnapshot d:snap.getDocuments()){String uid=d.getString("authorUid");String moderation=d.getString("moderationStatus");if("removed".equals(moderation)||(uid!=null&&mutedUids.contains(uid)))continue;addMessageCard(d);}scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));});}
    private void addMessageCard(DocumentSnapshot d){String author=d.getString("authorName"),country=d.getString("country"),body=d.getString("text"),uid=d.getString("authorUid");if(author==null)author="Member";if(country==null)country="";if(body==null)body="";LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(16));card.setBackground(bg);card.addView(text(author+(country.isEmpty()?"":" • "+country),15,true));card.addView(text(body,16,false));LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.END);Button report=button("Report",false),mute=button("Mute",false),block=button("Block",false);actions.addView(report,new LinearLayout.LayoutParams(dp(86),dp(44)));actions.addView(mute,new LinearLayout.LayoutParams(dp(78),dp(44)));actions.addView(block,new LinearLayout.LayoutParams(dp(82),dp(44)));card.addView(actions);final String finalAuthor=author;report.setOnClickListener(v->reportMessage(d));mute.setOnClickListener(v->muteUser(uid,finalAuthor));block.setOnClickListener(v->blockUser(uid,finalAuthor));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(5));messages.addView(card,lp);}
    private void sendMessage(){String value=composer.getText().toString().trim();if(value.isEmpty()){composer.setError("Write a message first");return;}if(value.length()>500){composer.setError("Maximum 500 characters");return;}Map<String,Object> data=new HashMap<>();data.put("text",value);functions.getHttpsCallable("sendCommunityMessage").call(data).addOnSuccessListener(r->{composer.setText("");toast("Message sent to the real community.");}).addOnFailureListener(e->toast(e.getMessage()==null?"Message could not be sent.":e.getMessage()));}
    private void reportMessage(DocumentSnapshot d){FirebaseUser u=auth.getCurrentUser();if(u==null)return;String reportedUid=d.getString("authorUid");if(reportedUid==null||reportedUid.equals(u.getUid())){toast("You cannot report your own message.");return;}final String[] reasons={"Spam / repeated promotion","Scam / money request","Harassment / abuse","Inappropriate content","Private contact details","Other"};new AlertDialog.Builder(this).setTitle("Report community message").setItems(reasons,(dialog,which)->{Map<String,Object> r=new HashMap<>();r.put("messageId",d.getId());r.put("reportedUid",reportedUid);r.put("reason",reasons[which]);functions.getHttpsCallable("submitCommunityReport").call(r).addOnSuccessListener(x->toast("Report sent securely to moderation.")).addOnFailureListener(x->toast("Report could not be submitted."));}).show();}
    private void muteUser(String uid,String name){if(uid==null||uid.equals(auth.getUid()))return;Map<String,Object> data=new HashMap<>();data.put("targetUid",uid);functions.getHttpsCallable("muteCommunityUser").call(data).addOnSuccessListener(x->{mutedUids.add(uid);listen();toast(name+" muted in Community Chat.");}).addOnFailureListener(x->toast("Mute could not be saved."));}
    private void blockUser(String uid,String name){if(uid==null||uid.equals(auth.getUid()))return;new AlertDialog.Builder(this).setTitle("Block "+name+"?").setMessage("Blocking is a real safety action and will stop the active connection if one exists.").setNegativeButton("Cancel",null).setPositiveButton("Block",(dialog,which)->{Map<String,Object> data=new HashMap<>();data.put("blockedUid",uid);functions.getHttpsCallable("blockUser").call(data).addOnSuccessListener(x->{mutedUids.add(uid);listen();toast("Member blocked securely.");}).addOnFailureListener(x->toast("Block failed."));}).show();}
    private void showSafety(){String[] choices={"Unmute a member","Refresh my safety settings"};new AlertDialog.Builder(this).setTitle("My Chat Safety").setItems(choices,(d,w)->{if(w==0)showUnmute();else{loadMuted();toast("Safety settings refreshed.");}}).show();}
    private void showUnmute(){if(mutedUids.isEmpty()){toast("No muted members.");return;}String[] ids=mutedUids.toArray(new String[0]);new AlertDialog.Builder(this).setTitle("Select member to unmute").setItems(ids,(d,w)->{String id=ids[w];Map<String,Object> data=new HashMap<>();data.put("targetUid",id);functions.getHttpsCallable("unmuteCommunityUser").call(data).addOnSuccessListener(x->{mutedUids.remove(id);listen();toast("Member unmuted.");}).addOnFailureListener(x->toast("Unmute failed."));}).show();}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
}
