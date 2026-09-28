package com.nikahbridge;

import android.app.PendingIntent;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class PushMessagingService extends FirebaseMessagingService {
    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        NotificationRegistrar.onNewToken(getApplicationContext(), token);
    }

    @Override public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        NotificationRegistrar.createChannel(this);

        Map<String,String> data = remoteMessage.getData();
        String type = data.get("type");
        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle() : "Best Nikah Bridge";
        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody() : "You have new activity.";
        if (title == null || title.trim().isEmpty()) title = "Best Nikah Bridge";
        if (body == null || body.trim().isEmpty()) body = "You have new activity.";

        Intent intent;
        if ("like".equals(type)) intent = new Intent(this, LikedMeActivity.class);
        else if ("profile_view".equals(type)) intent = new Intent(this, ViewedMeActivity.class);
        else if ("message".equals(type) || "mutual".equals(type))
            intent = new Intent(this, AzureHomeActivity.class);
        else intent = new Intent(this, PostVerificationFeatureHubActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        for (Map.Entry<String,String> entry : data.entrySet()) {
            intent.putExtra(entry.getKey(), entry.getValue());
        }

        PendingIntent pending = PendingIntent.getActivity(
                this,
                (int)(System.currentTimeMillis() & 0x7fffffff),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationRegistrar.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        try {
            NotificationManagerCompat.from(this).notify(
                    (int)(System.currentTimeMillis() & 0x7fffffff),
                    builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ permission is requested from the signed-in activity.
        }
    }
}
