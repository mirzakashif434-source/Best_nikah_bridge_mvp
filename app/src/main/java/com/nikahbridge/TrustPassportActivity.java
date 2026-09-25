package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/** Trust Passport built only from real Azure/PostgreSQL evidence. */
public class TrustPassportActivity extends Activity {
    private LinearLayout root;
    private LinearLayout results,matchChoices;
    private Button checkButton;
    private TextView matchStatus;
    private String selectedMatchId="";
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);PremiumFeatureGate.require(this,"paid20Features","20 SAR Basic or higher",this::render);}
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
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}
    private Button choiceBtn(String x){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(green);GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(18));g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);root.addView(e,new LinearLayout.LayoutParams(-1,dp(62)));return e;}

    private void render(){
        base();root.addView(txt("Trust Passport",27,true));
        root.addView(txt("A transparent evidence card built from real Azure verification, photo and profile records. No fabricated badge or hidden personality score.",15,false));
        matchStatus=txt("Loading your real Azure matches…",15,false);root.addView(matchStatus);
        matchChoices=new LinearLayout(this);matchChoices.setOrientation(LinearLayout.VERTICAL);root.addView(matchChoices);
        checkButton=btn("View Selected Trust Passport",true);checkButton.setEnabled(false);checkButton.setOnClickListener(v->load(selectedMatchId));
        loadMatchChoices();
        Button family=btn("Family Bridge 2.0",true);family.setOnClickListener(v->{
            if(!AzureAuthManager.hasAccount(this)){
                LanguageManager.dialog(this)
                    .setTitle("Sign in required")
                    .setMessage("Sign in with Azure to open your real Family Bridge.")
                    .setPositiveButton("Sign in",(d,w)->startActivity(new Intent(this,AzureExternalAuthActivity.class)))
                    .setNegativeButton("Not now",null)
                    .show();
            }else startActivity(new Intent(this,FamilyBridge2Activity.class));
        });
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);root.addView(results);
    }

    private void loadMatchChoices(){
        matchStatus.setText("Checking your secure Azure session…");
        checkButton.setEnabled(false);
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){runOnUiThread(()->loadMatchChoicesAuthorized());}
            @Override public void err(String message){runOnUiThread(()->showMatchAuthRecovery());}
        });
    }

    private void loadMatchChoicesAuthorized(){
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("matches");
                    matchChoices.removeAllViews();
                    if(a==null||a.length()==0){
                        matchStatus.setText("No real compatible matches are available yet.");
                        checkButton.setEnabled(false);
                        return;
                    }
                    matchStatus.setText("Choose a real match. Secure member IDs stay hidden.");
                    for(int i=0;i<Math.min(a.length(),50);i++){
                        JSONObject m=a.optJSONObject(i);if(m==null)continue;
                        String id=m.optString("userId","").trim();if(id.isEmpty())continue;
                        String name=m.optString("displayName","Member").trim();
                        final String matchId=id,matchName=name.isEmpty()?"Member":name;
                        Button choose=choiceBtn("Choose "+matchName);
                        matchChoices.addView(choose,new LinearLayout.LayoutParams(-1,dp(56)));
                        choose.setOnClickListener(v->{
                            selectedMatchId=matchId;
                            matchStatus.setText("Selected: "+matchName);
                            checkButton.setEnabled(true);
                        });
                    }
                }catch(Exception e){matchStatus.setText("Real Azure matches could not be displayed.");}
            });}
            public void err(String message){runOnUiThread(()->{
                if(message!=null&&(message.contains("AZURE_SIGN_IN_REQUIRED")||message.contains("AZURE_INTERACTION_REQUIRED")||message.contains("401"))){
                    showMatchAuthRecovery();
                }else{
                    matchStatus.setText("Real Azure matches are temporarily unavailable.");
                }
            });}
        });
    }

    private void showMatchAuthRecovery(){
        matchChoices.removeAllViews();
        selectedMatchId="";
        checkButton.setEnabled(false);
        matchStatus.setText("Azure sign in is required to choose a real match.");
        Button signIn=choiceBtn("Sign in with Azure");
        matchChoices.addView(signIn,new LinearLayout.LayoutParams(-1,dp(56)));
        signIn.setOnClickListener(v->{
            signIn.setEnabled(false);
            signIn.setText("Opening Azure Sign In…");
            AzureAuthManager.acquireTokenInteractive(this,new AzureAuthManager.Callback(){
                @Override public void ok(String accessToken){runOnUiThread(()->loadMatchChoicesAuthorized());}
                @Override public void err(String message){runOnUiThread(()->{
                    signIn.setEnabled(true);
                    signIn.setText("Sign in with Azure");
                    matchStatus.setText("Azure sign in was not completed. Please try again.");
                });}
            });
        });
    }

    private void load(String id){
        if(id.isEmpty()){toast("Choose a real match first.");return;}
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){runOnUiThread(()->loadAuthorized(id));}
            @Override public void err(String message){runOnUiThread(()->showMatchAuthRecovery());}
        });
    }

    private void loadAuthorized(String id){
        results.removeAllViews();
        checkButton.setEnabled(false);
        checkButton.setText("Loading real Trust Passport…");
        AzureApiClient.get("/matches/"+id,new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                checkButton.setEnabled(true);checkButton.setText("View Real Trust Passport");
                try{
                    JSONObject m=new JSONObject(body).optJSONObject("match");if(m==null){toast("Trust evidence unavailable.");return;}
                    results.addView(txt("Real member",20,true));
                    results.addView(txt(m.optString("displayName","Member")+" • Age "+m.optInt("age",0)+" • "+m.optString("country",""),17,true));
                    String intent=m.optString("marriageIntention","");
                    addEvidence("Marriage intent",intent.isEmpty()?"NOT STATED":intent,!intent.isEmpty());
                    addEvidence("Identity verification",m.optBoolean("identityVerified",false)?"APPROVED — Azure verification record exists":"NOT APPROVED — no approved verification record",m.optBoolean("identityVerified",false));
                    addEvidence("Profile photo",m.optBoolean("photoPresent",false)?"APPROVED PHOTO PRESENT":"NO APPROVED PHOTO RECORDED",m.optBoolean("photoPresent",false));
                    addEvidence("Profile readiness","Readiness value: "+m.optInt("readinessScore",0)+"/100",m.optInt("readinessScore",0)>0);
                    addEvidence("Compatibility context","Azure compatibility: "+m.optInt("compatibilityScore",0)+"/100",true);
                    JSONArray reasons=m.optJSONArray("whyWeMatched");if(reasons!=null&&reasons.length()>0){StringBuilder s=new StringBuilder();for(int i=0;i<reasons.length();i++)s.append("• ").append(reasons.optString(i)).append("\n");results.addView(txt("Recorded match factors\n"+s,15,false));}
                    addEvidence("Safety limitation","Trust Passport never overrides blocks, reports, verification, or privacy controls.",true);
                    results.addView(txt("Trust Passport is evidence, not a guarantee of character, honesty, compatibility, or marriage outcome.",14,false));
                }catch(Exception e){toast("Azure Trust Passport response could not be read.");}
            });}
            public void err(String message){runOnUiThread(()->{checkButton.setEnabled(true);checkButton.setText("View Real Trust Passport");toast("Could not load Trust Passport: "+message);});}
        });
    }
    private void addEvidence(String label,String value,boolean positive){TextView t=txt((positive?"✓ ":"• ")+label+"\n"+value,16,true);t.setPadding(dp(10),dp(12),dp(10),dp(12));results.addView(t);}
    private void toast(String x){LanguageManager.toast(this,x,Toast.LENGTH_LONG).show();}
}
