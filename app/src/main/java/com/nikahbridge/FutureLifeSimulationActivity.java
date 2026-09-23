package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;

/** Real Future Life Simulation using Azure AI and private Azure PostgreSQL settings. */
public class FutureLifeSimulationActivity extends Activity {
    private LinearLayout root;private Button run;private TextView result;
    private final EditText[] my=new EditText[8],partner=new EditText[8];
    private final String[] titles={"1. Job loss or financial pressure","2. Caring for parents","3. Children and parenting","4. Relocation or migration","5. Household responsibilities","6. Deen, lifestyle and social boundaries","7. Serious disagreement","8. Privacy and family involvement"};
    private final String[] prompts={"How would both spouses respond if income dropped?","How should care and support for parents be handled?","Expectations about children and parenting?","What if moving city/country became necessary?","How should household responsibilities be shared?","Which Deen/lifestyle boundaries matter?","How should a serious disagreement be handled?","What privacy and family/Wali boundaries should apply?"};
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;

    @Override protected void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(8),dp(4),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private EditText area(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setGravity(Gravity.TOP|Gravity.START);e.setMinHeight(dp(110));e.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(14));g.setStroke(dp(1),Color.rgb(205,215,211));e.setBackground(g);return e;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}

    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(light);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));sc.addView(root);setContentView(sc);
        root.addView(txt("🔥 Future Life Simulation",28,true));root.addView(txt("Real Azure AI comparison. It supports discussion and does not predict the future or decide whether two people should marry.",15,false));
        root.addView(txt("Your answers",20,true));
        for(int i=0;i<8;i++){root.addView(txt(titles[i]+" — "+prompts[i],16,true));my[i]=area("Write your honest answer");root.addView(my[i],new LinearLayout.LayoutParams(-1,dp(118)));}
        root.addView(txt("Partner's answers",20,true));root.addView(txt("Use the partner's answers only with explicit permission.",14,false));
        for(int i=0;i<8;i++){partner[i]=area("Partner's answer");root.addView(partner[i],new LinearLayout.LayoutParams(-1,dp(118)));}
        CheckBox consent=new CheckBox(this);consent.setText("I have permission to use the partner answers for this comparison.");root.addView(consent);
        run=btn("Run Real Future Life Comparison",true);root.addView(run,new LinearLayout.LayoutParams(-1,dp(62)));
        result=txt("Your Azure AI comparison will appear here.",15,false);root.addView(result);
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
        run.setOnClickListener(v->compare(consent));
    }

    private void load(){
        AzureApiClient.get("/settings/future_life_simulation",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{try{JSONObject v=new JSONObject(body).optJSONObject("value");if(v==null)return;for(int i=0;i<8;i++){my[i].setText(v.optString("my"+(i+1),""));partner[i].setText(v.optString("partner"+(i+1),""));}}catch(Exception ignored){}});}
            public void err(String message){}
        });
    }

    private void compare(CheckBox consent){
        if(!AzureAuthManager.hasAccount(this)){Toast.makeText(this,"Azure sign in required.",Toast.LENGTH_LONG).show();return;}
        if(!consent.isChecked()){Toast.makeText(this,"Permission is required before comparing partner answers.",Toast.LENGTH_LONG).show();return;}
        StringBuilder a=new StringBuilder(),b=new StringBuilder();
        for(int i=0;i<8;i++){String av=my[i].getText().toString().trim(),bv=partner[i].getText().toString().trim();if(av.length()<20||bv.length()<20){Toast.makeText(this,"Please answer all 8 scenarios in both sections.",Toast.LENGTH_LONG).show();return;}a.append("Scenario ").append(i+1).append(": ").append(av).append("\n");b.append("Scenario ").append(i+1).append(": ").append(bv).append("\n");}
        run.setEnabled(false);result.setText("Real Azure AI comparison in progress…");
        String prompt="Compare these two sets of marriage-scenario answers fairly. Return agreements, differences, issues to discuss, five practical questions and a neutral next step. Do not predict a marriage outcome or invent facts.\n\nPERSON A:\n"+a+"\nPERSON B:\n"+b;
        try{
            JSONObject body=new JSONObject().put("prompt",prompt);
            AzureApiClient.post("/ai/future-life",body.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String response){runOnUiThread(()->{try{String out=new JSONObject(response).getJSONObject("assistant").getString("content");result.setText(out);save(out);}catch(Exception e){result.setText("Azure AI returned an invalid response.");}run.setEnabled(true);});}
                public void err(String message){runOnUiThread(()->{result.setText("Azure AI comparison is temporarily unavailable.");run.setEnabled(true);});}
            });
        }catch(Exception e){result.setText("Could not prepare the Azure AI request.");run.setEnabled(true);}
    }

    private void save(String output){
        try{
            JSONObject v=new JSONObject();for(int i=0;i<8;i++){v.put("my"+(i+1),my[i].getText().toString().trim());v.put("partner"+(i+1),partner[i].getText().toString().trim());}v.put("lastResult",output);
            AzureApiClient.put("/settings/future_life_simulation",v.toString(),new AzureApiClient.Callback(){public void ok(int c,String b){}public void err(String m){runOnUiThread(()->Toast.makeText(FutureLifeSimulationActivity.this,"Comparison completed, but private Azure history could not be saved.",Toast.LENGTH_LONG).show());}});
        }catch(Exception ignored){}
    }
}
