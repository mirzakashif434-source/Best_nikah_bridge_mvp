package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

/** Post-verification production feature hub. Every item launches an existing real activity; no demo cards or fake data. */
public class PostVerificationFeatureHubActivity extends Activity {
    private LinearLayout root;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);
    @Override protected void onCreate(Bundle b){super.onCreate(b);build();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(8),dp(4),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,Class<?> target,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(primary?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(primary?green:Color.WHITE);g.setCornerRadius(dp(18));if(!primary)g.setStroke(dp(2),green);b.setBackground(g);b.setOnClickListener(v->startActivity(new Intent(this,target)));root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}
    private void build(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(light);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));s.addView(root);setContentView(s);
        TextView h=text("Best Nikah Bridge",29,true);h.setGravity(Gravity.CENTER);root.addView(h);
        root.addView(text("Verified member feature hub",19,true));
        root.addView(text("After identity verification, your real production tools are available here. These buttons open the existing Firebase-backed activities. No demo members, fake results or placeholder actions are used.",15,false));
        root.addView(text("💎 Marriage Planning",20,true));
        button("💎 Nikah Blueprint",NikahBlueprintActivity.class,true);
        button("🔥 Future Life Simulation",FutureLifeSimulationActivity.class,true);
        button("🧠 AI Nikah Mediator",NikahMediatorActivity.class,true);
        button("❤️ Nikah Success Network",NikahSuccessNetworkActivity.class,true);
        root.addView(text("👨‍👩‍👧 Family & Trust",20,true));
        button("Family / Wali Connect",FamilyBridge2Activity.class,true);
        button("Trust Passport",TrustPassportActivity.class,true);
        button("Intent & Identity Verification",IdentityVerificationActivity.class,false);
        root.addView(text("💚 Matching & Communication",20,true));
        button("Why We Matched",WhyWeMatchedActivity.class,true);
        button("Compatibility Deal-Breakers",CompatibilityDealBreakerActivity.class,true);
        button("Compatibility Traffic Light",CompatibilityTrafficLightActivity.class,true);
        button("Safe Communication",SafeCommunicationActivity.class,true);
        button("Nikah Journey / Progress",NikahJourneyActivity.class,true);
        button("AI Nikah Assistant",NikahAssistantActivity.class,true);
        root.addView(text("🛡️ Privacy & Community",20,true));
        button("Privacy Control Center",PrivacyControlCenterActivity.class,true);
        button("Community Chat",CommunityChatActivity.class,true);
        button("Blocked Members",BlockedMembersActivity.class,false);
        button("Help Line",HelpLineActivity.class,false);
        root.addView(text("💳 Account & Premium",20,true));
        button("Premium Plans — 20 / 40 / 60 SAR",PremiumPlansActivity.class,true);
        button("Wallet",WalletActivity.class,false);
        Button back=new Button(this);back.setText("Back to Main Nikah Bridge");back.setAllCaps(false);back.setTextSize(16);back.setTextColor(green);GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(18));g.setStroke(dp(2),green);back.setBackground(g);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->{startActivity(new Intent(this,ProductionMainActivity.class));finish();});
    }
}
