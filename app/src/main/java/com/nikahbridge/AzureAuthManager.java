package com.nikahbridge;

import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import org.json.JSONObject;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AcquireTokenSilentParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalException;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

final class AzureAuthManager {
    static final String API_SCOPE =
            "api://4733ae40-3b89-4994-b99b-3890bf87e876/access_as_user";

    interface Callback {
        void ok(String accessToken);
        void err(String message);
    }

    private static IMultipleAccountPublicClientApplication app;
    private static Context appContext;
    private static WeakReference<Activity> activeActivity = new WeakReference<>(null);
    private static boolean initializing;
    private static String cachedAccessToken;
    private static final java.util.ArrayList<Runnable> pending = new java.util.ArrayList<>();

    private AzureAuthManager() {}

    static void bindActivity(Activity activity) {
        if (activity != null) {
            activeActivity = new WeakReference<>(activity);
            appContext = activity.getApplicationContext();
        }
    }

    static void acquireToken(Context context, Callback callback) {
        if (context instanceof Activity) bindActivity((Activity) context);
        appContext = context.getApplicationContext();

        // Reuse the already verified in-memory production token while the
        // current app session is alive. This prevents every feature request
        // from restarting authentication/navigation.
        synchronized (AzureAuthManager.class) {
            if (cachedAccessToken != null && !cachedAccessToken.trim().isEmpty()) {
                if (isTokenUsable(cachedAccessToken)) {
                    callback.ok(cachedAccessToken);
                    return;
                }
                cachedAccessToken = null;
            }
        }

        initialize(appContext, () -> acquireTokenSilent(callback),
                message -> callback.err(message));
    }

    static void acceptVerifiedAccessToken(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) return;
        synchronized (AzureAuthManager.class) {
            cachedAccessToken = accessToken;
        }
    }

    static void clearCachedToken() {
        synchronized (AzureAuthManager.class) {
            cachedAccessToken = null;
        }
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
        if (context instanceof Activity) bindActivity((Activity) context);
        appContext = context.getApplicationContext();
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

    static void removeCurrentAccount(Context context, java.util.function.Consumer<Boolean> callback) {
        clearCachedToken();
        initialize(context.getApplicationContext(), () -> {
            try {
                List<IAccount> accounts = app.getAccounts();
                if (accounts == null || accounts.isEmpty()) {
                    context.getSharedPreferences("azure_session", Context.MODE_PRIVATE)
                            .edit().putBoolean("signed_in", false).apply();
                    callback.accept(true);
                    return;
                }
                app.removeAccount(accounts.get(0));
                context.getSharedPreferences("azure_session", Context.MODE_PRIVATE)
                        .edit().putBoolean("signed_in", false).apply();
                callback.accept(true);
            } catch (Exception e) {
                callback.accept(false);
            }
        }, message -> callback.accept(false));
    }

    static void markSignedIn(Context context) {
        context.getSharedPreferences("azure_session", Context.MODE_PRIVATE)
                .edit().putBoolean("signed_in", true).apply();
    }

    static boolean hasAccount(Context context) {
        try {
            if (app == null) {
                return context.getSharedPreferences("azure_session", Context.MODE_PRIVATE)
                        .getBoolean("signed_in", false);
            }
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
            String authority = account.getAuthority();
            if (authority == null || authority.trim().isEmpty()) {
                authority = app.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
            }

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
                                        synchronized (AzureAuthManager.class) {
                                            cachedAccessToken = token;
                                        }
                                        callback.ok(token);
                                    }
                                }

                                @Override public void onError(MsalException exception) {
                                    // Any silent-token failure means the cached/refreshable
                                    // token is not usable for this API right now. Recover
                                    // through the real interactive MSAL flow instead of
                                    // leaving the user on the generic silent-token error.
                                    acquireTokenInteractive(callback);
                                }
                            })
                            .build();

            app.acquireTokenSilentAsync(parameters);
        } catch (Exception e) {
            // If silent acquisition cannot even be started, recover through the
            // foreground MSAL flow rather than exposing a stale generic error.
            acquireTokenInteractive(callback);
        }
    }

    private static void acquireTokenInteractive(Callback callback) {
        Activity activity = activeActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            callback.err("AZURE_INTERACTION_REQUIRED");
            return;
        }

        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(Collections.singletonList(API_SCOPE))
                .withCallback(new AuthenticationCallback() {
                    @Override public void onSuccess(IAuthenticationResult result) {
                        String token = result.getAccessToken();
                        if (token == null || token.trim().isEmpty()) {
                            callback.err("AZURE_ACCESS_TOKEN_UNAVAILABLE");
                        } else {
                            synchronized (AzureAuthManager.class) {
                                cachedAccessToken = token;
                            }
                            callback.ok(token);
                        }
                    }

                    @Override public void onError(MsalException exception) {
                        callback.err("AZURE_INTERACTIVE_TOKEN_FAILED");
                    }

                    @Override public void onCancel() {
                        callback.err("AZURE_AUTH_CANCELLED");
                    }
                })
                .build();

        app.acquireToken(parameters);
    }

    private static boolean isTokenUsable(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return false;
            String json = new String(Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING), java.nio.charset.StandardCharsets.UTF_8);
            long exp = new JSONObject(json).optLong("exp", 0L);
            long now = System.currentTimeMillis() / 1000L;
            return exp > now + 60L;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String safe(String value) {
        return value == null || value.trim().isEmpty()
                ? "AZURE_AUTH_INITIALIZATION_FAILED"
                : value;
    }
}