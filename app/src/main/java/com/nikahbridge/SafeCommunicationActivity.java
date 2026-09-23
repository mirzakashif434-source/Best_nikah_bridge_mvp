package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Production mutual-only communication through Azure conversations and PostgreSQL messages. */
public class SafeCommunicationActivity extends Activity {
    private LinearLayout root,conversationList;private EditText conversationId,message;private TextView status,history;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();loadConversations();String openId=getIntent()!=null?getIntent().getStringExtra("conversationId"):null;if(openId!=null&&!openId.trim().isEmpty()){conversationId.setText(openId.trim());loadMessages();}}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);root.addView(e,new LinearLayout.LayoutParams(-1,dp(62)));return e;}

    private void render(){
        base();root.addView(txt("Mutual-Only Safe Communication",27,true));root.addView(txt("Messages are available only inside real mutual Azure conversations. No direct unsolicited messaging and no demo chat.",15,false));
        conversationList=new LinearLayout(this);conversationList.setOrientation(LinearLayout.VERTICAL);root.addView(conversationList);
        conversationId=input("Conversation ID from a real mutual connection");message=input("Write a respectful message");
        Button send=btn("Send Secure Azure Message",true);root.addView(send,new LinearLayout.LayoutParams(-1,dp(62)));send.setOnClickListener(v->send());
        Button load=btn("Load Recent Messages",false);root.addView(load,new LinearLayout.LayoutParams(-1,dp(62)));load.setOnClickListener(v->loadMessages());
        Button deleteBoth=btn("Delete Chat for Both",false);root.addView(deleteBoth,new LinearLayout.LayoutParams(-1,dp(62)));deleteBoth.setOnClickListener(v->confirmDeleteForBoth());
        status=txt("Status: waiting",15,false);root.addView(status);
        root.addView(txt("Recent secure Azure messages",15,true));
        history=txt("",15,false);LanguageManager.protectUserContent(history);root.addView(history);
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }

    private void loadConversations(){
        AzureApiClient.get("/conversations",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("conversations");conversationList.removeAllViews();
                    if(a==null||a.length()==0){conversationList.addView(txt("No real mutual conversations yet.",15,false));return;}
                    for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c==null)continue;String id=c.optString("id",""),name=c.optString("other_display_name","Mutual connection");Button choose=btn("Open: "+name,false);choose.setOnClickListener(v->{conversationId.setText(id);loadMessages();});conversationList.addView(choose,new LinearLayout.LayoutParams(-1,dp(56)));}
                }catch(Exception e){status.setText("Status: Azure conversation list could not be read");}
            });}
            public void err(String message){runOnUiThread(()->status.setText("Status: Azure conversations unavailable — "+message));}
        });
    }

    private void send(){
        String id=conversationId.getText().toString().trim(),body=message.getText().toString().trim();
        if(id.isEmpty()||body.isEmpty()){status.setText("Status: choose a conversation and write a message");return;}
        try{
            JSONObject j=new JSONObject().put("body",body);status.setText("Status: sending securely through Azure…");
            AzureApiClient.post("/conversations/"+id+"/messages",j.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String response){runOnUiThread(()->{
                    message.setText("");
                    try{
                        JSONObject o=new JSONObject(response);
                        if(o.optBoolean("waitingForReply",false)){
                            status.setText("Status: 2 messages sent — now wait for her reply");
                            LanguageManager.dialog(SafeCommunicationActivity.this)
                                .setTitle("Please wait for her reply")
                                .setMessage("You have sent 2 messages. You cannot send another message until she replies. Once she replies, regular chat will open for both of you.")
                                .setPositiveButton("OK",null).show();
                        }else status.setText("Status: Azure message sent");
                    }catch(Exception e){status.setText("Status: Azure message sent");}
                    loadMessages();
                });}
                public void err(String m){runOnUiThread(()->{
                    if(m!=null&&m.contains("WAIT_FOR_HER_REPLY")){
                        status.setText("Status: waiting for her reply");
                        LanguageManager.dialog(SafeCommunicationActivity.this)
                            .setTitle("Message paused")
                            .setMessage("You already sent 2 messages. Please wait for her reply. Your next message will be available after she replies.")
                            .setPositiveButton("OK",null).show();
                    }else status.setText("Status: not sent — "+m);
                });}
            });
        }catch(Exception e){status.setText("Status: invalid message");}
    }

    private void confirmDeleteForBoth(){
        String id=conversationId.getText().toString().trim();
        if(id.isEmpty()){status.setText("Status: choose a real conversation first");return;}
        LanguageManager.dialog(this)
            .setTitle("Delete this chat for both?")
            .setMessage("This permanently deletes the chat room and all messages for both people. The conversation, member ID/name in this chat room, and message history will no longer appear for either person.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Delete for Both",(d,w)->deleteForBoth(id))
            .show();
    }

    private void deleteForBoth(String id){
        status.setText("Status: deleting chat for both…");
        AzureApiClient.delete("/conversations/"+id,"{}",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                conversationId.setText("");
                message.setText("");
                history.setText("");
                status.setText("Status: chat deleted for both people");
                loadConversations();
                LanguageManager.toast(SafeCommunicationActivity.this,"Chat deleted for both.",Toast.LENGTH_LONG).show();
            });}
            public void err(String m){runOnUiThread(()->status.setText("Status: chat delete failed — "+m));}
        });
    }

    private void loadMessages(){
        String id=conversationId.getText().toString().trim();if(id.isEmpty()){status.setText("Status: choose a real conversation");return;}
        AzureApiClient.get("/conversations/"+id+"/messages",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("messages");StringBuilder out=new StringBuilder();
                    if(a==null||a.length()==0)out.append("No messages yet.");
                    else for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);out.append(m.optString("sender_user_id","Member")).append(": ").append(m.optString("body","")).append("\n\n");}
                    history.setText(out.toString());status.setText("Status: real Azure conversation history loaded");
                }catch(Exception e){status.setText("Status: message history could not be read");}
            });}
            public void err(String m){runOnUiThread(()->status.setText("Status: history unavailable — "+m));}
        });
    }
}
