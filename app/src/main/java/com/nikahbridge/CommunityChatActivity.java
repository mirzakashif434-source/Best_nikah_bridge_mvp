package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Best Nikah Bridge - production Global Community Chat. */
public class CommunityChatActivity extends Activity {
    private final int green = Premium2030Ui.GREEN, dark = Premium2030Ui.TEXT, gray = Premium2030Ui.MUTED, light = Premium2030Ui.CREAM;
    private LinearLayout messages; private EditText composer; private ScrollView scroll; private final Set<String> mutedUids = new HashSet<>();
    private final Handler azureHandler = new Handler(Looper.getMainLooper());
    private boolean chatBuilt=false;
    private final Runnable azurePoll = new Runnable(){ @Override public void run(){ if(chatBuilt){ loadAzureCommunity(); azureHandler.postDelayed(this,5000); } } };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        buildAuthLoading();
        openChatWithFreshAzureSession();
    }
    @Override protected void onDestroy(){ azureHandler.removeCallbacksAndMessages(null); super.onDestroy(); }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    private void buildAuthLoading(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(28),dp(18),dp(28));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(light);
        TextView title=text("Global Community Chat",27,true); title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(64)));
        TextView info=text("Checking your secure Azure session…",16,false); info.setGravity(Gravity.CENTER);
        root.addView(info,new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
    }

    private void openChatWithFreshAzureSession(){
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String token){ runOnUiThread(()->{
                if(chatBuilt)return;
                build();
                chatBuilt=true;
                loadAzureMutes();
                loadAzureCommunity();
                azureHandler.removeCallbacks(azurePoll);
                azureHandler.postDelayed(azurePoll,5000);
            });}
            @Override public void err(String message){ runOnUiThread(()->{
                chatBuilt=false;
                buildAuthRecovery();
            });}
        });
    }

    private void buildAuthRecovery(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(28),dp(18),dp(28));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(light);

        TextView title=text("Global Community Chat",27,true);
        title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(64)));

        TextView info=text("Your Azure session is not active. Sign in again to open the real community chat.",16,false);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(-1,-2);
        ilp.setMargins(0,dp(18),0,dp(18));
        root.addView(info,ilp);

        Button signIn=button("Sign in with Azure",true);
        LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,dp(60));
        slp.setMargins(0,dp(8),0,dp(8));
        root.addView(signIn,slp);

        Button back=button("Back",false);
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,dp(58));
        blp.setMargins(0,dp(8),0,0);
        root.addView(back,blp);

        signIn.setOnClickListener(v->{
            buildAuthLoading();
            openChatWithFreshAzureSession();
        });
        back.setOnClickListener(v->finish());
        setContentView(root);
    }
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
        AzureApiClient.get("/community/mutes",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){try{JSONArray a=new JSONObject(body).optJSONArray("mutedUids");mutedUids.clear();if(a!=null)for(int i=0;i<a.length();i++)mutedUids.add(a.getString(i));loadAzureCommunity();}catch(Exception e){toast("Azure safety settings could not be read.");}}
            @Override public void err(String message){toast("Azure safety settings failed.");}
        });
    }
    private void loadAzureCommunity(){
        AzureApiClient.get("/community/messages",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){try{JSONArray a=new JSONObject(body).optJSONArray("messages");messages.removeAllViews();if(a!=null)for(int i=0;i<a.length();i++)addAzureMessageCard(a.getJSONObject(i));scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}catch(Exception e){toast("Community data could not be read.");}}
            @Override public void err(String message){if(messages.getChildCount()==0)toast("Azure Community Chat could not be loaded.");}
        });
    }
    private void addAzureMessageCard(JSONObject d){
        String author=d.optString("author_name","Member"),country=d.optString("country",""),body=d.optString("text",""),uid=d.optString("author_uid","");
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(16));card.setBackground(bg);
        TextView authorView=text(author+(country.isEmpty()?"":" • "+country),15,true);
        TextView bodyView=text(body,16,false);
        LanguageManager.protectUserContent(authorView);LanguageManager.protectUserContent(bodyView);
        card.addView(authorView);card.addView(bodyView);
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.END);Button report=button("Report",false),mute=button("Mute",false),block=button("Block",false);actions.addView(report,new LinearLayout.LayoutParams(dp(86),dp(44)));actions.addView(mute,new LinearLayout.LayoutParams(dp(78),dp(44)));actions.addView(block,new LinearLayout.LayoutParams(dp(82),dp(44)));card.addView(actions);
        report.setOnClickListener(v->reportAzureMessage(d));mute.setOnClickListener(v->muteAzureUser(uid,author));block.setOnClickListener(v->blockAzureUser(uid,author));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(5));messages.addView(card,lp);
    }
    private void sendMessageAzure(){
        String value=composer.getText().toString().trim();if(value.isEmpty()){LanguageManager.setError(composer,"Write a message first");return;}if(value.length()>500){LanguageManager.setError(composer,"Maximum 500 characters");return;}
        JSONObject b=new JSONObject();try{b.put("text",value);}catch(Exception ignored){}
        AzureApiClient.post("/community/messages",b.toString(),new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){runOnUiThread(()->{composer.setText("");toast("Message sent to the real Azure community.");loadAzureCommunity();});}
            @Override public void err(String message){runOnUiThread(()->toast("Azure message failed: "+message));}
        });
    }
    private void reportAzureMessage(JSONObject d){
        final String[] reasons={"Spam / repeated promotion","Scam / money request","Harassment / abuse","Inappropriate content","Private contact details","Other"};
        LanguageManager.dialog(this).setTitle("Report community message").setItems(reasons,(dialog,which)->{
            try{JSONObject b=new JSONObject();b.put("messageId",d.optString("id"));b.put("reportedUid",d.optString("author_uid"));b.put("reason",reasons[which]);AzureApiClient.post("/community/reports",b.toString(),new AzureApiClient.Callback(){public void ok(int code,String body){toast("Report sent securely to Azure moderation.");}public void err(String message){toast("Report could not be submitted.");}});}catch(Exception e){toast("Report could not be submitted.");}
        }).show();
    }
    private void muteAzureUser(String uid,String name){
        if(uid.isEmpty())return;try{JSONObject b=new JSONObject();b.put("userId",uid);AzureApiClient.post("/community/mutes",b.toString(),new AzureApiClient.Callback(){public void ok(int code,String body){mutedUids.add(uid);loadAzureCommunity();toast("Member muted in Azure Community Chat.");}public void err(String message){toast("Mute could not be saved.");}});}catch(Exception e){toast("Mute could not be saved.");}
    }
    private void blockAzureUser(String uid,String name){
        if(uid.isEmpty())return;LanguageManager.dialog(this).setTitle("Block member?").setMessage("This is a real Azure safety action and blocks the member.").setNegativeButton("Cancel",null).setPositiveButton("Block",(dialog,which)->{try{JSONObject b=new JSONObject();b.put("userId",uid);AzureApiClient.post("/blocks",b.toString(),new AzureApiClient.Callback(){public void ok(int code,String body){mutedUids.add(uid);loadAzureCommunity();toast("Member blocked securely in Azure.");}public void err(String message){toast("Block failed: "+message);}});}catch(Exception e){toast("Block failed.");}}).show();
    }
    private void showSafetyAzure(){LanguageManager.dialog(this).setTitle("My Chat Safety").setItems(new String[]{"Refresh my safety settings","Unmute a member"},(d,w)->{if(w==0){loadAzureMutes();toast("Azure safety settings refreshed.");}else showAzureUnmute();}).show();}
    private void showAzureUnmute(){
        if(mutedUids.isEmpty()){toast("No muted members.");return;}String[] ids=mutedUids.toArray(new String[0]);
        LanguageManager.dialog(this).setTitle("Select member to unmute").setItems(ids,(d,w)->{String id=ids[w];AzureApiClient.delete("/community/mutes/"+id,"",new AzureApiClient.Callback(){public void ok(int code,String body){mutedUids.remove(id);loadAzureCommunity();toast("Member unmuted in Azure.");}public void err(String message){toast("Unmute failed.");}});}).show();
    }

    private void toast(String value){LanguageManager.toast(this,value,Toast.LENGTH_LONG).show();}
}
