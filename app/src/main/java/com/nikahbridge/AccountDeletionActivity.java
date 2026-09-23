package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.*;

public class AccountDeletionActivity extends Activity {
    private LinearLayout root;
    private final int dark=Premium2030Ui.TEXT,gray=Premium2030Ui.MUTED;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        AzureAuthManager.bindActivity(this);
        render();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(12));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);return b;}

    private void render(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(24),dp(22),dp(32));sc.addView(root);setContentView(sc);
        root.addView(txt("Account & Privacy",28,true));
        root.addView(txt("Permanent deletion removes account data handled by the Azure production backend, including profile, matches, messages, family links, verification records and stored photos.",16,false));
        Button del=btn("Permanently Delete Account");root.addView(del,new LinearLayout.LayoutParams(-1,dp(70)));
        Button out=btn("Sign out of Azure");root.addView(out,new LinearLayout.LayoutParams(-1,dp(70)));
        Button back=btn("Back");root.addView(back,new LinearLayout.LayoutParams(-1,dp(70)));
        del.setOnClickListener(v->confirmDelete());
        out.setOnClickListener(v->AzureAuthManager.removeCurrentAccount(this,ok->runOnUiThread(()->{
            if(ok){startActivity(new Intent(this,WelcomeActivity.class));finish();}
            else Toast.makeText(this,"Azure sign out failed. Please try again.",Toast.LENGTH_LONG).show();
        })));
        back.setOnClickListener(v->finish());
    }

    private void confirmDelete(){
        new AlertDialog.Builder(this).setTitle("Permanent deletion")
            .setMessage("This permanently deletes your Azure account data. Continue?")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("DELETE",(d,w)->AzureApiClient.delete("/account",null,new AzureApiClient.Callback(){
                public void ok(int c,String s){runOnUiThread(()->AzureAuthManager.removeCurrentAccount(AccountDeletionActivity.this,ignored->runOnUiThread(()->{
                    Toast.makeText(AccountDeletionActivity.this,"Account deleted permanently.",Toast.LENGTH_LONG).show();
                    startActivity(new Intent(AccountDeletionActivity.this,WelcomeActivity.class));finish();
                })));}
                public void err(String e){runOnUiThread(()->Toast.makeText(AccountDeletionActivity.this,"Azure deletion failed. Nothing was confirmed.",Toast.LENGTH_LONG).show());}
            })).show();
    }
}
