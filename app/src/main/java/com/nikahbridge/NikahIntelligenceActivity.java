package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONObject;

/** Azure-backed Nikah Intelligence Center. Real saved preferences only. */
public class NikahIntelligenceActivity extends Activity {
    private LinearLayout root;
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;
    private EditText country,city,timeline,family,children,career,living,deen,dealbreakers;private TextView score,status;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        AzureAuthManager.bindActivity(this);
        PremiumFeatureGate.require(this,"paid20Features","20 SAR Basic or higher",()->{
            render();
            recoverAzureSessionAndLoad();
        });
    }
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
    private TextView txt(String x,int z,boolean b){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(b?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(b)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private void title(String x){TextView t=txt(x,27,true);t.setGravity(Gravity.CENTER);root.addView(t);}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);root.addView(e,new LinearLayout.LayoutParams(-1,dp(62)));return e;}

    private void render(){
        base();title("Nikah Intelligence Center");root.addView(txt("Real marriage preferences stored through Azure. This is decision support, not a prediction or guarantee.",15,false));score=txt("Nikah Readiness: calculating…",19,true);root.addView(score);status=txt("Loading your real Azure preferences…",14,false);root.addView(status);
        country=input("Preferred country / country flexibility");city=input("Preferred city / relocation flexibility");timeline=input("Marriage timeline");family=input("Family / Wali involvement preference");children=input("Children expectation");career=input("Career / work expectation");living=input("Living arrangement after Nikah");deen=input("Deen / values priorities");dealbreakers=input("Important deal-breakers");
        Button save=btn("Save My Marriage Blueprint",true);save.setOnClickListener(v->save());
        Button scenarios=btn("Future Scenario Simulator",true);scenarios.setOnClickListener(v->startActivity(new Intent(this,FutureLifeSimulationActivity.class)));
        Button why=btn("Why We Matched",true);why.setOnClickListener(v->startActivity(new Intent(this,WhyWeMatchedActivity.class)));
        Button traffic=btn("Compatibility Traffic Light",true);traffic.setOnClickListener(v->startActivity(new Intent(this,CompatibilityTrafficLightActivity.class)));
        Button trust=btn("Trust Passport",true);trust.setOnClickListener(v->startActivity(new Intent(this,TrustPassportActivity.class)));
        Button familyBridge=btn("Family Bridge 2.0",true);familyBridge.setOnClickListener(v->startActivity(new Intent(this,FamilyBridge2Activity.class)));
        Button safe=btn("Mutual-Only Safe Communication",true);safe.setOnClickListener(v->startActivity(new Intent(this,SafeCommunicationActivity.class)));
        Button deal=btn("Compatibility Deal-Breaker Engine",true);deal.setOnClickListener(v->startActivity(new Intent(this,CompatibilityDealBreakerActivity.class)));
        Button timelineMatch=btn("Marriage Timeline Matching",true);timelineMatch.setOnClickListener(v->startActivity(new Intent(this,MarriageTimelineMatchingActivity.class)));
        Button ai=btn("AI Nikah Assistant",true);ai.setOnClickListener(v->startActivity(new Intent(this,NikahAssistantActivity.class)));
        Button journey=btn("My Nikah Journey",false);journey.setOnClickListener(v->startActivity(new Intent(this,NikahJourneyActivity.class)));
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }

    private void recoverAzureSessionAndLoad(){
        status.setText("Checking your secure Azure session…");
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){runOnUiThread(()->load());}
            @Override public void err(String message){runOnUiThread(()->showAuthRecovery());}
        });
    }

    private void showAuthRecovery(){
        status.setText("Azure sign in is required to load and save your real Nikah Intelligence preferences.");
        LanguageManager.dialog(this)
            .setTitle("Sign in required")
            .setMessage("Sign in with Azure to continue with your real Nikah Intelligence Center.")
            .setPositiveButton("Sign in",(d,w)->AzureAuthManager.acquireTokenInteractive(this,new AzureAuthManager.Callback(){
                @Override public void ok(String accessToken){runOnUiThread(()->load());}
                @Override public void err(String message){runOnUiThread(()->status.setText("Azure sign in was not completed. Please try again."));}
            }))
            .setNegativeButton("Not now",null)
            .show();
    }

    private void load(){
        status.setText("Loading your real Azure preferences…");
        AzureApiClient.get("/compatibility/living",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{try{JSONObject v=new JSONObject(body).optJSONObject("living");if(v!=null){country.setText(v.optString("country",""));city.setText(v.optString("city",""));timeline.setText(v.optString("marriage_timeline",""));family.setText(v.optString("family_involvement",""));children.setText(v.optString("children_expectation",""));career.setText(v.optString("career_plan",""));living.setText(v.optString("living_plan",""));}calculate();status.setText("Real Azure preferences loaded.");}catch(Exception ignored){status.setText("Some Azure preferences could not be read.");}});}
            public void err(String m){runOnUiThread(()->{if(m!=null&&(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("AZURE_INTERACTION_REQUIRED")||m.contains("401")))showAuthRecovery();else status.setText("Could not load some Azure preferences. Please refresh your sign-in/network and try again.");});}
        });
        AzureApiClient.get("/settings/nikah_intelligence",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{try{JSONObject v=new JSONObject(body).optJSONObject("value");if(v!=null){deen.setText(v.optString("deenPriorities",""));dealbreakers.setText(v.optString("dealbreakers",""));}calculate();}catch(Exception ignored){status.setText("Some private preferences could not be read.");}});}
            public void err(String m){runOnUiThread(()->{if(m!=null&&(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("AZURE_INTERACTION_REQUIRED")||m.contains("401")))showAuthRecovery();else status.setText("Private preferences are temporarily unavailable.");});}
        });
    }

    private void save(){
        status.setText("Checking your secure Azure session…");
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){runOnUiThread(()->saveAuthorized());}
            @Override public void err(String message){runOnUiThread(()->{
                LanguageManager.dialog(NikahIntelligenceActivity.this)
                    .setTitle("Sign in required")
                    .setMessage("Sign in with Azure to save your real Marriage Blueprint.")
                    .setPositiveButton("Sign in",(d,w)->AzureAuthManager.acquireTokenInteractive(NikahIntelligenceActivity.this,new AzureAuthManager.Callback(){
                        @Override public void ok(String accessToken){runOnUiThread(()->saveAuthorized());}
                        @Override public void err(String error){runOnUiThread(()->status.setText("Azure sign in was not completed. Your Blueprint was not changed."));}
                    }))
                    .setNegativeButton("Not now",null)
                    .show();
            });}
        });
    }

    private void saveAuthorized(){
        status.setText("Saving securely to Azure…");
        try{
            JSONObject livingBody=new JSONObject().put("country",v(country)).put("city",v(city)).put("marriageTimeline",v(timeline)).put("familyInvolvement",v(family)).put("childrenExpectation",v(children)).put("careerPlan",v(career)).put("livingPlan",v(living));
            AzureApiClient.post("/compatibility/living",livingBody.toString(),new AzureApiClient.Callback(){
                public void ok(int c,String x){try{JSONObject prefs=new JSONObject().put("deenPriorities",v(deen)).put("dealbreakers",v(dealbreakers));AzureApiClient.put("/settings/nikah_intelligence",prefs.toString(),new AzureApiClient.Callback(){public void ok(int c2,String x2){runOnUiThread(()->{status.setText("Marriage Blueprint saved securely in Azure.");toast("Marriage Blueprint saved securely in Azure.");calculate();});}public void err(String m){runOnUiThread(()->{if(m!=null&&(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("AZURE_INTERACTION_REQUIRED")||m.contains("401")))showAuthRecovery();else status.setText("Core compatibility saved, but private preferences were not saved.");});}});}catch(Exception e){runOnUiThread(()->toast("Could not prepare private preferences."));}}
                public void err(String m){runOnUiThread(()->{if(m!=null&&(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("AZURE_INTERACTION_REQUIRED")||m.contains("401")))showAuthRecovery();else status.setText("Could not save your Azure Blueprint. Please try again.");});}
            });
        }catch(Exception e){toast("Could not prepare Azure Blueprint.");}
    }
    private String v(EditText e){return e.getText().toString().trim();}
    private void calculate(){int filled=0;EditText[] a={country,city,timeline,family,children,career,living,deen,dealbreakers};for(EditText e:a)if(!v(e).isEmpty())filled++;int pct=Math.round(filled*100f/9);score.setText("Nikah Readiness: "+pct+"/100 — "+(pct>=80?"Strong preparation record":pct>=50?"A few important areas remain":"Start by completing your Marriage Blueprint")+"\nPreparation score only — not a prediction or guarantee.");}
    private void toast(String x){LanguageManager.toast(this,x,Toast.LENGTH_LONG).show();}
}
