package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Admin-only production Owner Wallet.
 * Google Play purchase verification is recorded in Azure PostgreSQL.
 * Google controls merchant payout timing; Saudi merchant payouts arrive by USD bank wire.
 */
public class OwnerEarningsActivity extends Activity {
    private LinearLayout root;
    private TextView summary;
    private Button refresh,signIn;
    private final int green=Premium2030Ui.GREEN, dark=Premium2030Ui.TEXT, gray=Premium2030Ui.MUTED;

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    private void add(String text,int size,boolean bold){
        TextView t=new TextView(this);
        t.setText(text);t.setTextSize(size);t.setTextColor(bold?dark:gray);
        t.setPadding(dp(8),dp(10),dp(8),dp(10));
        if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        root.addView(t);
    }

    private Button button(String text,boolean filled){
        Button b=new Button(this);
        b.setText(text);b.setAllCaps(false);b.setTextSize(16);
        b.setTextColor(filled?Color.WHITE:green);
        b.setBackgroundColor(filled?green:Color.WHITE);
        b.setMinHeight(dp(58));
        b.setPadding(dp(14),dp(10),dp(14),dp(10));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(5),0,dp(5));
        root.addView(b,lp);
        return b;
    }

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);scroll.setClipToPadding(false);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(24),dp(22),dp(30));
        root.setBackgroundColor(Premium2030Ui.CREAM);
        scroll.addView(root);
        setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(scroll);

        add("Owner Wallet — Google Play → Al Rajhi",25,true);
        add("Real Google Play verified sales + Azure payout tracking. Google controls the actual bank transfer.",14,false);

        summary=new TextView(this);
        summary.setTextSize(17);summary.setTextColor(dark);
        summary.setPadding(dp(10),dp(16),dp(10),dp(16));
        summary.setBackgroundColor(Color.WHITE);
        root.addView(summary);

        refresh=button("Refresh Owner Wallet",true);
        signIn=button("Sign in with Owner / Admin Azure Account",false);
        signIn.setVisibility(android.view.View.GONE);
        Button bank=button("Set Al Rajhi Payout Tracking",false);
        Button received=button("Record Google Payout Received",false);
        Button history=button("View Payout History",false);
        Button play=button("Open Google Play Console",false);
        Button back=button("Back",false);

        refresh.setOnClickListener(v->loadAzure());
        signIn.setOnClickListener(v->startActivity(new Intent(this,AzureExternalAuthActivity.class)));
        bank.setOnClickListener(v->alRajhiTrackingDialog());
        received.setOnClickListener(v->recordGooglePayoutDialog());
        history.setOnClickListener(v->showPayoutHistory());
        play.setOnClickListener(v->openPlayConsole());
        back.setOnClickListener(v->finish());

        add("Important: Best Nikah Bridge does not move Google funds itself. In Saudi Arabia, Google pays the merchant bank account by USD wire. This wallet shows verified sales and records real payouts after Google sends them.",13,false);
        loadAzure();
    }

    private boolean authError(String message){
        if(message==null)return false;
        return message.contains("AZURE_SIGN_IN_REQUIRED")||message.contains("AZURE_AUTH")||message.contains("SIGN_IN")||message.contains("401");
    }

    private void showOwnerSignIn(){
        summary.setText("Please sign in with the owner/admin Azure account.");
        signIn.setVisibility(android.view.View.VISIBLE);
    }

    private void loadAzure(){
        if(!AzureAuthManager.hasAccount(this)){showOwnerSignIn();return;}
        summary.setText("Loading secure Azure Owner Wallet…");
        refresh.setEnabled(false);refresh.setText("Loading Owner Wallet…");
        signIn.setVisibility(android.view.View.GONE);
        AzureApiClient.get("/admin/owner/earnings",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                runOnUiThread(()->{
                    refresh.setEnabled(true);refresh.setText("Refresh Owner Wallet");
                    signIn.setVisibility(android.view.View.GONE);
                    summary.setText(formatDashboard(body));
                });
            }
            @Override public void err(String message){
                runOnUiThread(()->{
                    refresh.setEnabled(true);refresh.setText("Refresh Owner Wallet");
                    if(message!=null&&(message.contains("ADMIN_REQUIRED")||message.contains("403"))){
                        summary.setText("This Azure account is signed in, but owner/admin access is not enabled.");
                    }else if(authError(message)){
                        showOwnerSignIn();
                    }else if(message!=null&&message.contains("404")){
                        summary.setText("Owner Wallet route could not be reached. Tap Refresh Owner Wallet.");
                    }else if(message!=null&&(message.contains("500")||message.contains("502")||message.contains("503")||message.contains("504"))){
                        summary.setText("Owner Wallet service is temporarily unavailable. Tap Refresh Owner Wallet.");
                    }else{
                        summary.setText("Owner Wallet is temporarily unavailable. Check your connection and try Refresh.");
                    }
                });
            }
        });
    }

    private String formatDashboard(String body){
        try{
            JSONObject d=new JSONObject(body);
            StringBuilder s=new StringBuilder();
            s.append("GOOGLE PLAY OWNER WALLET\n\n");
            s.append("This month verified upgrades: ").append(d.optInt("currentMonthUpgrades")).append("\n");
            s.append("This month gross plan value: ").append(money(d.optDouble("currentMonthPlanValueSar"))).append(" SAR\n\n");

            s.append("Total verified upgrades: ").append(d.optInt("totalVerifiedUpgrades")).append("\n");
            s.append("Total gross plan value: ").append(money(d.optDouble("totalVerifiedPlanValueSar"))).append(" SAR\n\n");

            s.append("Paid by Google to bank (recorded): $").append(money(d.optDouble("settledUsd"))).append(" USD\n");
            s.append("Pending tracked payout: $").append(money(d.optDouble("pendingUsd"))).append(" USD\n\n");

            s.append("Payout destination: Al Rajhi / Google Play Payments Profile\n");
            s.append("Saudi payout currency: USD\n");
            s.append("Provider: Google Play");
            return s.toString();
        }catch(Exception e){
            return "Owner Wallet response could not be displayed.";
        }
    }

    private String money(double value){
        return String.format(java.util.Locale.US,"%.2f",value);
    }

    private void alRajhiTrackingDialog(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        EditText label=new EditText(this);
        label.setHint("Bank label");
        label.setText("Al Rajhi Bank");

        EditText masked=new EditText(this);
        masked.setHint("Masked IBAN / last 4 only, e.g. ****1234");

        box.addView(label,new LinearLayout.LayoutParams(-1,dp(58)));
        box.addView(masked,new LinearLayout.LayoutParams(-1,dp(58)));

        LanguageManager.dialog(this)
            .setTitle("Al Rajhi Payout Tracking")
            .setMessage("For safety, save only a masked identifier here. Your full IBAN/SWIFT must be configured directly in Google Play Payments Profile.")
            .setView(box)
            .setPositiveButton("Save",(d,w)->{
                String bankLabel=label.getText().toString().trim();
                String destination=masked.getText().toString().trim();
                if(bankLabel.isEmpty()||destination.length()<6){
                    LanguageManager.toast(this,"Enter bank label and masked account identifier.",Toast.LENGTH_LONG).show();
                    return;
                }
                try{
                    JSONObject body=new JSONObject();
                    body.put("country","Saudi Arabia");
                    body.put("currency","USD");
                    body.put("destination",destination);
                    body.put("label",bankLabel);
                    AzureApiClient.post("/admin/owner/settlement-profile",body.toString(),new AzureApiClient.Callback(){
                        @Override public void ok(int code,String response){
                            runOnUiThread(()->{
                                LanguageManager.toast(OwnerEarningsActivity.this,"Al Rajhi payout tracking saved in Azure.",Toast.LENGTH_LONG).show();
                                loadAzure();
                            });
                        }
                        @Override public void err(String message){
                            runOnUiThread(()->{if(authError(message))showOwnerSignIn();else LanguageManager.toast(OwnerEarningsActivity.this,"Could not save payout tracking. Please try again.",Toast.LENGTH_LONG).show();});
                        }
                    });
                }catch(Exception e){
                    LanguageManager.toast(this,"Invalid payout tracking data.",Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel",null)
            .show();
    }

    private void recordGooglePayoutDialog(){
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);

        EditText amount=new EditText(this);
        amount.setHint("Google payout amount in USD");
        amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        EditText reference=new EditText(this);
        reference.setHint("Google payout / bank reference");

        box.addView(amount,new LinearLayout.LayoutParams(-1,dp(58)));
        box.addView(reference,new LinearLayout.LayoutParams(-1,dp(58)));

        LanguageManager.dialog(this)
            .setTitle("Record Google Payout Received")
            .setMessage("Use this only after the Google Play payout actually appears in your Al Rajhi account.")
            .setView(box)
            .setPositiveButton("Record Received",(d,w)->{
                String a=amount.getText().toString().trim();
                String ref=reference.getText().toString().trim();
                if(a.isEmpty()||ref.length()<3){
                    LanguageManager.toast(this,"Enter the real USD amount and payout reference.",Toast.LENGTH_LONG).show();
                    return;
                }
                try{
                    JSONObject body=new JSONObject();
                    body.put("currency","USD");
                    body.put("amount",Double.parseDouble(a));
                    body.put("provider","Google Play");
                    body.put("providerReference",ref);
                    AzureApiClient.post("/admin/owner/provider-settlement",body.toString(),new AzureApiClient.Callback(){
                        @Override public void ok(int code,String response){
                            runOnUiThread(()->{
                                LanguageManager.toast(OwnerEarningsActivity.this,"Google payout recorded as received.",Toast.LENGTH_LONG).show();
                                loadAzure();
                            });
                        }
                        @Override public void err(String message){
                            runOnUiThread(()->{if(authError(message))showOwnerSignIn();else LanguageManager.toast(OwnerEarningsActivity.this,"Payout could not be recorded. Please try again.",Toast.LENGTH_LONG).show();});
                        }
                    });
                }catch(Exception e){
                    LanguageManager.toast(this,"Enter a valid payout amount.",Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel",null)
            .show();
    }

    private void showPayoutHistory(){
        AzureApiClient.get("/admin/owner/earnings",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                runOnUiThread(()->{
                    try{
                        JSONArray a=new JSONObject(body).optJSONArray("providerPayouts");
                        if(a==null||a.length()==0){
                            LanguageManager.dialog(OwnerEarningsActivity.this)
                                .setTitle("Google Payout History")
                                .setMessage("No Google payout has been recorded yet.")
                                .setPositiveButton("OK",null).show();
                            return;
                        }
                        StringBuilder out=new StringBuilder();
                        for(int i=0;i<Math.min(a.length(),50);i++){
                            JSONObject p=a.optJSONObject(i);
                            if(p==null)continue;
                            out.append(p.optString("provider","Google Play"))
                               .append(" • ")
                               .append(money(p.optDouble("amount_minor")/100.0))
                               .append(" ")
                               .append(p.optString("currency","USD"))
                               .append("\nRef: ")
                               .append(p.optString("provider_reference",""))
                               .append("\n")
                               .append(p.optString("created_at",""))
                               .append("\n\n");
                        }
                        LanguageManager.dialog(OwnerEarningsActivity.this)
                            .setTitle("Google Payout History")
                            .setMessage(out.toString())
                            .setPositiveButton("OK",null).show();
                    }catch(Exception e){
                        LanguageManager.toast(OwnerEarningsActivity.this,"Payout history could not be displayed.",Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override public void err(String message){
                runOnUiThread(()->{if(authError(message))showOwnerSignIn();else LanguageManager.toast(OwnerEarningsActivity.this,"Payout history is temporarily unavailable.",Toast.LENGTH_LONG).show();});
            }
        });
    }

    private void openPlayConsole(){
        try{
            Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/console/"));
            startActivity(i);
        }catch(Exception e){
            LanguageManager.toast(this,"Open Play Console in your browser and go to Settings → Payments profile.",Toast.LENGTH_LONG).show();
        }
    }
}
