package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.content.Intent;

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

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 40, 32, 32);

        TextView title = new TextView(this);
        title.setText("Microsoft / Azure Sign in");
        title.setTextSize(25);
        title.setTextColor(Premium2030Ui.TEXT);
        root.addView(title);

        status = new TextView(this);
        status.setText("Connecting to Microsoft Entra External ID…");
        status.setTextSize(16);
        root.addView(status);

        Button signIn = new Button(this);
        signIn.setText("Continue with Microsoft / Azure");
        signIn.setEnabled(false);
        root.addView(signIn);

        Button back = new Button(this);
        back.setText("Back");
        root.addView(back);
        back.setOnClickListener(v -> finish());

        setContentView(root);

        PublicClientApplication.createMultipleAccountPublicClientApplication(
                this,
                R.raw.auth_config,
                new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override public void onCreated(IMultipleAccountPublicClientApplication application) {
                        msal = application;
                        signIn.setEnabled(true);
                        status.setText("Azure External ID is ready. Sign in or create your real account.");
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
                        status.setText("Azure sign-in cancelled.");
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
                                    LanguageManager.toast(this, "Real Azure login verified.", Toast.LENGTH_LONG).show();
                                    startActivity(new Intent(this, AzureHomeActivity.class));
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