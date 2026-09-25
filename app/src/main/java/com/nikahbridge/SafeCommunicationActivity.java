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

/** Production mutual-only communication through Azure conversations and PostgreSQL messages. */
public class SafeCommunicationActivity extends Activity {
    private LinearLayout root,conversationList;private EditText message;private TextView status,history,selectedConversation;
    private String selectedConversationId="";
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);PremiumFeatureGate.require(this,"paid40Features","40 SAR Plus or higher",()->{render();loadConversations();String openId=getIntent()!=null?getIntent().getStringExtra("conversationId"):null;if(openId!=null&&!openId.trim().isEmpty()){selectedConversationId=openId.trim();selectedConversation.setText("Selected secure mutual conversation");loadMessages();}});}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(light);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);
        s.addView(root);setContentView(s);
        ViewCompat.setOnApplyWindowInsetsListener(s,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(20),dp(22),dp(20),dp(30)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(s);
    }
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);root.addView(e,new LinearLayout.LayoutParams(-1,dp(62)));return e;}

    private void render(){
        base();root.addView(txt("Mutual-Only Safe Communication",27,true));root.addView(txt("Messages are available only inside real mutual Azure conversations. No direct unsolicited messaging and no demo chat.",15,false));
        conversationList=new LinearLayout(this);conversationList.setOrientation(LinearLayout.VERTICAL);root.addView(conversationList);
        selectedConversation=txt("Choose a mutual conversation above. Secure conversation IDs stay hidden.",15,false);root.addView(selectedConversation);
        message=input("Write a respectful message");
        Button send=btn("Send Secure Azure Message",true);root.addView(send,new LinearLayout.LayoutParams(-1,dp(62)));send.setOnClickListener(v->send());
        Button load=btn("Load Recent Messages",false);root.addView(load,new LinearLayout.LayoutParams(-1,dp(62)));load.setOnClickListener(v->loadMessages());
        Button deleteBoth=btn("Delete Chat for Both",false);root.addView(deleteBoth,new LinearLayout.LayoutParams(-1,dp(62)));deleteBoth.setOnClickListener(v->confirmDeleteForBoth());
        status=txt("Status: waiting",15,false);root.addView(status);
        root.addView(txt("Recent secure Azure messages",15,true));
        history=txt("",15,false);LanguageManager.protectUserContent(history);root.addView(history);
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }

    private boolean authError(String message){
        if(message==null) return false;
        return message.contains("AZURE_SIGN_IN_REQUIRED") || message.contains("AZURE_INTERACTION_REQUIRED") || message.contains("AZURE_AUTH");
    }

    private void showSignInRecovery(String action){
        status.setText("Status: Azure sign in required");
        LanguageManager.dialog(this)
            .setTitle("Sign in required")
            .setMessage("Your Azure session expired. Sign in again to "+action+".")
            .setPositiveButton("Sign in",(d,w)->startActivity(new Intent(this,AzureExternalAuthActivity.class)))
            .setNegativeButton("Not now",null)
            .show();
    }

    private void loadConversations(){
        AzureApiClient.get("/conversations",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("conversations");conversationList.removeAllViews();
                    if(a==null||a.length()==0){conversationList.addView(txt("No real mutual conversations yet.",15,false));return;}
                    for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c==null)continue;String id=c.optString("id",""),name=c.optString("other_display_name","Mutual connection");Button choose=btn("Open: "+name,false);choose.setOnClickListener(v->{selectedConversationId=id;selectedConversation.setText("Selected: "+name);loadMessages();});conversationList.addView(choose,new LinearLayout.LayoutParams(-1,dp(56)));}
                }catch(Exception e){status.setText("Status: Azure conversation list could not be read");}
            });}
            public void err(String message){runOnUiThread(()->{if(authError(message))showSignInRecovery("load your mutual conversations");else status.setText("Status: Azure conversations are temporarily unavailable.");});}
        });
    }

    private void send(){
        String id=selectedConversationId.trim(),body=message.getText().toString().trim();
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
                    }else if(authError(m))showSignInRecovery("send secure messages"); else status.setText("Status: message could not be sent. Please try again.");
                });}
            });
        }catch(Exception e){status.setText("Status: invalid message");}
    }

    private void confirmDeleteForBoth(){
        String id=selectedConversationId.trim();
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
                selectedConversationId="";
                selectedConversation.setText("Choose a mutual conversation above. Secure conversation IDs stay hidden.");
                message.setText("");
                history.setText("");
                status.setText("Status: chat deleted for both people");
                loadConversations();
                LanguageManager.toast(SafeCommunicationActivity.this,"Chat deleted for both.",Toast.LENGTH_LONG).show();
            });}
            public void err(String m){runOnUiThread(()->{if(authError(m))showSignInRecovery("delete this chat");else status.setText("Status: chat could not be deleted. Please try again.");});}
        });
    }

    private void loadMessages(){
        String id=selectedConversationId.trim();if(id.isEmpty()){status.setText("Status: choose a real conversation");return;}
        AzureApiClient.get("/conversations/"+id+"/messages",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("messages");StringBuilder out=new StringBuilder();
                    if(a==null||a.length()==0)out.append("No messages yet.");
                    else for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);out.append(m.optString("sender_user_id","Member")).append(": ").append(m.optString("body","")).append("\n\n");}
                    history.setText(out.toString());status.setText("Status: real Azure conversation history loaded");
                }catch(Exception e){status.setText("Status: message history could not be read");}
            });}
            public void err(String m){runOnUiThread(()->{if(authError(m))showSignInRecovery("load recent messages");else status.setText("Status: message history is temporarily unavailable.");});}
        });
    }
}
