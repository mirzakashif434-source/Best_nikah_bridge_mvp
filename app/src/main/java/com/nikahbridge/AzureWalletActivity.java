package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;

import org.json.JSONObject;

public class AzureWalletActivity extends Activity {
    private LinearLayout root;
    private void base(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(22,24,22,32);root.setBackgroundColor(Color.rgb(247,250,249));ScrollView s=new ScrollView(this);s.addView(root);setContentView(s);}
    private TextView t(String x){TextView v=new TextView(this);v.setText(x);v.setTextSize(16);v.setTextColor(Color.rgb(30,45,41));v.setPadding(6,10,6,12);return v;}
    private Button b(String x){Button v=new Button(this);v.setText(x);v.setAllCaps(false);root.addView(v,new LinearLayout.LayoutParams(-1,62));return v;}
    @Override protected void onCreate(Bundle state){super.onCreate(state);base();root.addView(t("Azure Wallet"));TextView status=t("Loading real wallet…");root.addView(status);
        AzureWalletApi.get("/wallet",new AzureWalletApi.Callback(){public void onSuccess(String body){runOnUiThread(()->{try{JSONObject w=new JSONObject(body).optJSONObject("wallet");status.setText(w==null?body:"Balance: "+w.optString("balance")+" "+w.optString("currency"));}catch(Exception e){status.setText(body);}});}public void onError(String m){runOnUiThread(()->status.setText("Wallet unavailable: "+m));}});
        Button tx=b("View Real Transactions");tx.setOnClickListener(v->AzureWalletApi.get("/wallet/transactions",new AzureWalletApi.Callback(){public void onSuccess(String body){runOnUiThread(()->status.setText(body));}public void onError(String m){runOnUiThread(()->status.setText("Transactions unavailable: "+m));}}));
        Button back=b("Back");back.setOnClickListener(v->finish());
    }
}
