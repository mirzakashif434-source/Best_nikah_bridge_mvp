package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/** Real rewarded-ad entry point. Credits are granted only by verified AdMob SSV callback. */
public class RewardedMessageActivity extends Activity {
    private RewardedAd rewardedAd;
    private LinearLayout root;
    private Button watch;
    private TextView status;
    private String productionUnit;
    private String azureSubject;
    private static final String AZURE_REWARDED_CONFIG_URL = "https://bestnikahbredge-prod-fn-dkf3ake6d8gsg7cw.eastus-01.azurewebsites.net/api/admob/rewarded/config";
    private ConsentInformation consentInformation;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        base();
        recoverAzureSessionAndLoad();
    }

    private void recoverAzureSessionAndLoad() {
        status.setText("Checking your secure Azure session…");
        watch.setEnabled(false);
        AzureAuthManager.acquireToken(this, new AzureAuthManager.Callback() {
            @Override public void ok(String accessToken) {
                runOnUiThread(() -> {
                    status.setText("Preparing a real rewarded ad…");
                    loadConfig();
                });
            }
            @Override public void err(String message) {
                runOnUiThread(() -> showAzureRecovery());
            }
        });
    }

    private void base() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setBackgroundColor(Premium2030Ui.CREAM);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,32); root.setBackgroundColor(Premium2030Ui.CREAM);
        scroll.addView(root); setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(28,28,28,32+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(scroll);
        TextView title = Premium2030Ui.title(this,"Earn 1 Message Credit"); root.addView(title);
        status = Premium2030Ui.subtitle(this,""); root.addView(status);
        watch = Premium2030Ui.primary(this,"Watch Rewarded Ad"); watch.setEnabled(false); Premium2030Ui.addButton(root,watch);
        Button back = Premium2030Ui.secondary(this,"Back"); Premium2030Ui.addButton(root,back); back.setOnClickListener(v->finish());
        watch.setOnClickListener(v->showRewarded());
        TextView note = new TextView(this); note.setText("Maximum 2 rewarded message credits per UTC day. The backend verifies the AdMob reward before crediting your account."); note.setTextSize(14); note.setTextColor(Color.rgb(95,108,103)); note.setPadding(4,24,4,4); root.addView(note);
    }

    private void showAzureRecovery() {
        status.setText("Please sign in with Azure to earn rewarded message credits.");
        watch.setEnabled(false);
        Button signIn = Premium2030Ui.primary(this,"Sign in with Azure");
        Premium2030Ui.addButton(root,signIn);
        signIn.setOnClickListener(v -> {
            signIn.setEnabled(false);
            signIn.setText("Opening Azure Sign In…");
            AzureAuthManager.acquireTokenInteractive(this,new AzureAuthManager.Callback(){
                @Override public void ok(String accessToken){runOnUiThread(()->{
                    root.removeView(signIn);
                    recoverAzureSessionAndLoad();
                });}
                @Override public void err(String message){runOnUiThread(()->{
                    signIn.setEnabled(true);
                    signIn.setText("Sign in with Azure");
                    status.setText("Azure sign in was not completed. Please try again.");
                });}
            });
        });
    }

    private void loadConfig() {
        AzureApiClient.get("/admob/rewarded/config", new AzureApiClient.Callback() {
            @Override public void ok(int code, String body) {
                try {
                    JSONObject data = new JSONObject(body);
                    if (!data.optBoolean("configured", false)) throw new IllegalStateException("Rewarded ads are not configured.");
                    String unit = data.optString("rewardedAdUnitId", "");
                    String subject = data.optString("azureSubject", "");
                    if (!unit.matches("ca-app-pub-\\d{16}/\\d+") || subject.trim().isEmpty()) throw new IllegalStateException("Invalid Azure rewarded configuration.");
                    runOnUiThread(() -> {
                        productionUnit = unit;
                        azureSubject = subject;
                        requestConsentThenLoad();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        rewardedAd = null;
                        watch.setEnabled(true);
                        watch.setText("Retry Rewarded Ad");
                        status.setText("Could not load secure Azure rewarded-ad configuration. Tap Retry.");
                    });
                }
            }
            @Override public void err(String message) { runOnUiThread(() -> {
                watch.setEnabled(false);
                if(message!=null&&message.contains("PREMIUM_AD_FREE")){
                    status.setText("Serious Nikah Plus is ad-free. Rewarded ads are disabled for your paid plan.");
                }else if(message!=null&&(message.contains("AZURE_SIGN_IN_REQUIRED")||message.contains("AZURE_INTERACTION_REQUIRED")||message.contains("401"))){
                    AzureAuthManager.clearCachedToken();
                    showAzureRecovery();
                }else if(message!=null&&message.contains("412")){
                    status.setText("Your Azure account is not active yet. Complete account setup, then try again.");
                }else{
                    rewardedAd = null;
                    watch.setEnabled(true);
                    watch.setText("Retry Rewarded Ad");
                    status.setText("Rewarded ad service could not be reached. Tap Retry.");
                }
            }); }
        });
    }

    private void requestConsentThenLoad() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(this, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(this, formError -> {
                    if (formError != null) {
                        rewardedAd = null;
                        watch.setEnabled(true);
                        watch.setText("Retry Rewarded Ad");
                        status.setText("Privacy consent could not be completed. Tap Retry.");
                        return;
                    }
                    initializeAdsIfAllowed();
                }),
                error -> {
                    rewardedAd = null;
                    watch.setEnabled(true);
                    watch.setText("Retry Rewarded Ad");
                    status.setText("Privacy consent status could not be loaded. Tap Retry.");
                });
    }

    private void initializeAdsIfAllowed() {
        if (consentInformation == null || !consentInformation.canRequestAds()) {
            rewardedAd = null;
            watch.setEnabled(true);
            watch.setText("Retry Rewarded Ad");
            status.setText("Ads cannot be requested until privacy consent is available. Tap Retry after consent is available.");
            return;
        }
        MobileAds.initialize(this, s -> loadRewarded());
    }

    private void loadRewarded() {
        watch.setEnabled(false); status.setText("Loading real rewarded ad…");
        RewardedAd.load(this, productionUnit, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                watch.setText("Watch Rewarded Ad");
                String uid = azureSubject;
                rewardedAd.setServerSideVerificationOptions(new ServerSideVerificationOptions.Builder()
                        .setUserId(uid)
                        .setCustomData(uid)
                        .build());
                watch.setEnabled(true); status.setText("Ad ready. Watch it fully to earn 1 message credit.");
            }
            @Override public void onAdFailedToLoad(LoadAdError error) {
                rewardedAd = null;
                watch.setEnabled(true);
                watch.setText("Retry Rewarded Ad");
                status.setText("Rewarded ad unavailable right now. Tap Retry.");
            }
        });
    }

    private void showRewarded() {
        if (rewardedAd == null) {
            watch.setEnabled(false);
            watch.setText("Preparing Rewarded Ad…");
            recoverAzureSessionAndLoad();
            return;
        }
        watch.setEnabled(false); status.setText("Reward in progress…");
        rewardedAd.show(this, reward -> { RewardItem ignored = reward; status.setText("Reward received. Waiting for secure server verification…"); });
        rewardedAd = null;
    }
}
