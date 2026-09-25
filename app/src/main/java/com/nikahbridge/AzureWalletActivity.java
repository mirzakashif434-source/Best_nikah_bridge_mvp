package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

public class AzureWalletActivity extends Activity {
    private LinearLayout root;
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(24),dp(22),dp(32));root.setBackgroundColor(Premium2030Ui.CREAM);
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(Premium2030Ui.CREAM);s.addView(root);setContentView(s);
        ViewCompat.setOnApplyWindowInsetsListener(s,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(s);
    }
    private TextView t(String x){TextView v=new TextView(this);v.setText(x);v.setTextSize(17);v.setTextColor(Premium2030Ui.TEXT);v.setPadding(dp(6),dp(10),dp(6),dp(14));return v;}
    private Button b(String x){
        Button v=new Button(this);
        v.setText(LanguageManager.tr(this,x));v.setAllCaps(false);v.setTextSize(16);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setTextColor(Premium2030Ui.GREEN);
        GradientDrawable g=Premium2030Ui.outlined(this,Color.WHITE,Premium2030Ui.GREEN,18);v.setBackground(g);
        v.setMinHeight(dp(58));v.setPadding(dp(14),dp(10),dp(14),dp(10));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(7),0,dp(7));root.addView(v,lp);
        return v;
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);AzureAuthManager.bindActivity(this);base();
        root.addView(Premium2030Ui.title(this,"Azure Wallet"));
        root.addView(Premium2030Ui.subtitle(this,"Real Azure wallet balance and transaction history"));
        TextView status=t("Loading real wallet…");status.setTextIsSelectable(true);root.addView(status);
        recoverAndLoadWallet(status);
        Button refresh=b("Refresh Wallet");refresh.setOnClickListener(v->recoverAndLoadWallet(status));
        Button tx=b("View Real Transactions");tx.setOnClickListener(v->recoverAndLoadTransactions(status));
        Button back=b("Back");back.setOnClickListener(v->finish());
    }

    private void recoverAndLoadWallet(TextView status){
        status.setText("Checking secure Azure session…");
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){runOnUiThread(()->loadWallet(status));}
            @Override public void err(String message){runOnUiThread(()->showAzureSignIn(status,false));}
        });
    }

    private void recoverAndLoadTransactions(TextView status){
        status.setText("Checking secure Azure session…");
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){runOnUiThread(()->loadTransactions(status));}
            @Override public void err(String message){runOnUiThread(()->showAzureSignIn(status,true));}
        });
    }

    private void showAzureSignIn(TextView status,boolean transactions){
        status.setText("Azure sign in is required to load your real "+(transactions?"transactions.":"wallet."));
        LanguageManager.dialog(this)
            .setTitle("Sign in required")
            .setMessage("Sign in with Azure to continue with your real wallet data.")
            .setPositiveButton("Sign in",(d,w)->AzureAuthManager.acquireTokenInteractive(this,new AzureAuthManager.Callback(){
                @Override public void ok(String accessToken){runOnUiThread(()->{
                    if(transactions) loadTransactions(status); else loadWallet(status);
                });}
                @Override public void err(String message){runOnUiThread(()->status.setText("Azure sign in was not completed. Please try again."));}
            }))
            .setNegativeButton("Not now",null)
            .show();
    }

    private void loadWallet(TextView status){
        AzureApiClient.get("/wallet",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject response=new JSONObject(body);
                    JSONObject w=response.optJSONObject("wallet");
                    if(w==null){
                        Object raw=response.opt("wallet");
                        if(raw instanceof String){
                            String value=((String)raw).trim();
                            if(!value.isEmpty()&&!"null".equalsIgnoreCase(value)){try{w=new JSONObject(value);}catch(Exception ignored){}}
                        }
                    }
                    if(w==null){status.setText("Wallet data is not available yet.");return;}
                    String balance=w.optString("balance","0.00");
                    String currency=w.optString("currency","SAR");
                    status.setText("✓ Wallet refreshed from Azure\nBalance: "+balance+" "+currency);
                }catch(Exception e){status.setText("Wallet response could not be displayed.");}
            });}
            public void err(String m){runOnUiThread(()->{
                if(m!=null&&(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("AZURE_INTERACTION_REQUIRED")||m.contains("401"))){
                    AzureAuthManager.clearCachedToken();
                    showAzureSignIn(status,false);
                }else{
                    status.setText("Wallet is temporarily unavailable. Check your connection and tap Refresh Wallet.");
                }
            });}
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
                    if(a==null){
                        Object raw=o.has("transactions")?o.opt("transactions"):o.opt("items");
                        if(raw instanceof String){
                            String value=((String)raw).trim();
                            if(value.isEmpty()||"[]".equals(value)||"null".equalsIgnoreCase(value)){status.setText("No wallet transactions yet.");return;}
                            try{a=new JSONArray(value);}catch(Exception ignored){}
                        }
                    }
                    if(a==null||a.length()==0){status.setText("✓ Azure transactions refreshed\nNo wallet transactions yet.");return;}
                    StringBuilder out=new StringBuilder("✓ Azure transactions refreshed\n\nRecent transactions\n\n");
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
            public void err(String m){runOnUiThread(()->{
                if(m!=null&&(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("AZURE_INTERACTION_REQUIRED")||m.contains("401"))){
                    AzureAuthManager.clearCachedToken();
                    showAzureSignIn(status,true);
                }else{
                    status.setText("Transactions are temporarily unavailable. Please try again.");
                }
            });}
        });
    }
}
