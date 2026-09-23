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

/** Real Azure outcome-focused progress plan. No fabricated success rates or outcomes. */
public class NikahSuccessPlanActivity extends Activity {
    private LinearLayout root,stages;private TextView summary;private Button refresh;private boolean authFailed;
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;
    private boolean profile,blueprint,trust,discoverable,connection,activity;private int interestCount,conversationCount;

    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}

    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);sc.setBackgroundColor(light);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);sc.addView(root);setContentView(sc);
        ViewCompat.setOnApplyWindowInsetsListener(sc,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(20),dp(22),dp(20),dp(30)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(sc);
        root.addView(txt("Nikah Success Plan",27,true));root.addView(txt("Focus on meaningful real Azure progress instead of endless swiping. No success percentage or marriage prediction is fabricated.",15,false));
        summary=txt("Checking real Azure progress…",18,true);root.addView(summary);stages=new LinearLayout(this);stages.setOrientation(LinearLayout.VERTICAL);root.addView(stages);
        refresh=btn("Refresh Real Progress",true);root.addView(refresh,new LinearLayout.LayoutParams(-1,dp(62)));refresh.setOnClickListener(v->load());
        Button journey=btn("Open My Nikah Journey",false);root.addView(journey,new LinearLayout.LayoutParams(-1,dp(62)));journey.setOnClickListener(v->startActivity(new Intent(this,NikahJourneyActivity.class)));
        Button matches=btn("Open Real Compatibility Matches",false);root.addView(matches,new LinearLayout.LayoutParams(-1,dp(62)));matches.setOnClickListener(v->startActivity(new Intent(this,AzureHomeActivity.class)));
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }

    private boolean authError(String message){
        if(message==null) return false;
        return message.contains("AZURE_SIGN_IN_REQUIRED") || message.contains("AZURE_INTERACTION_REQUIRED") || message.contains("AZURE_AUTH");
    }

    private void requireSignIn(){
        if(authFailed) return;
        authFailed=true;
        runOnUiThread(()->{
            refresh.setEnabled(true);
            refresh.setText("Refresh Real Progress");
            stages.removeAllViews();
            summary.setText("Azure sign in is required to load your real progress.");
            LanguageManager.dialog(this)
                .setTitle("Sign in required")
                .setMessage("Your Azure session expired. Sign in again to load your real Nikah Success Plan.")
                .setPositiveButton("Sign in",(d,w)->startActivity(new Intent(this,AzureExternalAuthActivity.class)))
                .setNegativeButton("Not now",null)
                .show();
        });
    }

    private void load(){
        if(!AzureAuthManager.hasAccount(this)){requireSignIn();return;}
        authFailed=false;
        profile=blueprint=trust=discoverable=connection=activity=false;interestCount=conversationCount=0;stages.removeAllViews();
        refresh.setEnabled(false);refresh.setText("Loading real progress…");summary.setText("Checking real Azure progress…");
        AzureApiClient.get("/profile",new AzureApiClient.Callback(){
            public void ok(int c,String b){try{JSONObject p=new JSONObject(b).optJSONObject("profile");profile=p!=null&&p.optBoolean("profile_completed",false);discoverable=p!=null&&p.optBoolean("is_visible",false);}catch(Exception ignored){}living();}
            public void err(String m){if(authError(m))requireSignIn();else living();}
        });
    }
    private void living(){AzureApiClient.get("/compatibility/living",new AzureApiClient.Callback(){public void ok(int c,String b){try{JSONObject v=new JSONObject(b).optJSONObject("living");blueprint=v!=null&&!v.optString("marriage_timeline","").isEmpty()&&!v.optString("family_involvement","").isEmpty()&&!v.optString("children_expectation","").isEmpty();}catch(Exception ignored){}verify();}public void err(String m){if(authError(m))requireSignIn();else verify();}});}
    private void verify(){AzureApiClient.get("/verification",new AzureApiClient.Callback(){public void ok(int c,String b){try{JSONArray a=new JSONObject(b).optJSONArray("verifications");if(a!=null)for(int i=0;i<a.length();i++)if("approved".equalsIgnoreCase(a.optJSONObject(i).optString("status")))trust=true;}catch(Exception ignored){}interests();}public void err(String m){if(authError(m))requireSignIn();else interests();}});}
    private void interests(){AzureApiClient.get("/interests",new AzureApiClient.Callback(){public void ok(int c,String b){try{JSONArray a=new JSONObject(b).optJSONArray("interests");interestCount=a==null?0:a.length();}catch(Exception ignored){}conversations();}public void err(String m){if(authError(m))requireSignIn();else conversations();}});}
    private void conversations(){AzureApiClient.get("/conversations",new AzureApiClient.Callback(){public void ok(int c,String b){try{JSONArray a=new JSONObject(b).optJSONArray("conversations");conversationCount=a==null?0:a.length();connection=conversationCount>0;activity=interestCount>0||conversationCount>0;}catch(Exception ignored){}show();}public void err(String m){if(authError(m))requireSignIn();else{activity=interestCount>0;show();}}});}

    private void show(){
        runOnUiThread(()->{
            if(authFailed)return;
            refresh.setEnabled(true);refresh.setText("Refresh Real Progress");
            stages.removeAllViews();int complete=0;
            complete+=add("1. Complete a truthful profile",profile,"Only your saved Azure profile counts.");
            complete+=add("2. Define marriage expectations",blueprint,"Your real Azure compatibility blueprint is recorded.");
            complete+=add("3. Build platform trust",trust,"An approved Azure identity verification is recorded.");
            complete+=add("4. Enter the real match pool",discoverable,"Your Azure profile is visible for matching.");
            complete+=add("5. Progress a real connection",connection,"At least one real mutual Azure conversation exists.");
            complete+=add("6. Have serious platform activity",activity,"Real interests or mutual conversations are recorded.");
            summary.setText("Real progress: "+complete+" / 6 outcome stages\n\nInterests recorded: "+interestCount+"\nMutual conversations: "+conversationCount+"\n\nBest Nikah Bridge does not manufacture success or predict a marriage outcome.");
        });
    }
    private int add(String title,boolean complete,String detail){stages.addView(txt((complete?"✓ ":"○ ")+title+"\n"+detail,16,complete));return complete?1:0;}
}
