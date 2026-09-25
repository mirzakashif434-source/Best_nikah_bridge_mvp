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

/** Real Azure journey tracker derived only from production API records. */
public class NikahJourneyActivity extends Activity {
    private LinearLayout root,stages;private TextView summary;
    private final int green=Premium2030Ui.GREEN,dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED,light=Premium2030Ui.CREAM;
    private boolean profileReady,blueprintReady,verified,hasMatches,hasConversation,hasFamily;
    private Button refresh;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        AzureAuthManager.bindActivity(this);
        render();
        recoverAzureSessionAndLoad();
    }
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
        root.addView(txt("My Nikah Journey",27,true));root.addView(txt("Progress is based only on real Azure profile, verification, matching, conversation and family records.",15,false));
        summary=txt("Checking your real Azure progress…",18,true);root.addView(summary);
        stages=new LinearLayout(this);stages.setOrientation(LinearLayout.VERTICAL);root.addView(stages);
        refresh=btn("Refresh My Journey",true);root.addView(refresh,new LinearLayout.LayoutParams(-1,dp(62)));refresh.setOnClickListener(v->recoverAzureSessionAndLoad());
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }

    private void recoverAzureSessionAndLoad(){
        refresh.setEnabled(false);
        summary.setText("Checking your Azure session…");
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){
                runOnUiThread(()->{
                    refresh.setEnabled(true);
                    load();
                });
            }
            @Override public void err(String message){
                runOnUiThread(()->showAuthRecovery());
            }
        });
    }

    private void showAuthRecovery(){
        stages.removeAllViews();
        summary.setText("Azure sign in is required to load your real Nikah Journey.");
        refresh.setEnabled(false);
        Button signIn=btn("Sign in with Azure",true);
        signIn.setOnClickListener(v->{
            signIn.setEnabled(false);
            signIn.setText("Opening Azure Sign In…");
            AzureAuthManager.acquireTokenInteractive(this,new AzureAuthManager.Callback(){
                @Override public void ok(String accessToken){runOnUiThread(()->recoverAzureSessionAndLoad());}
                @Override public void err(String message){runOnUiThread(()->{
                    signIn.setEnabled(true);
                    signIn.setText("Sign in with Azure");
                    summary.setText("Azure sign in was not completed. Please try again.");
                });}
            });
        });
        stages.addView(signIn,new LinearLayout.LayoutParams(-1,dp(62)));
    }

    private void load(){
        profileReady=blueprintReady=verified=hasMatches=hasConversation=hasFamily=false;
        refresh.setEnabled(false);
        refresh.setText("Loading My Journey…");
        stages.removeAllViews();
        summary.setText("Checking your real Azure progress…");
        AzureApiClient.get("/journey/summary",new AzureApiClient.Callback(){
            public void ok(int c,String b){runOnUiThread(()->{
                try{
                    JSONObject o=new JSONObject(b);
                    profileReady=o.optBoolean("profileReady",false);
                    blueprintReady=o.optBoolean("blueprintReady",false);
                    verified=o.optBoolean("verified",false);
                    hasMatches=o.optBoolean("hasMatches",false);
                    hasConversation=o.optBoolean("hasConversation",false);
                    hasFamily=o.optBoolean("hasFamily",false);
                    refresh.setEnabled(true);
                    refresh.setText("Refresh My Journey");
                    show();
                }catch(Exception e){
                    refresh.setEnabled(true);
                    refresh.setText("Refresh My Journey");
                    showLoadFailure("Journey data could not be displayed. Tap Refresh My Journey.");
                }
            });}
            public void err(String m){runOnUiThread(()->{
                refresh.setEnabled(true);
                refresh.setText("Refresh My Journey");
                if(m!=null&&(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("AZURE_INTERACTION_REQUIRED")||m.contains("401"))){
                    showAuthRecovery();
                }else{
                    showLoadFailure("Journey service is temporarily unavailable. Tap Refresh My Journey.");
                }
            });}
        });
    }

    private void show(){
        runOnUiThread(()->{
            stages.removeAllViews();int done=0;
            done+=add("1. Prepare your profile",profileReady,"Your Azure profile is completed.");
            done+=add("2. Define marriage expectations",blueprintReady,"Timeline, family involvement and children expectations are recorded.");
            done+=add("3. Build trust",verified,"An approved Azure identity verification is recorded.");
            done+=add("4. Review real matches",hasMatches,"At least one real Azure compatibility match is available.");
            done+=add("5. Have a mutual conversation",hasConversation,"A real mutual Azure conversation exists.");
            done+=add("6. Involve family / Wali",hasFamily,"A real family/Wali link exists in Azure.");
            add("7. Prepare for Nikah",false,"Never auto-completed. This requires real-life mutual agreement and appropriate family/legal/religious steps.");
            summary.setText("Journey progress: "+done+" / 7 stages recorded as complete\n\nThis is a progress aid, not a prediction or guarantee of marriage.");
        });
    }
    private void showLoadFailure(String message){
        stages.removeAllViews();
        summary.setText(message);
    }

    private int add(String title,boolean complete,String detail){stages.addView(txt((complete?"✓ ":"○ ")+title+"\n"+detail,16,complete));return complete?1:0;}
}
