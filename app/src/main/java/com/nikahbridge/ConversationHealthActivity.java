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

/** Real conversation-health summary from Azure mutual conversations and messages. */
public class ConversationHealthActivity extends Activity {
    private LinearLayout root;
    private LinearLayout results;
    private Button loadButton;
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;
    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);PremiumFeatureGate.require(this,"paid40Features","40 SAR Plus or higher",this::render);}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);sc.setBackgroundColor(light);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);sc.addView(root);setContentView(sc);
        ViewCompat.setOnApplyWindowInsetsListener(sc,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(20),dp(22),dp(20),dp(30)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(sc);
        root.addView(txt("Conversation Health",27,true));root.addView(txt("Factual Azure message activity only. It does not judge character, sincerity or marriage suitability.",15,false));
        loadButton=btn("Check Real Conversations",true);root.addView(loadButton,new LinearLayout.LayoutParams(-1,dp(62)));loadButton.setOnClickListener(v->loadConnections());
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);root.addView(results);
    }
    private boolean authError(String message){
        if(message==null) return false;
        return message.contains("AZURE_SIGN_IN_REQUIRED") || message.contains("AZURE_INTERACTION_REQUIRED") || message.contains("AZURE_AUTH");
    }

    private void showSignInRecovery(String action){
        LanguageManager.dialog(this)
            .setTitle("Sign in required")
            .setMessage("Your Azure session expired. Sign in again to "+action+".")
            .setPositiveButton("Sign in",(d,w)->startActivity(new Intent(this,AzureExternalAuthActivity.class)))
            .setNegativeButton("Not now",null)
            .show();
    }

    private void loadConnections(){
        if(!AzureAuthManager.hasAccount(this)){showSignInRecovery("check real conversations");return;}
        results.removeAllViews();
        loadButton.setEnabled(false);
        loadButton.setText("Loading real conversations…");
        AzureApiClient.get("/conversations",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                loadButton.setEnabled(true);loadButton.setText("Check Real Conversations");
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("conversations");
                    if(a==null||a.length()==0){results.addView(txt("No mutual Azure conversation exists yet.",16,true));return;}
                    for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c==null)continue;String id=c.optString("id","");String name=c.optString("other_display_name","Mutual connection");TextView header=txt(name,18,true);results.addView(header);Button inspect=btn("View Conversation Health",false);results.addView(inspect,new LinearLayout.LayoutParams(-1,dp(54)));inspect.setOnClickListener(v->loadMessages(id,name));}
                }catch(Exception e){results.addView(txt("Conversation list could not be read.",15,false));}
            });}
            public void err(String message){runOnUiThread(()->{loadButton.setEnabled(true);loadButton.setText("Check Real Conversations");if(authError(message))showSignInRecovery("check real conversations");else results.addView(txt("Azure conversations are temporarily unavailable.",15,false));});}
        });
    }
    private void loadMessages(String id,String name){
        AzureApiClient.get("/conversations/"+id+"/messages",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("messages");int total=a==null?0:a.length();
                    String last=total==0?"Not available":a.optJSONObject(total-1).optString("created_at","Not available");
                    results.addView(txt(name+"\nMessages reviewed: "+total+"\nLast recorded activity: "+last+"\n\nThis is descriptive Azure platform data only.",16,true));
                }catch(Exception e){results.addView(txt("Conversation history could not be read.",15,false));}
            });}
            public void err(String message){runOnUiThread(()->{if(authError(message))showSignInRecovery("view conversation activity");else results.addView(txt("Conversation history is temporarily unavailable.",15,false));});}
        });
    }
}
