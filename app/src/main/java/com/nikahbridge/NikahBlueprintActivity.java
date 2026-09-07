package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Real Nikah Blueprint. All answers are saved to the signed-in member's
 * Firebase users document. No demo data, fake completion, or local-only save.
 */
public class NikahBlueprintActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout root;
    private final Map<String, EditText> fields = new HashMap<>();
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        render();
        load();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(8),dp(4),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private EditText field(String key,String label,String hint){
        root.addView(txt(label,16,true));
        EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setGravity(Gravity.TOP|Gravity.START);e.setMinHeight(dp(70));e.setPadding(dp(14),dp(10),dp(14),dp(10));
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(14));bg.setStroke(dp(1),Color.rgb(205,215,211));e.setBackground(bg);
        root.addView(e,new LinearLayout.LayoutParams(-1,dp(82)));fields.put(key,e);return e;
    }
    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(light);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));sc.addView(root);setContentView(sc);
        root.addView(txt("💎 Nikah Blueprint",28,true));
        root.addView(txt("Build a private, structured picture of the marriage life you are genuinely looking for. Your answers are used to improve compatibility discussions and matching; they are not a religious ruling or a guarantee of marriage.",15,false));
        field("deenPriorities","Deen & religious priorities","What matters most to you in daily religious life, practice and values?");
        field("familyExpectations","Family & in-law expectations","How would you like both families to be involved after marriage?");
        field("childrenExpectation","Children & parenting","Your expectations about children, timing and parenting responsibilities.");
        field("livingPlan","Living arrangement","Where and how would you ideally live after marriage? Include parents/in-laws if relevant.");
        field("careerPlan","Career & work","Your expectations about work, career, study and responsibilities after marriage.");
        field("financialExpectations","Finances & responsibilities","How should financial responsibilities, budgeting and major decisions be discussed?");
        field("parentsSupport","Parents & family support","What responsibilities or support do you expect toward parents and close family?");
        field("relocationPlan","Relocation & migration","Would you consider another city/country? What are your important limits?");
        field("conflictStyle","Conflict & communication","How should disagreements be handled, and what communication style works for you?");
        field("lifestyleValues","Lifestyle & daily life","Describe important expectations around lifestyle, social life, privacy and household routines.");
        field("marriageTimeline","Marriage timeline","What is your realistic timeline for progressing toward marriage?");
        field("dealbreakers","Deal-breakers & non-negotiables","List the issues that would make a marriage unsuitable for you.");
        field("privacyExpectations","Privacy & boundaries","What personal, family and communication boundaries should be respected?");
        field("waliExpectations","Wali / family process","How would you like Wali/family involvement to work when a connection becomes serious?");
        Button save=btn("Save My Real Nikah Blueprint",true);root.addView(save,new LinearLayout.LayoutParams(-1,dp(62)));save.setOnClickListener(v->save());
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
        root.addView(txt("Privacy: only the signed-in member's account can submit these answers through this screen. Do not enter passwords, payment-card details or unnecessary sensitive personal information.",13,false));
    }
    private void load(){
        if(auth.getCurrentUser()==null)return;
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->{for(String k:fields.keySet()){Object v=d.get(k);if(v!=null)fields.get(k).setText(String.valueOf(v));}});
    }
    private void save(){
        if(auth.getCurrentUser()==null){Toast.makeText(this,"Sign in is required.",Toast.LENGTH_LONG).show();return;}
        Map<String,Object> data=new HashMap<>();int completed=0;
        for(Map.Entry<String,EditText> x:fields.entrySet()){
            String value=x.getValue().getText().toString().trim();
            if(!value.isEmpty())completed++;
            data.put(x.getKey(),value);
        }
        if(completed<fields.size()){
            Toast.makeText(this,"Please complete every blueprint section before saving.",Toast.LENGTH_LONG).show();return;
        }
        data.put("nikahBlueprintComplete",true);
        data.put("nikahBlueprintUpdatedAt",FieldValue.serverTimestamp());
        db.collection("users").document(auth.getUid()).set(data,SetOptions.merge())
            .addOnSuccessListener(v->{Toast.makeText(this,"Nikah Blueprint saved securely to your real account.",Toast.LENGTH_LONG).show();})
            .addOnFailureListener(e->Toast.makeText(this,"Could not save your blueprint. Please check your connection and try again.",Toast.LENGTH_LONG).show());
    }
}
