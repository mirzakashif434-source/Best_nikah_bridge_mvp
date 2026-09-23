package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AdvancedMatchFiltersActivity extends Activity {
    private LinearLayout root;
    private EditText countries,cities,education,family;
    private TextView status;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        AzureAuthManager.bindActivity(this);
        PremiumFeatureGate.require(this,"advancedMatching","Plus 40 SAR or VIP 60 SAR",this::render);
    }

    private int dp(int v){return Premium2030Ui.dp(this,v);}
    private EditText input(String hint){
        EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);
        e.setPadding(dp(14),0,dp(14),0);
        e.setBackground(Premium2030Ui.outlined(this,android.graphics.Color.WHITE,Premium2030Ui.GOLD_SOFT,16));
        root.addView(e,new LinearLayout.LayoutParams(-1,dp(58)));return e;
    }
    private Button btn(String label,boolean primary){
        Button b=primary?Premium2030Ui.primary(this,label):Premium2030Ui.secondary(this,label);
        Premium2030Ui.addButton(root,b);return b;
    }
    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Premium2030Ui.CREAM);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));
        sc.addView(root);setContentView(sc);

        root.addView(Premium2030Ui.title(this,"Advanced Match Filters"));
        root.addView(Premium2030Ui.subtitle(this,"Plus/VIP filters apply to real Azure profiles before matches are shown."));
        root.addView(Premium2030Ui.heroLine(this,"Country • City • Education • Family involvement"));

        countries=input("Preferred countries, comma separated");
        cities=input("Preferred cities, comma separated");
        education=input("Preferred education levels, comma separated");
        family=input("Preferred family involvement style");

        Button save=btn("Save Advanced Filters",true);save.setOnClickListener(v->save());
        status=Premium2030Ui.subtitle(this,"Status: loading…");root.addView(status);
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
        load();
    }
    private JSONArray array(String raw){
        JSONArray a=new JSONArray();
        if(raw==null)return a;
        for(String x:raw.split(",")){String s=x.trim();if(!s.isEmpty())a.put(s);}
        return a;
    }
    private String join(JSONArray a){
        if(a==null)return "";
        List<String> out=new ArrayList<>();
        for(int i=0;i<a.length();i++){String s=a.optString(i,"").trim();if(!s.isEmpty())out.add(s);}
        return android.text.TextUtils.join(", ",out);
    }
    private void load(){
        AzureApiClient.get("/premium/advanced-filters",new AzureApiClient.Callback(){
            public void ok(int c,String body){runOnUiThread(()->{
                try{
                    JSONObject f=new JSONObject(body).optJSONObject("filters");
                    if(f==null){status.setText("Status: no saved advanced filters");return;}
                    countries.setText(join(f.optJSONArray("countries")));
                    cities.setText(join(f.optJSONArray("cities")));
                    education.setText(join(f.optJSONArray("education_levels")));
                    family.setText(f.optString("family_involvement",""));
                    status.setText("Status: advanced filters loaded");
                }catch(Exception e){status.setText("Status: filter response could not be read");}
            });}
            public void err(String e){runOnUiThread(()->status.setText("Status: advanced filters unavailable"));}
        });
    }
    private void save(){
        try{
            JSONObject b=new JSONObject()
                .put("countries",array(countries.getText().toString()))
                .put("cities",array(cities.getText().toString()))
                .put("educationLevels",array(education.getText().toString()))
                .put("familyInvolvement",family.getText().toString().trim());
            status.setText("Status: saving advanced filters…");
            AzureApiClient.patch("/premium/advanced-filters",b.toString(),new AzureApiClient.Callback(){
                public void ok(int c,String s){runOnUiThread(()->status.setText("Status: advanced filters saved in Azure")); }
                public void err(String e){runOnUiThread(()->status.setText("Status: advanced filters save failed")); }
            });
        }catch(Exception e){status.setText("Status: invalid filter data");}
    }
}
