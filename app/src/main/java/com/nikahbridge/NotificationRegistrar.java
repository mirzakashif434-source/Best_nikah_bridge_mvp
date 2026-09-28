package com.nikahbridge;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;

final class NotificationRegistrar {
    static final String CHANNEL_ID = "nikah_events";
    private static final String PREF = "push_transport";
    private static final int REQUEST_NOTIFICATIONS = 2609;
    private static volatile boolean initialized;

    private NotificationRegistrar() {}

    static void initialize(Context context) {
        if (context == null || initialized) return;
        synchronized (NotificationRegistrar.class) {
            if (initialized) return;
            initialized = true;
        }
        createChannel(context);
        if (!ensureFirebase(context)) return;
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null && !token.trim().isEmpty()) {
                        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                                .edit().putString("token", token).apply();
                        syncToken(context, true);
                    }
                })
                .addOnFailureListener(e ->
                        Log.w("BestNikahBridge", "FCM token unavailable: " + e.getMessage()));
    }

    static void onNewToken(Context context, String token) {
        if (context == null || token == null || token.trim().isEmpty()) return;
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString("token", token).putLong("last_sync", 0L).apply();
        syncToken(context, true);
    }

    static void onActivityResumed(Activity activity) {
        if (activity == null) return;
        createChannel(activity);
        if (Build.VERSION.SDK_INT >= 33
                && AzureAuthManager.hasAccount(activity)
                && ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                   != PackageManager.PERMISSION_GRANTED) {
            boolean asked = activity.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .getBoolean("permission_asked", false);
            if (!asked) {
                activity.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                        .edit().putBoolean("permission_asked", true).apply();
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATIONS);
            }
        }
        syncToken(activity, false);
    }

    static void syncToken(Context context, boolean force) {
        if (context == null || !AzureAuthManager.hasAccount(context)) return;
        String token = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("token", "");
        if (token == null || token.trim().isEmpty()) return;
        long now = System.currentTimeMillis();
        long last = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getLong("last_sync", 0L);
        if (!force && now - last < 5L * 60L * 1000L) return;

        try {
            JSONObject body = new JSONObject();
            body.put("token", token);
            body.put("platform", "android");
            AzureApiClient.post("/notifications/devices", body.toString(), new AzureApiClient.Callback() {
                @Override public void ok(int code, String response) {
                    context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                            .edit().putLong("last_sync", System.currentTimeMillis()).apply();
                }
                @Override public void err(String message) {
                    Log.w("BestNikahBridge", "Push token Azure sync: " + message);
                }
            });
        } catch (Exception e) {
            Log.w("BestNikahBridge", "Push token payload: " + e.getMessage());
        }
    }

    static void unregisterBeforeLogout(Context context, Runnable done) {
        if (context == null) { if (done != null) done.run(); return; }
        String token = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString("token", "");
        if (token == null || token.trim().isEmpty() || !AzureAuthManager.hasAccount(context)) {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit().putLong("last_sync", 0L).apply();
            if (done != null) done.run();
            return;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("token", token);
            AzureApiClient.delete("/notifications/devices/current", body.toString(), new AzureApiClient.Callback() {
                private void finish() {
                    context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                            .edit().putLong("last_sync", 0L).apply();
                    if (done != null) done.run();
                }
                @Override public void ok(int code, String response) { finish(); }
                @Override public void err(String message) {
                    Log.w("BestNikahBridge", "Push unregister: " + message);
                    finish();
                }
            });
        } catch (Exception e) {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .edit().putLong("last_sync", 0L).apply();
            if (done != null) done.run();
        }
    }

    private static boolean ensureFirebase(Context context) {
        try {
            FirebaseApp.getInstance();
            return true;
        } catch (IllegalStateException ignored) {}

        String appId = BuildConfig.FCM_APP_ID;
        String apiKey = BuildConfig.FCM_API_KEY;
        String projectId = BuildConfig.FCM_PROJECT_ID;
        String senderId = BuildConfig.FCM_SENDER_ID;
        if (empty(appId) || empty(apiKey) || empty(projectId) || empty(senderId)) {
            Log.w("BestNikahBridge",
                    "Push transport not configured. FCM build identifiers are missing.");
            return false;
        }
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId(appId)
                    .setApiKey(apiKey)
                    .setProjectId(projectId)
                    .setGcmSenderId(senderId)
                    .build();
            FirebaseApp.initializeApp(context.getApplicationContext(), options);
            return true;
        } catch (Exception e) {
            Log.w("BestNikahBridge", "FCM initialization failed: " + e.getMessage());
            return false;
        }
    }

    static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Likes, views and messages",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Important matrimonial activity and new messages");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }
    }

    static boolean notificationsAllowed(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    private static boolean empty(String v) {
        return v == null || v.trim().isEmpty();
    }
}
