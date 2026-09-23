package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Production Android entry point.
 * Azure External ID is the only active production authentication path.
 * Previous Firebase implementation is preserved in the migration checkpoint branch.
 */
public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        AzureAuthManager.initialize(this,
            ()->runOnUiThread(()->{
                if(AzureAuthManager.hasAccount(this)){
                    startActivity(new Intent(this,AzureHomeActivity.class));
                }else{
                    startActivity(new Intent(this,AzureExternalAuthActivity.class));
                }
                finish();
            }),
            message->runOnUiThread(()->{
                Toast.makeText(this,"Azure authentication setup failed. Please try again.",Toast.LENGTH_LONG).show();
                startActivity(new Intent(this,AzureExternalAuthActivity.class));
                finish();
            })
        );
    }
}
