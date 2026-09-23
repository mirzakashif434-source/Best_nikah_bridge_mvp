package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Production feature hub with the locked premium 2030 visual system.
 * Azure behavior is preserved; this class changes presentation/navigation only.
 */
public class ProductionMainActivity extends Activity {
    private LinearLayout root;
    private boolean urdu=false;

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

    private int dp(int v){return Premium2030Ui.dp(this,v);}

    private void base(){
        ScrollView s=new ScrollView(this);
        s.setFillViewport(true);
        s.setVerticalScrollBarEnabled(false);
        s.setBackgroundColor(Premium2030Ui.CREAM);

        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(32));
        s.addView(root);
        setContentView(s);
    }

    private void addButton(Button b){Premium2030Ui.addButton(root,b);}
    private void open(Class<?> cls){startActivity(new Intent(this,cls));}

    private LinearLayout featureCard(String title,String body,String chip){
        LinearLayout card=Premium2030Ui.card(this);
        TextView c=Premium2030Ui.chip(this,chip);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,-2);
        card.addView(c,cp);
        card.addView(Premium2030Ui.section(this,title));
        TextView desc=Premium2030Ui.subtitle(this,body);
        desc.setGravity(android.view.Gravity.START);
        desc.setPadding(0,0,0,dp(8));
        card.addView(desc);
        root.addView(card);
        return card;
    }

    private void addCardAction(String title,String body,String chip,String buttonLabel,Class<?> target,boolean primary){
        LinearLayout card=featureCard(title,body,chip);
        Button b=primary?Premium2030Ui.primary(this,buttonLabel):Premium2030Ui.secondary(this,buttonLabel);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(56));
        card.addView(b,lp);
        b.setOnClickListener(v->open(target));
    }

    private void home(){
        base();

        root.addView(Premium2030Ui.title(this,urdu?"بیسٹ نکاح برج":"Best Nikah Bridge"));
        root.addView(Premium2030Ui.subtitle(this,urdu
            ?"ایمان • خاندان • اعتماد • بہتر مستقبل"
            :"Faith • Family • Trust • A Brighter Tomorrow"));

        root.addView(Premium2030Ui.heroLine(this,urdu
            ?"سنجیدہ نکاح، محفوظ رابطے، مضبوط خاندان"
            :"Serious Nikah • Safe Connections • Stronger Families"));

        LinearLayout quick=Premium2030Ui.card(this);
        quick.addView(Premium2030Ui.section(this,urdu?"آپ کا نکاح سفر":"Your Nikah Journey"));
        TextView q=Premium2030Ui.subtitle(this,urdu
            ?"اصل پروفائل، حقیقی میچز، فیملی/ولی اور پرائیویسی — سب Azure پر محفوظ۔"
            :"Real profile, real matching, Wali support and privacy — securely powered by Azure.");
        q.setGravity(android.view.Gravity.START);
        q.setPadding(0,0,0,dp(8));
        quick.addView(q);
        Button profile=Premium2030Ui.primary(this,urdu?"میرا پروفائل":"My Real Profile");
        quick.addView(profile,new LinearLayout.LayoutParams(-1,dp(56)));
        profile.setOnClickListener(v->open(AzureHomeActivity.class));
        root.addView(quick);

        addCardAction("Discover Matches",
            "Real compatibility matching with privacy, reciprocal preferences and safety rules.",
            "MEANINGFUL MATCHES","Open Matches",GenderFilteredMatchesActivity.class,true);

        addCardAction("Family / Wali Connect",
            "Invite and manage real Wali or family involvement for a more trusted halal journey.",
            "FAMILY FIRST","Open Wali Connect",FamilyBridge2Activity.class,true);

        addCardAction("Identity Verification",
            "Submit your real verification privately to Azure and track authorized review status.",
            "TRUST & SAFETY","Open Verification",IdentityVerificationActivity.class,false);

        addCardAction("Privacy Control Center",
            "Control discoverability, city visibility and profile-photo access.",
            "YOUR PRIVACY","Manage Privacy",PrivacyControlCenterActivity.class,false);

        addCardAction("Premium 20 / 40 / 60",
            "Real Google Play purchases verified by Azure. No fake payment or demo subscription.",
            "GO PREMIUM","View Premium Plans",PremiumPlansActivity.class,true);

        addCardAction("AI Nikah Assistant",
            "Use Azure AI for profile guidance, serious questions and nikah preparation.",
            "AZURE AI","Open AI Assistant",NikahAssistantActivity.class,false);

        addCardAction("Owner Wallet",
            "Track verified Google Play earnings and real Google payout records to Al Rajhi.",
            "OWNER","Open Owner Wallet",OwnerEarningsActivity.class,false);

        addCardAction("Help & Safety",
            "Access help, reports, blocked members and community guidance.",
            "SAFE COMMUNITY","Open Help Line",HelpLineActivity.class,false);

        LinearLayout tools=Premium2030Ui.card(this);
        tools.addView(Premium2030Ui.section(this,"More Controls"));

        Button photo=Premium2030Ui.secondary(this,"Real Profile Photo");
        Button reward=Premium2030Ui.secondary(this,"Rewarded Message Credits");
        Button blocked=Premium2030Ui.secondary(this,"Blocked Members");
        Button admin=Premium2030Ui.secondary(this,"Admin Verification Review");
        Button terms=Premium2030Ui.secondary(this,"Terms & Community Guidelines");
        for(Button b:new Button[]{photo,reward,blocked,admin,terms}){
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));
            lp.setMargins(0,dp(3),0,dp(3));
            tools.addView(b,lp);
        }
        photo.setOnClickListener(v->open(ProfilePhotoActivity.class));
        reward.setOnClickListener(v->open(RewardedMessageActivity.class));
        blocked.setOnClickListener(v->open(BlockedMembersActivity.class));
        admin.setOnClickListener(v->open(VerificationAdminActivity.class));
        terms.setOnClickListener(v->open(TermsAndCommunityGuidelinesActivity.class));
        root.addView(tools);

        Button lang=Premium2030Ui.secondary(this,urdu?"English":"اردو / Urdu");
        addButton(lang);
        lang.setOnClickListener(v->{urdu=!urdu;home();});

        Button out=Premium2030Ui.secondary(this,"Sign out of Azure");
        addButton(out);
        out.setOnClickListener(v->AzureAuthManager.removeCurrentAccount(this,ok->runOnUiThread(()->{
            if(ok){
                startActivity(new Intent(ProductionMainActivity.this,AzureExternalAuthActivity.class));
                finish();
            }else{
                Toast.makeText(this,"Azure sign out failed. Please try again.",Toast.LENGTH_LONG).show();
            }
        })));

        root.addView(Premium2030Ui.subtitle(this,
            "Real people • Real intentions • Family stronger together • Halal today, brighter tomorrow"));
    }
}
