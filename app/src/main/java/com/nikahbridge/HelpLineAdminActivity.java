package com.nikahbridge;

import android.app.*;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.*;

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
