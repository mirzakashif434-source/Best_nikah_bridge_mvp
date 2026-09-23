package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** Private real Nikah Blueprint stored in authenticated Azure PostgreSQL user_settings. */
public class NikahBlueprintActivity extends Activity {
    private LinearLayout root;
    private final Map<String,EditText> fields=new LinkedHashMap<>();
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;

    @Override protected void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(8),dp(4),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private void field(String key,String label,String hint){root.addView(txt(label,16,true));EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setGravity(Gravity.TOP|Gravity.START);e.setMinHeight(dp(70));e.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(14));bg.setStroke(dp(1),Color.rgb(205,215,211));e.setBackground(bg);root.addView(e,new LinearLayout.LayoutParams(-1,dp(82)));fields.put(key,e);}

    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(light);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));sc.addView(root);setContentView(sc);
        root.addView(txt("💎 Nikah Blueprint",28,true));root.addView(txt("Your private marriage expectations are stored in your authenticated Azure account. No local-only or demo save.",15,false));
        field("deenPriorities","Deen & religious priorities","What matters most in daily religious life and values?");
        field("familyExpectations","Family & in-law expectations","How should both families be involved?");
        field("childrenExpectation","Children & parenting","Expectations about children and parenting.");
        field("livingPlan","Living arrangement","Where and how would you ideally live?");
        field("careerPlan","Career & work","Expectations around work, career and study.");
        field("financialExpectations","Finances & responsibilities","How should financial responsibilities be discussed?");
        field("parentsSupport","Parents & family support","Expected support toward parents/family.");
        field("relocationPlan","Relocation & migration","Limits or flexibility around moving.");
        field("conflictStyle","Conflict & communication","How should disagreements be handled?");
        field("lifestyleValues","Lifestyle & daily life","Important lifestyle/privacy expectations.");
        field("marriageTimeline","Marriage timeline","Your realistic timeline toward marriage.");
        field("dealbreakers","Deal-breakers & non-negotiables","Important non-negotiables.");
        field("privacyExpectations","Privacy & boundaries","Boundaries that should be respected.");
        field("waliExpectations","Wali / family process","How should Wali/family involvement work?");
        Button save=btn("Save My Real Nikah Blueprint",true);root.addView(save,new LinearLayout.LayoutParams(-1,dp(62)));save.setOnClickListener(v->save());
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }

    private void load(){
        AzureApiClient.get("/settings/nikah_blueprint",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{try{JSONObject v=new JSONObject(body).optJSONObject("value");if(v==null)return;for(Map.Entry<String,EditText> e:fields.entrySet())e.getValue().setText(v.optString(e.getKey(),""));}catch(Exception ignored){}});}
            public void err(String message){}
        });
    }

    private void save(){
        if(!AzureAuthManager.hasAccount(this)){Toast.makeText(this,"Azure sign in required.",Toast.LENGTH_LONG).show();return;}
        try{
            JSONObject data=new JSONObject();int completed=0;
            for(Map.Entry<String,EditText> e:fields.entrySet()){String value=e.getValue().getText().toString().trim();if(!value.isEmpty())completed++;data.put(e.getKey(),value);}
            if(completed<fields.size()){Toast.makeText(this,"Please complete every blueprint section before saving.",Toast.LENGTH_LONG).show();return;}
            data.put("complete",true);
            AzureApiClient.put("/settings/nikah_blueprint",data.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->{Toast.makeText(NikahBlueprintActivity.this,"Nikah Blueprint saved securely in Azure.",Toast.LENGTH_LONG).show();syncLiving();});}
                public void err(String message){runOnUiThread(()->Toast.makeText(NikahBlueprintActivity.this,"Could not save Azure Blueprint: "+message,Toast.LENGTH_LONG).show());}
            });
        }catch(Exception e){Toast.makeText(this,"Blueprint data is invalid.",Toast.LENGTH_LONG).show();}
    }

    private void syncLiving(){
        try{
            JSONObject b=new JSONObject();
            b.put("marriageTimeline",fields.get("marriageTimeline").getText().toString().trim());
            b.put("familyInvolvement",fields.get("familyExpectations").getText().toString().trim());
            b.put("childrenExpectation",fields.get("childrenExpectation").getText().toString().trim());
            b.put("careerPlan",fields.get("careerPlan").getText().toString().trim());
            b.put("livingPlan",fields.get("livingPlan").getText().toString().trim());
            AzureApiClient.post("/compatibility/living",b.toString(),new AzureApiClient.Callback(){public void ok(int c,String x){}public void err(String m){}});
        }catch(Exception ignored){}
    }
}
