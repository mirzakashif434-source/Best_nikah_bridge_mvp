package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import org.json.JSONObject;

/** Small client-side UX gate. Server endpoints still enforce premium access independently. */
public final class PremiumFeatureGate {
    private PremiumFeatureGate(){}

    public static void require(Activity a,String feature,String requiredPlan,Runnable allowed){
        if(!AzureAuthManager.hasAccount(a)){
            LanguageManager.dialog(a)
                .setTitle("Sign in required")
                .setMessage("Sign in with Azure first so the app can verify your real account and premium access before opening this feature.")
                .setPositiveButton("Sign in",(d,w)->a.startActivity(new Intent(a,AzureExternalAuthActivity.class)))
                .setNegativeButton("Not now",null)
                .show();
            return;
        }
        AzureApiClient.get("/premium/serious-plus-summary",new AzureApiClient.Callback(){
            public void ok(int code,String body){
                a.runOnUiThread(()->{
                    try{
                        JSONObject o=new JSONObject(body);
                        JSONObject f=o.optJSONObject("features");
                        if(f!=null&&f.optBoolean(feature,false)){allowed.run();return;}
                    }catch(Exception ignored){}
                    showUpgrade(a,requiredPlan);
                });
            }
            public void err(String message){a.runOnUiThread(()->showUpgrade(a,requiredPlan));}
        });
    }

    public static void showUpgrade(Activity a,String requiredPlan){
        LanguageManager.dialog(a)
            .setTitle("Serious Nikah Plus")
            .setMessage(requiredPlan+" is required for this advanced feature. Free matching, essential privacy/safety and basic Family Circle remain available.")
            .setPositiveButton("View Plans",(d,w)->a.startActivity(new Intent(a,PremiumPlansActivity.class)))
            .setNegativeButton("Not now",null)
            .show();
    }
}
