package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Production feature hub.
 * Azure External ID + Azure Functions + PostgreSQL + Azure Blob Storage are the
 * production data path. No demo users, fake balances, fake rewards, or sample matches.
 */
public class ProductionMainActivity extends Activity {
    private LinearLayout root;
    private boolean urdu=false;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(90,105,100),light=Color.rgb(247,250,249);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        if(!AzureAuthManager.hasAccount(this)){
            startActivity(new Intent(this,AzureExternalAuthActivity.class));
            finish();
            return;
        }
        home();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(light);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));
        s.addView(root);setContentView(s);
    }
    private TextView text(String x,int size,boolean bold){
        TextView v=new TextView(this);v.setText(x);v.setTextSize(size);v.setTextColor(bold?dark:gray);v.setPadding(dp(6),dp(8),dp(6),dp(10));
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private void title(String x){TextView v=text(x,27,true);v.setGravity(Gravity.CENTER);root.addView(v);}
    private Button button(String label,boolean filled){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(filled?Color.WHITE:green);
        GradientDrawable g=new GradientDrawable();g.setColor(filled?green:Color.WHITE);g.setCornerRadius(dp(18));if(!filled)g.setStroke(dp(2),green);b.setBackground(g);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(62));lp.setMargins(0,dp(5),0,dp(5));root.addView(b,lp);return b;
    }
    private void open(Class<?> cls){startActivity(new Intent(this,cls));}

    private void home(){
        base();title(urdu?"بہترین نکاح برج":"Best Nikah Bridge");
        root.addView(text(urdu?"Production features Azure سے منسلک ہیں۔ کوئی demo data نہیں۔":"Production feature hub — Azure External ID, Azure API, PostgreSQL and Azure Storage. No demo data.",15,false));

        Button lang=button(urdu?"English":"اردو / Urdu",false);
        lang.setOnClickListener(v->{urdu=!urdu;home();});

        Button profile=button("My Real Azure Profile",true);profile.setOnClickListener(v->open(AzureHomeActivity.class));
        Button photo=button("Real Profile Photo",true);photo.setOnClickListener(v->open(ProfilePhotoActivity.class));
        Button family=button("Family / Wali Connect",true);family.setOnClickListener(v->open(FamilyBridge2Activity.class));
        Button premium=button("Premium 20 / 40 / 60 SAR",true);premium.setOnClickListener(v->open(PremiumPlansActivity.class));
        Button reward=button("Rewarded Message Credits",true);reward.setOnClickListener(v->open(RewardedMessageActivity.class));
        Button privacy=button("Privacy Control Center",false);privacy.setOnClickListener(v->open(PrivacyControlCenterActivity.class));
        Button blocked=button("Blocked Members",false);blocked.setOnClickListener(v->open(BlockedMembersActivity.class));
        Button verify=button("Identity Verification",false);verify.setOnClickListener(v->open(IdentityVerificationActivity.class));
        Button help=button("Help Line",false);help.setOnClickListener(v->open(HelpLineActivity.class));
        Button owner=button("Owner Earnings",false);owner.setOnClickListener(v->open(OwnerEarningsActivity.class));
        Button admin=button("Admin Verification Review",false);admin.setOnClickListener(v->open(VerificationAdminActivity.class));
        Button terms=button("Terms & Community Guidelines",false);terms.setOnClickListener(v->open(TermsAndCommunityGuidelinesActivity.class));

        root.addView(text("All server-side actions above use authenticated Azure production APIs. Access-controlled admin screens remain protected by Azure backend roles.",14,false));
        Button out=button("Sign out of Azure",false);
        out.setOnClickListener(v->AzureAuthManager.removeCurrentAccount(this,ok->runOnUiThread(()->{
            if(ok){startActivity(new Intent(ProductionMainActivity.this,AzureExternalAuthActivity.class));finish();}
            else Toast.makeText(this,"Azure sign out failed. Please try again.",Toast.LENGTH_LONG).show();
        })));
    }
}
