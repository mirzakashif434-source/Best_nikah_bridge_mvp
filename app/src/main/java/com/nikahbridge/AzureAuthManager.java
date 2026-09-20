package com.nikahbridge;

import android.content.Context;

import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.MsalException;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;

import java.util.Collections;
import java.util.List;

/**
 * Real Azure External ID/MSAL session manager.
 * MSAL owns the token cache; access tokens are never persisted by the app.
 */
final class AzureAuthManager {
    static final String API_SCOPE =
            "api://4733ae40-3b89-4994-b99b-3890bf87e876/access_as_user";

    interface Callback {
        void ok(String accessToken);
        void err(String message);
    }

    private static IMultipleAccountPublicClientApplication app;
    private static Context appContext;
    private static boolean initializing;
    private static final java.util.ArrayList<Runnable> pending = new java.util.ArrayList<>();

    private AzureAuthManager() {}

    static void acquireToken(Context context, Callback callback) {
        appContext = context.getApplicationContext();
        initialize(appContext, () -> acquireTokenSilent(callback),
                message -> callback.err(message));
    }

    static void acquireToken(Callback callback) {
        Context context;
        synchronized (AzureAuthManager.class) {
            context = appContext;
        }
        if (context == null) {
            callback.err("AZURE_AUTH_NOT_INITIALIZED");
            return;
        }
        acquireToken(context, callback);
    }

    static void initialize(Context context, Runnable ready, java.util.function.Consumer<String> error) {
        synchronized (AzureAuthManager.class) {
            if (app != null) {
                ready.run();
                return;
            }
            pending.add(ready);
            if (initializing) return;
            initializing = true;
        }

        PublicClientApplication.createMultipleAccountPublicClientApplication(
                context,
                R.raw.auth_config,
                new IPublicClientApplication.IMultipleAccountApplicationCreatedListener() {
                    @Override public void onCreated(IMultipleAccountPublicClientApplication application) {
                        java.util.ArrayList<Runnable> callbacks;
                        synchronized (AzureAuthManager.class) {
                            app = application;
                            initializing = false;
                            callbacks = new java.util.ArrayList<>(pending);
                            pending.clear();
                        }
                        for (Runnable r : callbacks) r.run();
                    }

                    @Override public void onError(MsalException exception) {
                        synchronized (AzureAuthManager.class) {
                            initializing = false;
                            pending.clear();
                        }
                        error.accept(safe(exception.getMessage()));
                    }
                });
    }

    static boolean hasAccount(Context context) {
        try {
            if (app == null) return false;
            List<IAccount> accounts = app.getAccounts();
            return accounts != null && !accounts.isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void acquireTokenSilent(Callback callback) {
        try {
            List<IAccount> accounts = app.getAccounts();
            if (accounts == null || accounts.isEmpty()) {
                callback.err("AZURE_SIGN_IN_REQUIRED");
                return;
            }

            IAccount account = accounts.get(0);
            String authority = app.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();

            AcquireTokenSilentParameters parameters =
                    new AcquireTokenSilentParameters.Builder()
                            .forAccount(account)
                            .withScopes(Collections.singletonList(API_SCOPE))
                            .fromAuthority(authority)
                            .withCallback(new SilentAuthenticationCallback() {
                                @Override public void onSuccess(IAuthenticationResult result) {
                                    String token = result.getAccessToken();
                                    if (token == null || token.trim().isEmpty()) {
                                        callback.err("AZURE_ACCESS_TOKEN_UNAVAILABLE");
                                    } else {
                                        callback.ok(token);
                                    }
                                }

                                @Override public void onError(MsalException exception) {
                                    callback.err("AZURE_SILENT_TOKEN_FAILED");
                                }
                            })
                            .build();

            app.acquireTokenSilentAsync(parameters);
        } catch (Exception e) {
            callback.err("AZURE_SILENT_TOKEN_FAILED");
        }
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty() ? "AZURE_AUTH_INITIALIZATION_FAILED" : value;
    }
}
