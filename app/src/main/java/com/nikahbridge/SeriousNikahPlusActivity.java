package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class SeriousNikahPlusActivity extends Activity {
    private LinearLayout root,peopleBox;
    private TextView status,countText,limitText;
    private Button upgrade,whoLiked;
    private boolean premium=false;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        AzureAuthManager.bindActivity(this);
        build();
        loadSummary();
    }

    private int dp(int v){return Premium2030Ui.dp(this,v);}
    private Button btn(String s,boolean primary){
        Button b=primary?Premium2030Ui.primary(this,s):Premium2030Ui.secondary(this,s);
        Premium2030Ui.addButton(root,b);return b;
    }
    private void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Premium2030Ui.CREAM);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));
        sc.addView(root);setContentView(sc);

        root.addView(Premium2030Ui.title(this,"Serious Nikah Plus"));
        root.addView(Premium2030Ui.subtitle(this,"Premium for serious members. Free core matching stays available."));
        root.addView(Premium2030Ui.heroLine(this,"Real interests • Advanced compatibility • AI • Larger Family Circle"));

        countText=Premium2030Ui.section(this,"Checking real incoming interests…");root.addView(countText);
        limitText=Premium2030Ui.subtitle(this,"Checking today’s free interest allowance…");root.addView(limitText);

        LinearLayout card=Premium2030Ui.card(this);
        card.addView(Premium2030Ui.section(this,"Premium unlocks"));
        card.addView(Premium2030Ui.subtitle(this,
            "20 SAR Basic\n✓ See Who Liked You\n✓ Unlimited interests\n✓ Family Circle up to 4\n\n"+
            "40 SAR Plus\n✓ Basic features\n✓ Full Why We Matched\n✓ Advanced compatibility\n✓ Marriage timeline\n✓ Family Circle up to 7\n\n"+
            "60 SAR VIP\n✓ Plus features\n✓ Azure AI Nikah Assistant\n✓ Family Circle up to 10\n✓ Priority profile visibility"));
        root.addView(card);

        whoLiked=btn("See Who Liked You",true);whoLiked.setOnClickListener(v->loadWhoLiked());
        upgrade=btn("Upgrade with Google Play",true);upgrade.setOnClickListener(v->startActivity(new Intent(this,PremiumPlansActivity.class)));
        peopleBox=new LinearLayout(this);peopleBox.setOrientation(LinearLayout.VERTICAL);root.addView(peopleBox);
        status=Premium2030Ui.subtitle(this,"Status: loading…");status.setGravity(Gravity.START);root.addView(status);
        Button refresh=btn("Refresh Serious Nikah Plus",false);refresh.setOnClickListener(v->loadSummary());
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }

    private void loadSummary(){
        AzureApiClient.get("/premium/serious-plus-summary",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject o=new JSONObject(body);
                    JSONObject p=o.optJSONObject("premium");
                    premium=p!=null&&p.optBoolean("active",false);
                    int incoming=o.optInt("incomingInterestCount",0);
                    countText.setText(incoming==1
                        ?"1 serious member has sent you an interest."
                        :incoming+" serious members have sent you interests.");
                    if(premium){
                        String plan=p==null?"":p.optString("planKey","");
                        limitText.setText("Premium active • unlimited serious interests");
                        upgrade.setVisibility(android.view.View.VISIBLE);
                        upgrade.setText("Change / Upgrade Plan");
                        whoLiked.setText("Open Who Liked You");
                        status.setText("Status: Serious Nikah Plus active • "+plan);
                    }else{
                        int remaining=o.optInt("freeInterestsRemaining",0),limit=o.optInt("freeInterestDailyLimit",3);
                        limitText.setText("Free plan: "+remaining+" of "+limit+" interests remaining today");
                        upgrade.setVisibility(android.view.View.VISIBLE);
                        whoLiked.setText(incoming>0?"See Who Liked You • Premium":"See Who Liked You");
                        status.setText("Status: free core plan active");
                    }
                }catch(Exception e){status.setText("Status: premium summary could not be read");}
            });}
            public void err(String e){runOnUiThread(()->status.setText("Status: premium summary unavailable"));}
        });
    }

    private void loadWhoLiked(){
        peopleBox.removeAllViews();
        if(!premium){
            status.setText("Who Liked You is included in Serious Nikah Plus.");
            startActivity(new Intent(this,PremiumPlansActivity.class));
            return;
        }
        status.setText("Status: loading real incoming interests…");
        AzureApiClient.get("/premium/who-liked-you",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("people");
                    if(a==null||a.length()==0){
                        peopleBox.addView(Premium2030Ui.subtitle(SeriousNikahPlusActivity.this,"No pending incoming interests right now."));
                    }else{
                        for(int i=0;i<a.length();i++){
                            JSONObject x=a.optJSONObject(i);if(x==null)continue;
                            LinearLayout card=Premium2030Ui.card(SeriousNikahPlusActivity.this);
                            TextView name=Premium2030Ui.section(SeriousNikahPlusActivity.this,
                                x.optString("display_name","Member")+" • "+x.optInt("age",0));
                            LanguageManager.protectUserContent(name);card.addView(name);
                            String place=x.optString("country","");
                            card.addView(Premium2030Ui.subtitle(SeriousNikahPlusActivity.this,place));
                            peopleBox.addView(card);
                        }
                    }
                    status.setText("Status: Who Liked You loaded from real Azure interests");
                }catch(Exception e){status.setText("Status: Who Liked You response could not be read");}
            });}
            public void err(String e){runOnUiThread(()->status.setText("Status: Who Liked You unavailable"));}
        });
    }
}
