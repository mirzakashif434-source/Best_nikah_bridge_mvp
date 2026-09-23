package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TermsAndCommunityGuidelinesActivity extends Activity {
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(95,108,103), light=Color.rgb(247,250,249);
    private CheckBox agree;
    private Button accept;
    private static final String TERMS_VERSION="2026-09-07-v1";

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(7),dp(4),dp(7));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        if(!AzureAuthManager.hasAccount(this)){buildAuthRecovery();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(20),dp(22),dp(28));root.setBackgroundColor(light);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackgroundColor(light);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        TextView title=text("Terms & Community Guidelines",27,true);title.setGravity(Gravity.CENTER);content.addView(title,new LinearLayout.LayoutParams(-1,dp(65)));
        content.addView(text("This is a serious Muslim matrimonial service. It is not a dating app. These rules apply before you create or upload user-generated content.",16,false));
        content.addView(text("You agree that:\n\n• You will use the service only for genuine marriage/Nikah intentions.\n• You will provide truthful information and will not impersonate another person.\n• You will not post or upload sexual, exploitative, abusive, hateful, fraudulent, or illegal content.\n• You will not request money, passwords, OTPs, banking details, or other sensitive credentials from members.\n• You will not use the service for scams, spam, harassment, stalking, promotion, or unwanted contact.\n• You will not upload another person's photo or private material without permission.\n• Community posts and profile photos may be reviewed, reported, blocked, removed, or restricted for safety and policy enforcement.\n• Use Report and Block tools when you see harmful content.\n• Limited safety/legal records may be preserved where required by law or necessary to prevent fraud and abuse.\n• Accounts that violate these rules may be restricted, suspended, or removed.\n\nChild sexual abuse and exploitation material is strictly prohibited and is handled according to applicable law and platform safety procedures.",15,false));
        content.addView(text("Current policy version: "+TERMS_VERSION,13,false));
        agree=new CheckBox(this);agree.setText("I have read and agree to the Terms & Community Guidelines.");agree.setTextSize(15);content.addView(agree,new LinearLayout.LayoutParams(-1,dp(60)));
        accept=new Button(this);accept.setText("Accept & Continue");accept.setAllCaps(false);accept.setTextSize(16);accept.setTextColor(Color.WHITE);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(green);bg.setCornerRadius(dp(18));accept.setBackground(bg);content.addView(accept,new LinearLayout.LayoutParams(-1,dp(62)));
        Button privacy=new Button(this);privacy.setText("View Privacy Policy");privacy.setAllCaps(false);privacy.setTextColor(green);content.addView(privacy,new LinearLayout.LayoutParams(-1,dp(54)));
        Button back=new Button(this);back.setText("Back");back.setAllCaps(false);back.setTextColor(green);content.addView(back,new LinearLayout.LayoutParams(-1,dp(54)));
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(dp(22)+bars.left,dp(20)+bars.top,dp(22)+bars.right,dp(28)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        accept.setOnClickListener(v->saveAcceptance());
        privacy.setOnClickListener(v->{android.content.Intent i=new android.content.Intent(this,PrivacyControlCenterActivity.class);startActivity(i);});
        back.setOnClickListener(v->finish());
    }

    private void buildAuthRecovery(){
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(28),dp(22),dp(28));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(light);

        TextView title=text("Terms & Community Guidelines",27,true);
        title.setGravity(Gravity.CENTER);
        root.addView(title,new LinearLayout.LayoutParams(-1,dp(70)));

        TextView info=text("Your Azure session is not active. Sign in again to open the Terms & Community Guidelines.",16,false);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(-1,-2);
        ilp.setMargins(0,dp(18),0,dp(18));
        root.addView(info,ilp);

        Button signIn=Premium2030Ui.primary(this,"Sign in with Azure");
        Premium2030Ui.addButton(root,signIn);
        Button back=Premium2030Ui.secondary(this,"Back");
        Premium2030Ui.addButton(root,back);

        signIn.setOnClickListener(v->startActivity(new Intent(this,AzureExternalAuthActivity.class)));
        back.setOnClickListener(v->finish());
        setContentView(root);

        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(dp(22)+bars.left,dp(28)+bars.top,dp(22)+bars.right,dp(28)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void saveAcceptance(){
        if(!agree.isChecked()){LanguageManager.toast(this,"Please read and accept the Terms & Community Guidelines.",Toast.LENGTH_LONG).show();return;}
        accept.setEnabled(false);accept.setText("Saving securely…");
        AzureApiClient.post("/terms/accept","{}",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                runOnUiThread(()->{setResult(RESULT_OK);LanguageManager.toast(TermsAndCommunityGuidelinesActivity.this,"Terms accepted. Azure has saved your acceptance.",Toast.LENGTH_LONG).show();finish();});
            }
            @Override public void err(String message){
                runOnUiThread(()->{accept.setEnabled(true);accept.setText("Accept & Continue");LanguageManager.toast(TermsAndCommunityGuidelinesActivity.this,"Could not save acceptance: "+message,Toast.LENGTH_LONG).show();});
            }
        });
    }
}
