package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real conversation-health summary from Azure mutual conversations and messages. */
public class ConversationHealthActivity extends Activity {
    private LinearLayout root;
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;
    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private void render(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);sc.addView(root);setContentView(sc);
        root.addView(txt("Conversation Health",27,true));root.addView(txt("Factual Azure message activity only. It does not judge character, sincerity or marriage suitability.",15,false));
        Button load=btn("Check Real Conversations",true);root.addView(load,new LinearLayout.LayoutParams(-1,dp(62)));load.setOnClickListener(v->loadConnections());
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }
    private void loadConnections(){
        AzureApiClient.get("/conversations",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("conversations");
                    if(a==null||a.length()==0){root.addView(txt("No mutual Azure conversation exists yet.",16,true));return;}
                    for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c==null)continue;String id=c.optString("id","");String name=c.optString("other_display_name","Mutual connection");TextView header=txt(name,18,true);root.addView(header);Button inspect=btn("View Conversation Health",false);root.addView(inspect,new LinearLayout.LayoutParams(-1,dp(54)));inspect.setOnClickListener(v->loadMessages(id,name));}
                }catch(Exception e){root.addView(txt("Conversation list could not be read.",15,false));}
            });}
            public void err(String message){runOnUiThread(()->root.addView(txt("Could not load Azure conversations: "+message,15,false)));}
        });
    }
    private void loadMessages(String id,String name){
        AzureApiClient.get("/conversations/"+id+"/messages",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("messages");int total=a==null?0:a.length();
                    String last=total==0?"Not available":a.optJSONObject(total-1).optString("created_at","Not available");
                    root.addView(txt(name+"\nMessages reviewed: "+total+"\nLast recorded activity: "+last+"\n\nThis is descriptive Azure platform data only.",16,true));
                }catch(Exception e){root.addView(txt("Conversation history could not be read.",15,false));}
            });}
            public void err(String message){runOnUiThread(()->root.addView(txt("Conversation history unavailable: "+message,15,false)));}
        });
    }
}
