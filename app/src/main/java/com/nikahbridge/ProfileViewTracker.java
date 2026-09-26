package com.nikahbridge;

import android.app.Activity;
import org.json.JSONObject;

/** Records only explicit real profile opens in Azure; never records passive card impressions. */
public final class ProfileViewTracker {
    private ProfileViewTracker(){}

    public static void record(Activity activity,String viewedUserId){
        if(activity==null||viewedUserId==null||viewedUserId.trim().isEmpty())return;
        try{
            JSONObject body=new JSONObject().put("viewedUserId",viewedUserId.trim());
            AzureApiClient.post("/profile-views",body.toString(),new AzureApiClient.Callback(){
                @Override public void ok(int code,String response){}
                @Override public void err(String message){}
            });
        }catch(Exception ignored){}
    }
}
