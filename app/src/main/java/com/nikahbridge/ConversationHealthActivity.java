package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;
import java.util.Date;

/** Real conversation-health summary from recorded mutual connection messages. */
public class ConversationHealthActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout root;
    private final int green = Color.rgb(18, 103, 82);
    private final int dark = Color.rgb(30, 45, 41);
    private final int gray = Color.rgb(85, 100, 95);
    private final int light = Color.rgb(247, 250, 249);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        render();
    }
    private TextView txt(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(6,8,6,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);return b;}
    private void render(){ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);sc.addView(root);setContentView(sc);root.addView(txt("Conversation Health",27,true));root.addView(txt("A factual view of your mutual connection: recorded message volume, replies and recent activity. It does not judge personality or relationship quality.",15,false));Button load=btn("Check Real Conversation",true);root.addView(load,new LinearLayout.LayoutParams(-1,62));load.setOnClickListener(v->loadConnections());Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,62));back.setOnClickListener(v->finish());}
    private void loadConnections(){if(auth.getCurrentUser()==null){toast("Sign in is required.");return;}String uid=auth.getCurrentUser().getUid();db.collection("connections").whereArrayContains("members",uid).limit(50).get().addOnSuccessListener(snapshot->showConnections(snapshot,uid)).addOnFailureListener(e->toast("Could not load real mutual connections."));}
    private void showConnections(com.google.firebase.firestore.QuerySnapshot snapshot,String uid){if(snapshot.isEmpty()){root.addView(txt("No mutual connection has been recorded yet. Nothing will be invented.",16,true));return;}for(DocumentSnapshot connection:snapshot.getDocuments())loadMessages(connection.getId(),uid);}
    private void loadMessages(String connectionId,String uid){db.collection("connections").document(connectionId).collection("messages").orderBy("createdAt",Query.Direction.ASCENDING).limit(100).get().addOnSuccessListener(messages->{int mine=0;int theirs=0;int total=0;Date last=null;for(DocumentSnapshot message:messages.getDocuments()){total++;String sender=message.getString("senderUid");if(uid.equals(sender))mine++;else theirs++;Timestamp ts=message.getTimestamp("createdAt");if(ts!=null)last=ts.toDate();}String status=theirs>0?"Replies recorded":"No reply recorded in the loaded messages";String lastText=last==null?"Not available":last.toString();root.addView(txt("Connection: "+connectionId+"\nMessages reviewed: "+total+"\nYour messages: "+mine+"\nOther member messages: "+theirs+"\nStatus: "+status+"\nLast message: "+lastText,16,true);}).addOnFailureListener(e->toast("Conversation history is unavailable for one connection."));}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}
