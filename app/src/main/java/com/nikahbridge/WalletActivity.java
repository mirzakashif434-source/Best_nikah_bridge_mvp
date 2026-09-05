package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.content.Intent;
import android.widget.*;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;

public class WalletActivity extends Activity {
    private LinearLayout root;
    private FirebaseFunctions functions;
    private TextView balance;
    private int dp(int value){ return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
    private void add(String s,int size,boolean bold){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(bold?Color.rgb(30,45,41):Color.rgb(95,108,103)); t.setPadding(dp(8),dp(10),dp(8),dp(10)); if(bold)t.setTypeface(null,1); root.addView(t); }
    private Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(Color.rgb(18,103,82)); root.addView(b,new LinearLayout.LayoutParams(-1,dp(62))); return b; }
    @Override protected void onCreate(Bundle state){ super.onCreate(state); functions=FirebaseFunctions.getInstance(); ScrollView sv=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(22),dp(24),dp(22),dp(30)); sv.addView(root); setContentView(sv); add("Best Nikah Bridge Wallet",28,true); add("Real Firebase wallet ledger. No fake balance and no simulated payout.",15,false); balance=new TextView(this); balance.setTextSize(22); balance.setTextColor(Color.rgb(30,45,41)); balance.setPadding(dp(8),dp(16),dp(8),dp(16)); root.addView(balance); Button refresh=btn("Refresh Wallet"); Button premium=btn("Premium Plans — 20 / 40 / 60 SAR"); Button withdraw=btn("Request Withdrawal"); Button history=btn("Transaction History"); Button owner=btn("Owner Earnings & Settlement"); Button back=btn("Back"); refresh.setOnClickListener(v->load()); premium.setOnClickListener(v->startActivity(new Intent(this,PremiumPlansActivity.class))); withdraw.setOnClickListener(v->withdrawDialog()); history.setOnClickListener(v->history()); owner.setOnClickListener(v->startActivity(new Intent(this,OwnerEarningsActivity.class))); back.setOnClickListener(v->finish()); load(); }
    private void load(){ functions.getHttpsCallable("getWallet").call(new HashMap<>()).addOnSuccessListener(r->{Object d=r.getData(); balance.setText(String.valueOf(d));}).addOnFailureListener(e->balance.setText("Wallet unavailable: "+e.getMessage())); }
    private void withdrawDialog(){ LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); EditText amount=new EditText(this); amount.setHint("Amount (10.00 minimum)"); EditText currency=new EditText(this); currency.setHint("Currency: SAR or USDT"); currency.setText("SAR"); EditText country=new EditText(this); country.setHint("Country"); EditText destination=new EditText(this); destination.setHint("Real payout destination / account"); box.addView(amount,new LinearLayout.LayoutParams(-1,dp(56)));box.addView(currency,new LinearLayout.LayoutParams(-1,dp(56)));box.addView(country,new LinearLayout.LayoutParams(-1,dp(56)));box.addView(destination,new LinearLayout.LayoutParams(-1,dp(56))); new AlertDialog.Builder(this).setTitle("Real Withdrawal Request").setView(box).setMessage("Withdrawal requires verified identity/KYC and is sent to a real payout provider only after review.").setPositiveButton("Submit",(d,w)->{Map<String,Object> m=new HashMap<>();m.put("amount",amount.getText().toString().trim());m.put("currency",currency.getText().toString().trim());m.put("country",country.getText().toString().trim());m.put("destination",destination.getText().toString().trim());functions.getHttpsCallable("requestWalletWithdrawal").call(m).addOnSuccessListener(x->{new AlertDialog.Builder(this).setTitle("Withdrawal submitted").setMessage(String.valueOf(x.getData())).setPositiveButton("OK",null).show();load();}).addOnFailureListener(x->new AlertDialog.Builder(this).setTitle("Withdrawal not submitted").setMessage(x.getMessage()).setPositiveButton("OK",null).show());}).setNegativeButton("Cancel",null).show(); }
    private void history(){ functions.getHttpsCallable("listWalletTransactions").call(new HashMap<>()).addOnSuccessListener(r->new AlertDialog.Builder(this).setTitle("Wallet Transactions").setMessage(String.valueOf(r.getData())).setPositiveButton("Close",null).show()).addOnFailureListener(e->new AlertDialog.Builder(this).setTitle("History unavailable").setMessage(e.getMessage()).setPositiveButton("Close",null).show()); }
}
