package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.*;

/** Real rewarded-ad entry point. Credits are granted only by verified AdMob SSV callback. */
public class RewardedMessageActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFunctions functions;
    private RewardedAd rewardedAd;
    private LinearLayout root;
    private Button watch;
    private TextView status;
    private String productionUnit;
    private ConsentInformation consentInformation;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = FirebaseAuth.getInstance();
        functions = FirebaseFunctions.getInstance();
        base();
        if (auth.getCurrentUser() == null) { status.setText("Please sign in first."); watch.setEnabled(false); return; }
        status.setText("Preparing a real rewarded ad…");
        loadConfig();
    }

    private void base() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,32); root.setBackgroundColor(Color.rgb(247,250,249));
        scroll.addView(root); setContentView(scroll);
        TextView title = new TextView(this); title.setText("Earn 1 Message Credit"); title.setTextSize(26); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setGravity(Gravity.CENTER); title.setTextColor(Color.rgb(30,45,41)); root.addView(title,new LinearLayout.LayoutParams(-1,80));
        status = new TextView(this); status.setTextSize(16); status.setTextColor(Color.rgb(95,108,103)); root.addView(status,new LinearLayout.LayoutParams(-1,90));
        watch = new Button(this); watch.setText("Watch Rewarded Ad"); watch.setAllCaps(false); watch.setEnabled(false); root.addView(watch,new LinearLayout.LayoutParams(-1,64));
        Button back = new Button(this); back.setText("Back"); back.setAllCaps(false); root.addView(back,new LinearLayout.LayoutParams(-1,64)); back.setOnClickListener(v->finish());
        watch.setOnClickListener(v->showRewarded());
        TextView note = new TextView(this); note.setText("Maximum 2 rewarded message credits per UTC day. The backend verifies the AdMob reward before crediting your account."); note.setTextSize(14); note.setTextColor(Color.rgb(95,108,103)); note.setPadding(4,24,4,4); root.addView(note);
    }

    private void loadConfig() {
        functions.getHttpsCallable("getRewardedAdConfig").call(new HashMap<>()).addOnSuccessListener(result -> {
            Map<?,?> data = (Map<?,?>) result.getData();
            if (!Boolean.TRUE.equals(data.get("configured"))) { status.setText("Rewarded ads are not configured for production yet."); return; }
            productionUnit = String.valueOf(data.get("rewardedAdUnitId"));
            if (!productionUnit.matches("ca-app-pub-\\d{16}/\\d+")) { status.setText("Production rewarded-ad configuration is invalid."); return; }
            requestConsentThenLoad();
        }).addOnFailureListener(e -> status.setText("Could not load secure rewarded-ad configuration."));
    }

    private void requestConsentThenLoad() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(this, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(this, formError -> {
                    if (formError != null) {
                        status.setText("Privacy consent could not be completed. Please try again later.");
                        return;
                    }
                    initializeAdsIfAllowed();
                }),
                error -> status.setText("Privacy consent status could not be loaded. Please try again later."));
    }

    private void initializeAdsIfAllowed() {
        if (consentInformation == null || !consentInformation.canRequestAds()) {
            status.setText("Ads cannot be requested until privacy consent is available.");
            return;
        }
        MobileAds.initialize(this, s -> loadRewarded());
    }

    private void loadRewarded() {
        watch.setEnabled(false); status.setText("Loading real rewarded ad…");
        RewardedAd.load(this, productionUnit, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                String uid = auth.getUid();
                rewardedAd.setServerSideVerificationOptions(new ServerSideVerificationOptions.Builder()
                        .setUserId(uid)
                        .setCustomData(uid)
                        .build());
                watch.setEnabled(true); status.setText("Ad ready. Watch it fully to earn 1 message credit.");
            }
            @Override public void onAdFailedToLoad(LoadAdError error) { rewardedAd = null; status.setText("Rewarded ad unavailable right now. Please try again later."); }
        });
    }

    private void showRewarded() {
        if (rewardedAd == null) { loadRewarded(); return; }
        watch.setEnabled(false); status.setText("Reward in progress…");
        rewardedAd.show(this, reward -> { RewardItem ignored = reward; status.setText("Reward received. Waiting for secure server verification…"); });
        rewardedAd = null;
    }
}
