package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.graphics.Color;
import android.content.Intent;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.PublicClientApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class AzureExternalAuthActivity extends Activity {
    private static final String API_BASE =
            "https://bestnikahbredge-prod-fn-dkf3ake6d8gsg7cw.eastus-01.azurewebsites.net";
    private static final String API_SCOPE =
            "api://4733ae40-3b89-4994-b99b-3890bf87e876/access_as_user";

    private IMultipleAccountPublicClientApplication msal;
    private TextView status;
    private IAccount currentAccount;
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Premium2030Ui.CREAM);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(28));
        root.setBackgroundColor(Premium2030Ui.CREAM);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText(LanguageManager.tr(AzureExternalAuthActivity.this,"Microsoft / Azure Sign in"));
        title.setTextSize(25);
        title.setTextColor(Premium2030Ui.TEXT);
        root.addView(title);

        status = new TextView(this);
        status.setText(LanguageManager.tr(AzureExternalAuthActivity.this,"Connecting to Microsoft Entra External ID…"));
        status.setTextSize(16);
        root.addView(status);

        Button signIn = Premium2030Ui.primary(this,LanguageManager.tr(AzureExternalAuthActivity.this,"Continue with Microsoft / Azure"));
        signIn.setEnabled(false);
        Premium2030Ui.addButton(root,signIn);

        Button back = Premium2030Ui.secondary(this,LanguageManager.tr(AzureExternalAuthActivity.this,"Back"));
        Premium2030Ui.addButton(root,back);
        back.setOnClickListener(v -> finish());

        setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(22),dp(28),dp(22),dp(28)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(scroll);

        PublicClientApplication.createMultipleAccountPublicClientApplication(
                this,
                R.raw.auth_config,
                new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override public void onCreated(IMultipleAccountPublicClientApplication application) {
                        msal = application;
                        signIn.setEnabled(true);
                        status.setText(LanguageManager.tr(AzureExternalAuthActivity.this,"Azure External ID is ready. Sign in or create your real account."));
                        signIn.setOnClickListener(v -> acquireTokenInteractive());
                    }
                    @Override public void onError(MsalException exception) {
                        status.setText("Azure authentication setup failed: " + safe(exception.getMessage()));
                    }
                });
    }

    private void acquireTokenInteractive() {
        List<String> scopes = Collections.singletonList(API_SCOPE);
        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(this)
                .withScopes(scopes)
                .withCallback(new AuthenticationCallback() {
                    @Override public void onSuccess(IAuthenticationResult result) {
                        currentAccount = result.getAccount();
                        String accessToken = result.getAccessToken();
                        status.setText("Azure token received. Verifying it with the real Azure backend…");
                        verifyWithAzure(accessToken);
                    }
                    @Override public void onError(MsalException exception) {
                        status.setText("Azure sign-in failed: " + safe(exception.getMessage()));
                    }
                    @Override public void onCancel() {
                        status.setText(LanguageManager.tr(AzureExternalAuthActivity.this,"Azure sign-in cancelled."));
                    }
                })
                .build();
        msal.acquireToken(parameters);
    }

    private void verifyWithAzure(String accessToken) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_BASE + "/api/auth/azure/me");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(20000);
                connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                connection.setRequestProperty("Accept", "application/json");

                int code = connection.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code >= 400 ? connection.getErrorStream() : connection.getInputStream()));
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
                reader.close();

                final String message = "Azure backend HTTP " + code + ": " + body;
                runOnUiThread(() -> {
                    status.setText(message);
                    if (code == 200) {
                        AzureAuthManager.acceptVerifiedAccessToken(accessToken);
                        AzureAuthManager.initialize(this,
                                () -> {
                                    AzureAuthManager.markSignedIn(this);
                                    LanguageManager.toast(AzureExternalAuthActivity.this, "Real Azure login verified.", Toast.LENGTH_LONG).show();
                                    Intent next = FamilyCircleActivity.pendingInvite(AzureExternalAuthActivity.this).isEmpty()
                                            ? new Intent(AzureExternalAuthActivity.this, AzureHomeActivity.class)
                                            : new Intent(AzureExternalAuthActivity.this, FamilyCircleActivity.class);
                                    startActivity(next);
                                    finish();
                                },
                                messageText -> status.setText("Azure session initialization failed: " + messageText));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Azure backend verification failed: " + safe(e.getMessage())));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "unknown error" : value;
    }
}