package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class AzureWalletActivity extends Activity {
    private LinearLayout root;
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(24),dp(22),dp(32));root.setBackgroundColor(Premium2030Ui.CREAM);ScrollView s=new ScrollView(this);s.setFillViewport(true);s.addView(root);setContentView(s);}
    private TextView t(String x){TextView v=new TextView(this);v.setText(x);v.setTextSize(16);v.setTextColor(Premium2030Ui.TEXT);v.setPadding(dp(6),dp(10),dp(6),dp(12));return v;}
    private Button b(String x){Button v=Premium2030Ui.secondary(this,x);Premium2030Ui.addButton(root,v);return v;}
    @Override protected void onCreate(Bundle state){super.onCreate(state);AzureAuthManager.bindActivity(this);base();root.addView(Premium2030Ui.title(this,"Azure Wallet"));TextView status=t("Loading real wallet…");status.setTextIsSelectable(true);root.addView(status);
        loadWallet(status);
        Button tx=b("View Real Transactions");tx.setOnClickListener(v->loadTransactions(status));
        Button back=b("Back");back.setOnClickListener(v->finish());
    }

    private void loadWallet(TextView status){
        AzureApiClient.get("/wallet",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject w=new JSONObject(body).optJSONObject("wallet");
                    if(w==null){status.setText("Wallet data is not available yet.");return;}
                    status.setText("Balance: "+w.optString("balance","0")+" "+w.optString("currency","SAR"));
                }catch(Exception e){status.setText("Wallet response could not be displayed.");}
            });}
            public void err(String m){runOnUiThread(()->status.setText("Wallet unavailable: "+m));}
        });
    }

    private void loadTransactions(TextView status){
        status.setText("Loading transactions…");
        AzureApiClient.get("/wallet/transactions",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject o=new JSONObject(body);
                    JSONArray a=o.optJSONArray("transactions");
                    if(a==null)a=o.optJSONArray("items");
                    if(a==null||a.length()==0){status.setText("No wallet transactions yet.");return;}
                    StringBuilder out=new StringBuilder("Recent transactions\n\n");
                    for(int i=0;i<Math.min(a.length(),50);i++){
                        JSONObject x=a.optJSONObject(i);if(x==null)continue;
                        out.append(x.optString("type",x.optString("kind","Transaction")))
                           .append(" • ").append(x.optString("amount",""))
                           .append(" ").append(x.optString("currency",""))
                           .append("\n").append(x.optString("created_at",x.optString("createdAt","")))
                           .append("\n\n");
                    }
                    status.setText(out.toString());
                }catch(Exception e){status.setText("Transactions could not be displayed.");}
            });}
            public void err(String m){runOnUiThread(()->status.setText("Transactions unavailable: "+m));}
        });
    }
}
