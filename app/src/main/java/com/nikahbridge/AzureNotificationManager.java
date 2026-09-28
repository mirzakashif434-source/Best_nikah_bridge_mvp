package com.nikahbridge;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

final class AzureNotificationManager {
    static final String CHANNEL_ID="nikah_events";
    private static final String PREF="azure_notifications";
    private static final int REQUEST_NOTIFICATIONS=2609;
    private static final String PERIODIC_WORK="azure-notification-sync";
    private AzureNotificationManager(){}

    static void initialize(Context context){
        if(context==null)return;
        createChannel(context);
        Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest periodic=new PeriodicWorkRequest.Builder(AzureNotificationWorker.class,15,TimeUnit.MINUTES)
                .setConstraints(constraints).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, periodic);
    }

    static void onActivityResumed(Activity activity){
        if(activity==null)return;
        createChannel(activity);
        if(Build.VERSION.SDK_INT>=33
                && AzureAuthManager.hasAccount(activity)
                && ActivityCompat.checkSelfPermission(activity,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            boolean asked=activity.getSharedPreferences(PREF,Context.MODE_PRIVATE).getBoolean("permission_asked",false);
            if(!asked){
                activity.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putBoolean("permission_asked",true).apply();
                ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.POST_NOTIFICATIONS},REQUEST_NOTIFICATIONS);
            }
        }
        if(AzureAuthManager.hasAccount(activity)){
            WorkManager.getInstance(activity).enqueue(new OneTimeWorkRequest.Builder(AzureNotificationWorker.class).build());
        }
    }

    static void syncNow(Context context, Runnable done){
        if(context==null||!AzureAuthManager.hasAccount(context)){if(done!=null)done.run();return;}
        AzureApiClient.get("/notifications",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                try{
                    JSONObject root=new JSONObject(body);
                    JSONArray rows=root.optJSONArray("notifications");
                    if(rows!=null){
                        for(int i=rows.length()-1;i>=0;i--){
                            JSONObject n=rows.optJSONObject(i);
                            if(n==null||!n.isNull("read_at"))continue;
                            showIfNew(context,n);
                        }
                    }
                }catch(Exception ignored){}
                if(done!=null)done.run();
            }
            @Override public void err(String message){if(done!=null)done.run();}
        });
    }

    private static void showIfNew(Context context,JSONObject n){
        String id=n.optString("id","");
        if(id.isEmpty())return;
        android.content.SharedPreferences prefs=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        String raw=prefs.getString("shown_ids","");
        LinkedHashSet<String> shown=new LinkedHashSet<>();
        if(raw!=null&&!raw.isEmpty())for(String s:raw.split(","))if(!s.isEmpty())shown.add(s);
        if(shown.contains(id))return;

        String type=n.optString("type","general");
        String title=n.optString("title","Best Nikah Bridge");
        String body=n.optString("body","You have new activity.");
        Intent intent=targetIntent(context,type);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        android.app.PendingIntent pending=android.app.PendingIntent.getActivity(
                context,Math.abs(id.hashCode()),intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT|android.app.PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder=new NotificationCompat.Builder(context,CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true).setContentIntent(pending);

        try{
            if(Build.VERSION.SDK_INT<33 || ActivityCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
                NotificationManagerCompat.from(context).notify(Math.abs(id.hashCode()),builder.build());
                shown.add(id);
                while(shown.size()>200){String first=shown.iterator().next();shown.remove(first);}
                prefs.edit().putString("shown_ids",String.join(",",shown)).apply();
            }
        }catch(SecurityException ignored){}
    }

    private static Intent targetIntent(Context context,String type){
        if("like".equals(type))return new Intent(context,LikedMeActivity.class);
        if("profile_view".equals(type))return new Intent(context,ViewedMeActivity.class);
        if("message".equals(type)||"mutual".equals(type))return new Intent(context,SafeCommunicationActivity.class);
        if("verification".equals(type))return new Intent(context,IdentityVerificationActivity.class);
        if("family_wali".equals(type))return new Intent(context,FamilyBridge2Activity.class);
        if("wallet".equals(type)||"payout".equals(type))return new Intent(context,AzureWalletActivity.class);
        return new Intent(context,AzureHomeActivity.class);
    }

    private static void createChannel(Context context){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationManager manager=context.getSystemService(NotificationManager.class);
            if(manager==null)return;
            NotificationChannel channel=new NotificationChannel(
                    CHANNEL_ID,"Likes, views, messages and account activity",NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Important Best Nikah Bridge activity");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }
    }
}
