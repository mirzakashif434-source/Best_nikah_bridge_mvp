package com.nikahbridge;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AzureNotificationWorker extends Worker {
    public AzureNotificationWorker(@NonNull Context appContext,@NonNull WorkerParameters params){super(appContext,params);}
    @NonNull @Override public Result doWork(){
        if(!AzureAuthManager.hasAccount(getApplicationContext()))return Result.success();
        CountDownLatch latch=new CountDownLatch(1);
        AzureNotificationManager.syncNow(getApplicationContext(),latch::countDown);
        try{
            return latch.await(25,TimeUnit.SECONDS)?Result.success():Result.retry();
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
            return Result.retry();
        }
    }
}
