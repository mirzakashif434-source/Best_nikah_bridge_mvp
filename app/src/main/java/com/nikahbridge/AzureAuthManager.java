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
    private static boolean tokenAcquisitionInFlight;
    private static final java.util.ArrayList<Callback> tokenWaiters = new java.util.ArrayList<>();
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

        // Reuse the verified token while it is still valid.
        synchronized (AzureAuthManager.class) {
            if (cachedAccessToken != null && !cachedAccessToken.trim().isEmpty()) {
                if (isTokenUsable(cachedAccessToken)) {
                    callback.ok(cachedAccessToken);
                    return;
                }
                cachedAccessToken = null;
            }

            // One token acquisition serves every concurrent Azure request.
            // This prevents several screens/API calls from opening duplicate
            // MSAL refresh or interactive sign-in flows at the same time.
            tokenWaiters.add(callback);
            if (tokenAcquisitionInFlight) return;
            tokenAcquisitionInFlight = true;
        }

        initialize(appContext, AzureAuthManager::acquireTokenSilentForWaiters,
                AzureAuthManager::finishTokenError);
    }

    static void acceptVerifiedAccessToken(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) return;
        synchronized (AzureAuthManager.class) {
            cachedAccessToken = accessToken;
        }
        setSignedInState(true);
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
            synchronized (AzureAuthManager.class) {
                if (cachedAccessToken != null && isTokenUsable(cachedAccessToken)) return true;
            }
            boolean remembered = context.getSharedPreferences("azure_session", Context.MODE_PRIVATE)
                    .getBoolean("signed_in", false);
            if (app == null) return remembered;
            List<IAccount> accounts = app.getAccounts();
            boolean cachedAccount = accounts != null && !accounts.isEmpty();
            if (cachedAccount) {
                setSignedInState(true);
                return true;
            }
            // Do not throw away a verified app session merely because MSAL is
            // still restoring its account cache. The next API call will run the
            // real token acquisition and correct stale state if needed.
            return remembered;
        } catch (Exception ignored) {
            return context.getSharedPreferences("azure_session", Context.MODE_PRIVATE)
                    .getBoolean("signed_in", false);
        }
    }

    private static boolean rememberedSignedIn() {
        Context context = appContext;
        return context != null && context.getSharedPreferences("azure_session", Context.MODE_PRIVATE)
                .getBoolean("signed_in", false);
    }

    private static void setSignedInState(boolean signedIn) {
        Context context = appContext;
        if (context != null) {
            context.getSharedPreferences("azure_session", Context.MODE_PRIVATE)
                    .edit().putBoolean("signed_in", signedIn).apply();
        }
    }

    private static void finishTokenSuccess(String token) {
        java.util.ArrayList<Callback> callbacks;
        synchronized (AzureAuthManager.class) {
            cachedAccessToken = token;
            tokenAcquisitionInFlight = false;
            callbacks = new java.util.ArrayList<>(tokenWaiters);
            tokenWaiters.clear();
        }
        setSignedInState(true);
        for (Callback callback : callbacks) callback.ok(token);
    }

    private static void finishTokenError(String message) {
        java.util.ArrayList<Callback> callbacks;
        synchronized (AzureAuthManager.class) {
            tokenAcquisitionInFlight = false;
            callbacks = new java.util.ArrayList<>(tokenWaiters);
            tokenWaiters.clear();
        }
        if ("AZURE_SIGN_IN_REQUIRED".equals(message)) setSignedInState(false);
        for (Callback callback : callbacks) callback.err(message);
    }

    private static void acquireTokenSilentForWaiters() {
        try {
            List<IAccount> accounts = app.getAccounts();
            if (accounts == null || accounts.isEmpty()) {
                if (rememberedSignedIn()) acquireTokenInteractiveForWaiters();
                else finishTokenError("AZURE_SIGN_IN_REQUIRED");
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
                                        finishTokenError("AZURE_ACCESS_TOKEN_UNAVAILABLE");
                                    } else {
                                        finishTokenSuccess(token);
                                    }
                                }

                                @Override public void onError(MsalException exception) {
                                    // Any silent-token failure means the cached/refreshable
                                    // token is not usable for this API right now. Recover
                                    // through one real interactive MSAL flow for all waiters.
                                    acquireTokenInteractiveForWaiters();
                                }
                            })
                            .build();

            app.acquireTokenSilentAsync(parameters);
        } catch (Exception e) {
            acquireTokenInteractiveForWaiters();
        }
    }

    private static void acquireTokenInteractiveForWaiters() {
        Activity activity = activeActivity.get();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            finishTokenError("AZURE_INTERACTION_REQUIRED");
            return;
        }

        AcquireTokenParameters parameters = new AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(Collections.singletonList(API_SCOPE))
                .withCallback(new AuthenticationCallback() {
                    @Override public void onSuccess(IAuthenticationResult result) {
                        String token = result.getAccessToken();
                        if (token == null || token.trim().isEmpty()) {
                            finishTokenError("AZURE_ACCESS_TOKEN_UNAVAILABLE");
                        } else {
                            finishTokenSuccess(token);
                        }
                    }

                    @Override public void onError(MsalException exception) {
                        finishTokenError("AZURE_INTERACTIVE_TOKEN_FAILED");
                    }

                    @Override public void onCancel() {
                        finishTokenError("AZURE_AUTH_CANCELLED");
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