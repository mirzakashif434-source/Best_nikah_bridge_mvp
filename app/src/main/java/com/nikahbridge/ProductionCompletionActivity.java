package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

/** Azure production completion center. All feature buttons open real production activities. */
public class ProductionCompletionActivity extends Activity {
    private LinearLayout root;private boolean urdu=false;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);dashboard();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView txt(String x,int size,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private void title(String x){TextView t=txt(x,27,true);t.setGravity(Gravity.CENTER);root.addView(t);}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}
    private void open(Class<?> c){startActivity(new Intent(this,c));}

    private void dashboard(){
        base();title(urdu?"بہترین نکاح برج":"Best Nikah Bridge");
        root.addView(txt(urdu?"حقیقی Azure production features — کوئی demo data نہیں":"Real Azure production completion center — no demo data, fake members or fake rewards.",16,false));
        Button lang=btn(urdu?"English":"اردو / Urdu",false);lang.setOnClickListener(v->{urdu=!urdu;dashboard();});
        Button intelligence=btn("Nikah Intelligence Center",true);intelligence.setOnClickListener(v->open(NikahIntelligenceActivity.class));
        Button profile=btn("My Real Azure Profile",true);profile.setOnClickListener(v->open(AzureHomeActivity.class));
        Button photo=btn("Real Profile Photos",true);photo.setOnClickListener(v->open(RealFourPhotoActivity.class));
        Button matches=btn("Real Compatibility Matches",true);matches.setOnClickListener(v->open(GenderFilteredMatchesActivity.class));
        Button family=btn("Family / Wali Bridge",true);family.setOnClickListener(v->open(FamilyBridge2Activity.class));
        Button reward=btn("Rewarded Message Credits",true);reward.setOnClickListener(v->open(RewardedMessageActivity.class));
        Button premium=btn("Premium 20 / 40 / 60 SAR",true);premium.setOnClickListener(v->open(PremiumPlansActivity.class));
        Button verification=btn("Identity Verification",false);verification.setOnClickListener(v->open(IdentityVerificationActivity.class));
        Button privacy=btn("Privacy Control Center",false);privacy.setOnClickListener(v->open(PrivacyControlCenterActivity.class));
        Button admin=btn("Admin Verification Review",false);admin.setOnClickListener(v->open(VerificationAdminActivity.class));
        Button core=btn("Open Azure Home",false);core.setOnClickListener(v->open(AzureHomeActivity.class));
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
        root.addView(txt("All data actions use Azure External ID, Azure Functions, PostgreSQL and Azure Blob Storage. Ad rewards use the production AdMob flow and server verification.",14,false));
    }
}
