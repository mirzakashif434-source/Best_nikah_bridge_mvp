package com.nikahbridge;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

/**
 * Additive navigation helper. It does not replace any Activity layout or flow.
 * Secondary screens get a consistent visible Back button when they do not
 * already provide one. Main screens additionally receive production entries
 * for Global Community Chat and the real rewarded-message flow.
 *
 * Privacy consent is initialized when the first Activity resumes through
 * Google's UMP SDK. Ad requests remain controlled by the consent state; no
 * test/demo ad IDs are used.
 */
public class NikahBridgeApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final int BACK_TAG = 0x4E42424B;
    private static final int COMMUNITY_TAG = 0x4E42434D;
    private static final int REWARD_TAG = 0x4E425257;
    private ConsentInformation consentInformation;
    private boolean privacyConsentStarted;

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    private void initializePrivacyConsent(Activity activity) {
        if (privacyConsentStarted || activity == null || activity.isFinishing()) return;
        privacyConsentStarted = true;
        consentInformation = UserMessagingPlatform.getConsentInformation(getApplicationContext());
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(activity, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    if (formError != null) android.util.Log.w("BestNikahBridge", "UMP consent form: " + formError.getMessage());
                }),
                error -> android.util.Log.w("BestNikahBridge", "UMP consent update: " + error.getMessage()));
    }

    public boolean canRequestAds() { return consentInformation != null && consentInformation.canRequestAds(); }

    private int dp(Activity a,int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
    private boolean isMainScreen(Activity a){String n=a.getClass().getSimpleName();return "WelcomeActivity".equals(n)||"MainActivity".equals(n)||"ProductionMainActivity".equals(n);}
    private boolean isCommunityHost(Activity a){String n=a.getClass().getSimpleName();return "MainActivity".equals(n)||"ProductionMainActivity".equals(n);}
    private boolean containsBack(View v){if(v instanceof TextView){CharSequence text=((TextView)v).getText();if(text!=null&&"Back".equalsIgnoreCase(text.toString().trim()))return true;CharSequence desc=v.getContentDescription();if(desc!=null&&"Back".equalsIgnoreCase(desc.toString().trim()))return true;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsBack(g.getChildAt(i)))return true;}return false;}
    private void addBack(Activity a){if(a.isFinishing()||isMainScreen(a))return;ViewGroup content=a.findViewById(android.R.id.content);if(content==null||content.getTag(BACK_TAG)!=null||containsBack(content)||!(content instanceof FrameLayout))return;Button back=new Button(a);back.setText("‹  Back");back.setAllCaps(false);back.setTextSize(15);back.setTextColor(Color.rgb(18,103,82));back.setContentDescription("Back");back.setElevation(dp(a,5));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(a,22));bg.setStroke(dp(a,1),Color.rgb(18,103,82));back.setBackground(bg);back.setPadding(dp(a,8),0,dp(a,10),0);back.setOnClickListener(v->a.finish());FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(a,104),dp(a,48),Gravity.TOP|Gravity.START);lp.leftMargin=dp(a,12);lp.topMargin=dp(a,10);content.addView(back,lp);content.setTag(BACK_TAG,Boolean.TRUE);}
    private void addCommunityEntry(Activity a){if(!isCommunityHost(a)||a.isFinishing())return;ViewGroup content=a.findViewById(android.R.id.content);if(content==null||content.getTag(COMMUNITY_TAG)!=null||!(content instanceof FrameLayout))return;Button community=new Button(a);community.setText("🌍  Global Community Chat");community.setAllCaps(false);community.setTextSize(14);community.setTextColor(Color.WHITE);community.setContentDescription("Global Community Chat");community.setElevation(dp(a,7));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(18,103,82));bg.setCornerRadius(dp(a,22));community.setBackground(bg);community.setPadding(dp(a,8),0,dp(a,8),0);community.setOnClickListener(v->a.startActivity(new Intent(a,CommunityChatActivity.class)));FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(a,220),dp(a,54),Gravity.TOP|Gravity.END);lp.rightMargin=dp(a,12);lp.topMargin=dp(a,10);content.addView(community,lp);content.setTag(COMMUNITY_TAG,Boolean.TRUE);}
    private void addRewardEntry(Activity a){if(!isCommunityHost(a)||a.isFinishing())return;ViewGroup content=a.findViewById(android.R.id.content);if(content==null||content.getTag(REWARD_TAG)!=null||!(content instanceof FrameLayout))return;Button reward=new Button(a);reward.setText("🎁  Earn 1 Message Credit");reward.setAllCaps(false);reward.setTextSize(14);reward.setTextColor(Color.WHITE);reward.setContentDescription("Earn Message Credit");reward.setElevation(dp(a,7));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.rgb(35,92,72));bg.setCornerRadius(dp(a,22));reward.setBackground(bg);reward.setPadding(dp(a,8),0,dp(a,8),0);reward.setOnClickListener(v->a.startActivity(new Intent(a,RewardedMessageActivity.class)));FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(a,220),dp(a,54),Gravity.TOP|Gravity.END);lp.rightMargin=dp(a,12);lp.topMargin=dp(a,72);content.addView(reward,lp);content.setTag(REWARD_TAG,Boolean.TRUE);}
    @Override public void onActivityResumed(Activity activity){initializePrivacyConsent(activity);addBack(activity);addCommunityEntry(activity);addRewardEntry(activity);}
    @Override public void onActivityCreated(Activity activity,Bundle state){}
    @Override public void onActivityStarted(Activity activity){}
    @Override public void onActivityPaused(Activity activity){}
    @Override public void onActivityStopped(Activity activity){}
    @Override public void onActivitySaveInstanceState(Activity activity,Bundle outState){}
    @Override public void onActivityDestroyed(Activity activity){}
}
