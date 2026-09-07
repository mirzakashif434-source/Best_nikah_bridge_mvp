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

/** Additive navigation helper. Existing screens and flows are preserved. */
public class NikahBridgeApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final int BACK_TAG=0x4E42424B, COMMUNITY_TAG=0x4E42434D, REWARD_TAG=0x4E425257, PREMIUM_TAG=0x4E425050, BLUEPRINT_TAG=0x4E424250, MEDIATOR_TAG=0x4E424D44;
    private ConsentInformation consentInformation; private boolean privacyConsentStarted;
    @Override public void onCreate(){super.onCreate();registerActivityLifecycleCallbacks(this);}
    private void initializePrivacyConsent(Activity a){if(privacyConsentStarted||a==null||a.isFinishing())return;privacyConsentStarted=true;consentInformation=UserMessagingPlatform.getConsentInformation(getApplicationContext());ConsentRequestParameters p=new ConsentRequestParameters.Builder().build();consentInformation.requestConsentInfoUpdate(a,p,()->UserMessagingPlatform.loadAndShowConsentFormIfRequired(a,e->{if(e!=null)android.util.Log.w("BestNikahBridge","UMP consent form: "+e.getMessage());}),e->android.util.Log.w("BestNikahBridge","UMP consent update: "+e.getMessage()));}
    public boolean canRequestAds(){return consentInformation!=null&&consentInformation.canRequestAds();}
    private int dp(Activity a,int v){return Math.round(v*a.getResources().getDisplayMetrics().density);}
    private boolean isMainScreen(Activity a){String n=a.getClass().getSimpleName();return "WelcomeActivity".equals(n)||"MainActivity".equals(n)||"ProductionMainActivity".equals(n);}
    private boolean isHost(Activity a){String n=a.getClass().getSimpleName();return "MainActivity".equals(n)||"ProductionMainActivity".equals(n);}
    private boolean containsBack(View v){if(v instanceof TextView){CharSequence t=((TextView)v).getText();if(t!=null&&"Back".equalsIgnoreCase(t.toString().trim()))return true;CharSequence d=v.getContentDescription();if(d!=null&&"Back".equalsIgnoreCase(d.toString().trim()))return true;}if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(containsBack(g.getChildAt(i)))return true;}return false;}
    private void addBack(Activity a){if(a.isFinishing()||isMainScreen(a))return;ViewGroup c=a.findViewById(android.R.id.content);if(c==null||c.getTag(BACK_TAG)!=null||containsBack(c)||!(c instanceof FrameLayout))return;Button b=new Button(a);b.setText("‹  Back");b.setAllCaps(false);b.setTextSize(15);b.setTextColor(Color.rgb(18,103,82));b.setContentDescription("Back");b.setElevation(dp(a,5));GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(a,22));g.setStroke(dp(a,1),Color.rgb(18,103,82));b.setBackground(g);b.setOnClickListener(v->a.finish());FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(a,104),dp(a,48),Gravity.TOP|Gravity.START);lp.leftMargin=dp(a,12);lp.topMargin=dp(a,10);c.addView(b,lp);c.setTag(BACK_TAG,Boolean.TRUE);}
    private Button overlay(Activity a,String text,int width,int top,int tag){Button b=new Button(a);b.setText(text);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(Color.WHITE);b.setElevation(dp(a,7));GradientDrawable g=new GradientDrawable();g.setColor(Color.rgb(18,103,82));g.setCornerRadius(dp(a,22));b.setBackground(g);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(a,width),dp(a,54),Gravity.TOP|Gravity.END);lp.rightMargin=dp(a,12);lp.topMargin=dp(a,top);ViewGroup c=a.findViewById(android.R.id.content);c.addView(b,lp);c.setTag(tag,Boolean.TRUE);return b;}
    private void addCommunityEntry(Activity a){if(!isHost(a)||a.isFinishing())return;ViewGroup c=a.findViewById(android.R.id.content);if(c==null||c.getTag(COMMUNITY_TAG)!=null||!(c instanceof FrameLayout))return;Button b=overlay(a,"🌍  Global Community Chat",220,10,COMMUNITY_TAG);b.setOnClickListener(v->a.startActivity(new Intent(a,CommunityChatActivity.class)));}
    private void addRewardEntry(Activity a){if(!isHost(a)||a.isFinishing())return;ViewGroup c=a.findViewById(android.R.id.content);if(c==null||c.getTag(REWARD_TAG)!=null||!(c instanceof FrameLayout))return;Button b=overlay(a,"🎁  Earn 1 Message Credit",220,72,REWARD_TAG);b.setOnClickListener(v->a.startActivity(new Intent(a,RewardedMessageActivity.class)));}
    private void addPremiumEntry(Activity a){if(!isHost(a)||a.isFinishing())return;ViewGroup c=a.findViewById(android.R.id.content);if(c==null||c.getTag(PREMIUM_TAG)!=null||!(c instanceof FrameLayout))return;Button b=overlay(a,"⭐  Premium Upgrade • 20 / 40 / 60 SAR",250,134,PREMIUM_TAG);b.setOnClickListener(v->a.startActivity(new Intent(a,PremiumPlansActivity.class)));}
    private void addBlueprintEntry(Activity a){if(!isHost(a)||a.isFinishing())return;ViewGroup c=a.findViewById(android.R.id.content);if(c==null||c.getTag(BLUEPRINT_TAG)!=null||!(c instanceof FrameLayout))return;Button b=overlay(a,"💎  Nikah Blueprint",220,196,BLUEPRINT_TAG);b.setOnClickListener(v->a.startActivity(new Intent(a,NikahBlueprintActivity.class)));}
    private void addMediatorEntry(Activity a){if(!isHost(a)||a.isFinishing())return;ViewGroup c=a.findViewById(android.R.id.content);if(c==null||c.getTag(MEDIATOR_TAG)!=null||!(c instanceof FrameLayout))return;Button b=overlay(a,"🧠  AI Nikah Mediator",220,258,MEDIATOR_TAG);b.setOnClickListener(v->a.startActivity(new Intent(a,NikahMediatorActivity.class)));}
    @Override public void onActivityResumed(Activity a){initializePrivacyConsent(a);addBack(a);addCommunityEntry(a);addRewardEntry(a);addPremiumEntry(a);addBlueprintEntry(a);addMediatorEntry(a);}
    @Override public void onActivityCreated(Activity a,Bundle s){} @Override public void onActivityStarted(Activity a){} @Override public void onActivityPaused(Activity a){} @Override public void onActivityStopped(Activity a){} @Override public void onActivitySaveInstanceState(Activity a,Bundle s){} @Override public void onActivityDestroyed(Activity a){}
}
