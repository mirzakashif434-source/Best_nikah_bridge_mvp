package com.nikahbridge;

import android.app.*;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.*;
import org.json.*;

public class HelpLineAdminActivity extends Activity {
    private LinearLayout root;
    private FirebaseFunctions functions;

    private Button button(String text) { Button b=new Button(this); b.setText(text); b.setAllCaps(false); root.addView(b,new LinearLayout.LayoutParams(-1,64)); return b; }
    private void text(String s, boolean bold) { TextView t=new TextView(this); t.setText(s); t.setTextSize(bold?18:15); t.setTextColor(Color.rgb(30,45,41)); t.setPadding(8,10,8,10); if(bold)t.setTypeface(null,1); root.addView(t); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); functions=FirebaseFunctions.getInstance();
        ScrollView scroll=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,24,20,30); scroll.addView(root); setContentView(scroll);
        text("Best Nikah Bridge — Help Line Inbox",true);
        text("Admin only. Human-support requests have a 24-hour response target.",false);
        if (FirebaseAuth.getInstance().getCurrentUser()==null) { text("Sign in as admin to view tickets.",false); return; }
        Button refresh=button("Refresh Help Requests"); Button back=button("Back");
        refresh.setOnClickListener(v->load()); back.setOnClickListener(v->finish()); load();
    }

    private void load() {
        loadAzurePrimary();
    }

    private void loadAzurePrimary() {
        if(!AzureAuthManager.hasAccount(this)){ text("Sign in with Azure as admin to view tickets.",false); return; }
        AzureApiClient.get("/admin/help/tickets",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){ runOnUiThread(()->renderAzureTickets(body)); }
            @Override public void err(String message){ runOnUiThread(()->text("Unable to load Azure Help Line: "+message,false)); }
        });
    }

    private void renderAzureTickets(String body){
        try{
            JSONObject rootJson=new JSONObject(body);
            JSONArray tickets=rootJson.optJSONArray("tickets");
            root.removeViews(2, Math.max(0,root.getChildCount()-2));
            if(tickets==null || tickets.length()==0){ text("No help/complaint requests yet.",false); return; }
            for(int i=0;i<tickets.length();i++) showAzureTicket(tickets.getJSONObject(i));
        }catch(Exception e){ text("Unable to read Azure Help Line response.",false); }
    }

    private void showAzureTicket(JSONObject x){
        String status=x.optString("status");
        text("Status: "+status+"\\nUser: "+x.optString("uid")+"\\nCreated: "+x.optString("created_at")+"\\n24h target: "+x.optString("human_reply_target_at")+"\\n\\nQuestion:\\n"+x.optString("question"),true);
        if(!x.isNull("ai_answer")) text("AI answer:\\n"+x.optString("ai_answer"),false);
        if(!x.isNull("human_reply")) text("Human reply:\\n"+x.optString("human_reply"),false);
        if(!"human_replied".equals(status)) { Button b=button("Reply to this request"); b.setOnClickListener(v->replyAzureDialog(x.optString("id"))); }
    }

    private void replyAzureDialog(String ticketId){
        EditText input=new EditText(this); input.setHint("Write your human support reply"); input.setMinLines(4);
        new AlertDialog.Builder(this).setTitle("Human Support Reply").setView(input).setPositiveButton("Send Reply",(d,w)->{
            try{
                JSONObject body=new JSONObject(); body.put("reply",input.getText().toString().trim());
                AzureApiClient.patch("/admin/help/tickets/"+ticketId,body.toString(),new AzureApiClient.Callback(){
                    @Override public void ok(int code,String response){runOnUiThread(()->{Toast.makeText(HelpLineAdminActivity.this,"Reply saved in Azure",Toast.LENGTH_SHORT).show();loadAzurePrimary();});}
                    @Override public void err(String message){runOnUiThread(()->Toast.makeText(HelpLineAdminActivity.this,"Reply failed: "+message,Toast.LENGTH_LONG).show());}
                });
            }catch(Exception e){Toast.makeText(this,"Reply failed.",Toast.LENGTH_LONG).show();}
        }).setNegativeButton("Cancel",null).show();
    }

    // Legacy Firebase admin implementation retained below for rollback safety.
    private void legacyLoad() {
        functions.getHttpsCallable("listHelpLineTickets").call(new HashMap<>()).addOnSuccessListener(r -> {
            root.removeViews(2, Math.max(0,root.getChildCount()-2));
            Object data=r.getData(); if(data instanceof Map){ Object raw=((Map)data).get("tickets"); if(raw instanceof List){ List list=(List)raw; if(list.isEmpty()){text("No help/complaint requests yet.",false);return;} for(Object item:list) showTicket((Map)item); }}
        }).addOnFailureListener(e->text("Unable to load Help Line: "+e.getMessage(),false));
    }

    private void showTicket(Map x) {
        String status=String.valueOf(x.get("status")); String q=String.valueOf(x.get("question"));
        text("Status: "+status+"\nUser: "+String.valueOf(x.get("uid"))+"\nCreated: "+String.valueOf(x.get("createdAt"))+"\n24h target: "+String.valueOf(x.get("humanReplyTargetAt"))+"\n\nQuestion:\n"+q, true);
        Object answer=x.get("aiAnswer"); if(answer!=null) text("AI answer:\n"+answer,false);
        Object reply=x.get("humanReply"); if(reply!=null) text("Human reply:\n"+reply,false);
        if(!"human_replied".equals(status)) { Button b=button("Reply to this request"); b.setOnClickListener(v->replyDialog(String.valueOf(x.get("id")))); }
    }

    private void replyDialog(String ticketId) {
        EditText input=new EditText(this); input.setHint("Write your human support reply"); input.setMinLines(4);
        new AlertDialog.Builder(this).setTitle("Human Support Reply").setView(input).setPositiveButton("Send Reply",(d,w)->{
            Map<String,Object> m=new HashMap<>(); m.put("ticketId",ticketId); m.put("reply",input.getText().toString().trim());
            functions.getHttpsCallable("replyHelpLineTicket").call(m).addOnSuccessListener(r->{ Toast.makeText(this,"Reply saved",Toast.LENGTH_SHORT).show(); load(); }).addOnFailureListener(e->new AlertDialog.Builder(this).setTitle("Reply failed").setMessage(e.getMessage()).setPositiveButton("OK",null).show());
        }).setNegativeButton("Cancel",null).show();
    }
}
