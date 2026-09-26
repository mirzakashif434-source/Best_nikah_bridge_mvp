package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;

/** Production multilingual Welcome. Azure External ID is the only active account path. */
public class WelcomeActivity extends Activity {
    @Override protected void onCreate(Bundle state){
        LanguageManager.apply(this);
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        if(AzureAuthManager.hasAccount(this)){startActivity(new Intent(this,AzureHomeActivity.class));finish();return;}
        showWelcome();
    }

    private void showWelcome(){
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);scroll.setBackgroundColor(Color.rgb(250,247,239));
        FrameLayout canvas=new FrameLayout(this);WelcomeArtworkView artwork=new WelcomeArtworkView(this);canvas.addView(artwork,new FrameLayout.LayoutParams(-1,-2));
        TextView start=hit("Get Started"),login=hit("Sign In"),lang=hit("Language: "+LanguageManager.currentName(this));\n        start.setId(R.id.welcome_get_started);\n        login.setId(R.id.welcome_sign_in);\n        lang.setId(R.id.welcome_language);
        canvas.addView(start);canvas.addView(login);canvas.addView(lang);
        artwork.post(()->position(artwork,start,login,lang));
        start.setOnClickListener(v->openAzure());
        login.setOnClickListener(v->openAzure());
        lang.setOnClickListener(v->showLanguageDialog());
        scroll.addView(canvas,new ScrollView.LayoutParams(-1,-2));setContentView(scroll);
    }

    private TextView hit(String d){TextView v=new TextView(this);v.setText("");v.setContentDescription(d);v.setBackgroundColor(Color.TRANSPARENT);v.setClickable(true);v.setFocusable(true);return v;}
    private void position(WelcomeArtworkView a,TextView s,TextView l,TextView g){int w=a.getWidth(),h=a.getHeight();if(w<=0)return;int bw=(int)(w*.74f),bh=Math.max(dp(52),(int)(h*.06f)),left=(w-bw)/2;place(s,left,(int)(h*.800f),bw,bh);place(l,left,(int)(h*.878f),bw,bh);place(g,(int)(w*.735f),(int)(h*.010f),(int)(w*.245f),Math.max(dp(44),(int)(h*.055f)));}
    private void place(View v,int x,int y,int w,int h){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(w,h);p.leftMargin=x;p.topMargin=y;v.setLayoutParams(p);}
    private void showLanguageDialog(){
        LanguageManager.dialog(this)
            .setTitle(LanguageManager.tr(this,"Choose Your Language"))
            .setSingleChoiceItems(LanguageManager.NAMES,LanguageManager.currentIndex(this),(d,which)->{
                LanguageManager.select(this,which);
                d.dismiss();
                recreate();
            })
            .setNegativeButton(LanguageManager.tr(this,"Cancel"),null)
            .show();
    }
    private void openAzure(){startActivity(new Intent(this,AzureExternalAuthActivity.class));}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
}
