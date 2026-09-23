package com.nikahbridge;

import android.app.*;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import org.json.*;

public class HelpLineAdminActivity extends Activity {
    private LinearLayout root;

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private Button button(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);root.addView(b,new LinearLayout.LayoutParams(-1,dp(64)));return b;}
    private void text(String s,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(bold?18:15);t.setTextColor(Premium2030Ui.TEXT);t.setPadding(dp(8),dp(10),dp(8),dp(10));if(bold)t.setTypeface(null,1);root.addView(t);}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);AzureAuthManager.bindActivity(this);
        ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(30));scroll.addView(root);setContentView(scroll);
        text("Best Nikah Bridge — Help Line Inbox",true);
        text("Azure admin only. Human-support requests have a 24-hour response target.",false);
        Button refresh=button("Refresh Help Requests");Button back=button("Back");
        refresh.setOnClickListener(v->load());back.setOnClickListener(v->finish());load();
    }

    private void load(){
        if(!AzureAuthManager.hasAccount(this)){text("Sign in with Azure as admin to view tickets.",false);return;}
        AzureApiClient.get("/admin/help/tickets",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->renderTickets(body));}
            public void err(String message){runOnUiThread(()->text("Unable to load Azure Help Line: "+message,false));}
        });
    }

    private void renderTickets(String body){
        try{
            JSONObject j=new JSONObject(body);JSONArray tickets=j.optJSONArray("tickets");
            while(root.getChildCount()>4)root.removeViewAt(4);
            if(tickets==null||tickets.length()==0){text("No help/complaint requests yet.",false);return;}
            for(int i=0;i<tickets.length();i++)showTicket(tickets.getJSONObject(i));
        }catch(Exception e){text("Unable to read Azure Help Line response.",false);}
    }

    private void showTicket(JSONObject x){
        String status=x.optString("status");
        text("Status: "+status+"\nUser: "+x.optString("uid")+"\nCreated: "+x.optString("created_at")+"\n24h target: "+x.optString("human_reply_target_at")+"\n\nQuestion:\n"+x.optString("question"),true);
        if(!x.isNull("ai_answer"))text("AI answer:\n"+x.optString("ai_answer"),false);
        if(!x.isNull("human_reply"))text("Human reply:\n"+x.optString("human_reply"),false);
        if(!"human_replied".equals(status)){Button b=button("Reply to this request");b.setOnClickListener(v->replyDialog(x.optString("id")));}
    }

    private void replyDialog(String id){
        EditText input=new EditText(this);input.setHint("Write your human support reply");input.setMinLines(4);
        LanguageManager.dialog(this).setTitle("Human Support Reply").setView(input).setPositiveButton("Send Reply",(d,w)->{
            String reply=input.getText().toString().trim();if(reply.isEmpty()){LanguageManager.toast(this,"Reply is required.",Toast.LENGTH_LONG).show();return;}
            try{
                JSONObject body=new JSONObject().put("reply",reply);
                AzureApiClient.patch("/admin/help/tickets/"+id,body.toString(),new AzureApiClient.Callback(){
                    public void ok(int code,String response){runOnUiThread(()->{LanguageManager.toast(HelpLineAdminActivity.this,"Reply saved in Azure.",Toast.LENGTH_SHORT).show();load();});}
                    public void err(String message){runOnUiThread(()->LanguageManager.toast(HelpLineAdminActivity.this,"Reply failed: "+message,Toast.LENGTH_LONG).show());}
                });
            }catch(Exception e){LanguageManager.toast(this,"Reply failed.",Toast.LENGTH_LONG).show();}
        }).setNegativeButton("Cancel",null).show();
    }
}
