package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.content.Intent;
import android.widget.*;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public class WalletActivity extends Activity {
    private LinearLayout root;
    private TextView balance;
    private int dp(int value){ return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private void add(String s,int size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(bold?Color.rgb(30,45,41):Color.rgb(95,108,103)); t.setPadding(dp(8),dp(10),dp(8),dp(10)); if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD); root.addView(t); }
    private Button btn(String s){ Button b=Premium2030Ui.secondary(this,s); Premium2030Ui.addButton(root,b); return b; }

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);AzureAuthManager.bindActivity(this);
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(24),dp(22),dp(30));sv.addView(root);setContentView(sv);
        add("Best Nikah Bridge Wallet",28,true);
        add("Real Azure wallet ledger. Balances come only from authenticated Azure ledger records and verified transactions.",15,false);
        balance=new TextView(this);balance.setTextSize(20);balance.setTextColor(Color.rgb(30,45,41));balance.setPadding(dp(8),dp(16),dp(8),dp(16));balance.setTextIsSelectable(true);root.addView(balance);
        Button refresh=btn("Refresh Wallet"),premium=btn("Premium Plans — 20 / 40 / 60 SAR"),withdraw=btn("Request Withdrawal"),history=btn("Transaction History"),owner=btn("Owner Earnings & Settlement"),back=btn("Back");
        refresh.setOnClickListener(v->load());premium.setOnClickListener(v->startActivity(new Intent(this,PremiumPlansActivity.class)));withdraw.setOnClickListener(v->withdrawDialog());history.setOnClickListener(v->history());owner.setOnClickListener(v->startActivity(new Intent(this,OwnerEarningsActivity.class)));back.setOnClickListener(v->finish());load();
    }

    private String safeWalletError(String e){
        String m=e==null?"":e;
        if(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("UNAUTHENTICATED")) return "Please sign in with Azure, then try again.";
        if(m.contains("INSUFFICIENT_BALANCE")) return "Insufficient wallet balance for this withdrawal.";
        if(m.contains("MINIMUM_WITHDRAWAL_IS_10")) return "Minimum withdrawal is 10.";
        if(m.contains("WALLET_CURRENCY_MISMATCH")) return "Wallet currency does not match the requested withdrawal currency.";
        if(m.contains("UNSUPPORTED_CURRENCY")) return "This payout currency is not supported.";
        if(m.contains("PAYOUT_DETAILS_REQUIRED")) return "Enter valid payout details.";
        if(m.contains("ACCOUNT_NOT_ACTIVE")) return "This account is not active.";
        return "Wallet service is temporarily unavailable. Please try again.";
    }

    private void load(){
        balance.setText("Loading wallet…");
        AzureWalletApi.get("/wallet",new AzureWalletApi.Callback(){
            public void onSuccess(String body){runOnUiThread(()->{
                try{
                    JSONObject w=new JSONObject(body).optJSONObject("wallet");
                    if(w==null){balance.setText("Wallet data is not available yet.");return;}
                    balance.setText("Balance: "+w.optString("balance","0")+" "+w.optString("currency","SAR"));
                }catch(Exception e){balance.setText("Wallet response could not be displayed.");}
            });}
            public void onError(String e){runOnUiThread(()->balance.setText(safeWalletError(e)));}
        });
    }

    private void withdrawDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        EditText amount=new EditText(this);amount.setHint("Amount (10.00 minimum)");
        EditText currency=new EditText(this);currency.setHint("Currency: SAR or USDT");currency.setText("SAR");
        EditText country=new EditText(this);country.setHint("Country");
        EditText destination=new EditText(this);destination.setHint("Real payout destination / account");
        for(EditText e:new EditText[]{amount,currency,country,destination}){e.setMinHeight(dp(56));box.addView(e,new LinearLayout.LayoutParams(-1,-2));}
        LanguageManager.dialog(this).setTitle("Real Withdrawal Request").setView(box).setMessage("Withdrawal requires verified identity/KYC and is sent to a real payout provider only after review.")
          .setPositiveButton("Submit",(d,w)->{
            String a=amount.getText().toString().trim(),cur=currency.getText().toString().trim().toUpperCase(Locale.US),countryValue=country.getText().toString().trim(),dest=destination.getText().toString().trim();
            if(a.isEmpty()||countryValue.isEmpty()||dest.isEmpty()){LanguageManager.toast(this,"Enter amount, country and payout destination.",Toast.LENGTH_LONG).show();return;}
            String json="{\"amount\":"+quote(a)+",\"currency\":"+quote(cur)+",\"country\":"+quote(countryValue)+",\"destination\":"+quote(dest)+"}";
            AzureWalletApi.post("/wallet/withdrawals",json,new AzureWalletApi.Callback(){
                public void onSuccess(String body){runOnUiThread(()->{String msg="Withdrawal request submitted.";try{JSONObject x=new JSONObject(body).optJSONObject("withdrawal");if(x!=null)msg="Withdrawal submitted\nAmount: "+x.optString("amount")+" "+x.optString("currency")+"\nStatus: "+x.optString("status","pending");}catch(Exception ignored){}LanguageManager.dialog(WalletActivity.this).setTitle("Withdrawal submitted").setMessage(msg).setPositiveButton("OK",null).show();load();});}
                public void onError(String e){runOnUiThread(()->LanguageManager.dialog(WalletActivity.this).setTitle("Withdrawal not submitted").setMessage(safeWalletError(e)).setPositiveButton("Close",null).show());}
            });
          }).setNegativeButton("Cancel",null).show();
    }

    private String quote(String value){return "\""+value.replace("\\","\\\\").replace("\"","\\\"")+"\"";}

    private void history(){
        AzureWalletApi.get("/wallet/transactions",new AzureWalletApi.Callback(){
            public void onSuccess(String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("transactions");
                    if(a==null||a.length()==0){LanguageManager.dialog(WalletActivity.this).setTitle("Azure Wallet Transactions").setMessage("No transactions yet.").setPositiveButton("Close",null).show();return;}
                    StringBuilder s=new StringBuilder();
                    for(int i=0;i<Math.min(a.length(),50);i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;s.append(x.optString("entry_type","Transaction")).append(" • ").append(x.optString("amount","")).append(" ").append(x.optString("currency","")).append("\n").append(x.optString("description","")).append("\n").append(x.optString("created_at","")).append("\n\n");}
                    LanguageManager.dialog(WalletActivity.this).setTitle("Azure Wallet Transactions").setMessage(s.toString()).setPositiveButton("Close",null).show();
                }catch(Exception e){LanguageManager.dialog(WalletActivity.this).setTitle("History unavailable").setMessage("Transactions could not be displayed.").setPositiveButton("Close",null).show();}
            });}
            public void onError(String e){runOnUiThread(()->LanguageManager.dialog(WalletActivity.this).setTitle("History unavailable").setMessage(safeWalletError(e)).setPositiveButton("Close",null).show());}
        });
    }
}
