package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

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
    private final int green=Premium2030Ui.GREEN, dark=Premium2030Ui.TEXT, gray=Premium2030Ui.MUTED;

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    private void add(String text,int size,boolean bold){
        TextView t=new TextView(this);
        t.setText(text);t.setTextSize(size);t.setTextColor(bold?dark:gray);
        t.setPadding(dp(8),dp(10),dp(8),dp(10));
        if(bold)t.setTypeface(null,1);
        root.addView(t);
    }

    private Button button(String text,boolean filled){
        Button b=new Button(this);
        b.setText(text);b.setAllCaps(false);b.setTextSize(16);
        b.setTextColor(filled?Color.WHITE:green);
        b.setBackgroundColor(filled?green:Color.WHITE);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(62));
        lp.setMargins(0,dp(5),0,dp(5));
        root.addView(b,lp);
        return b;
    }

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);

        ScrollView scroll=new ScrollView(this);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(24),dp(22),dp(30));
        root.setBackgroundColor(Premium2030Ui.CREAM);
        scroll.addView(root);
        setContentView(scroll);

        add("Owner Wallet — Google Play → Al Rajhi",25,true);
        add("Real Google Play verified sales + Azure payout tracking. Google controls the actual bank transfer.",14,false);

        summary=new TextView(this);
        summary.setTextSize(17);summary.setTextColor(dark);
        summary.setPadding(dp(10),dp(16),dp(10),dp(16));
        summary.setBackgroundColor(Color.WHITE);
        root.addView(summary);

        Button refresh=button("Refresh Owner Wallet",true);
        Button bank=button("Set Al Rajhi Payout Tracking",false);
        Button received=button("Record Google Payout Received",false);
        Button history=button("View Payout History",false);
        Button play=button("Open Google Play Console",false);
        Button back=button("Back",false);

        refresh.setOnClickListener(v->loadAzure());
        bank.setOnClickListener(v->alRajhiTrackingDialog());
        received.setOnClickListener(v->recordGooglePayoutDialog());
        history.setOnClickListener(v->showPayoutHistory());
        play.setOnClickListener(v->openPlayConsole());
        back.setOnClickListener(v->finish());

        add("Important: Best Nikah Bridge does not move Google funds itself. In Saudi Arabia, Google pays the merchant bank account by USD wire. This wallet shows verified sales and records real payouts after Google sends them.",13,false);
        loadAzure();
    }

    private void loadAzure(){
        if(!AzureAuthManager.hasAccount(this)){
            summary.setText("Please sign in with Azure as an admin.");
            return;
        }
        summary.setText("Loading secure Azure Owner Wallet…");
        AzureApiClient.get("/admin/owner/earnings",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                runOnUiThread(()->summary.setText(formatDashboard(body)));
            }
            @Override public void err(String message){
                runOnUiThread(()->summary.setText("Owner Wallet unavailable: "+message));
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

        new AlertDialog.Builder(this)
            .setTitle("Al Rajhi Payout Tracking")
            .setMessage("For safety, save only a masked identifier here. Your full IBAN/SWIFT must be configured directly in Google Play Payments Profile.")
            .setView(box)
            .setPositiveButton("Save",(d,w)->{
                String bankLabel=label.getText().toString().trim();
                String destination=masked.getText().toString().trim();
                if(bankLabel.isEmpty()||destination.length()<6){
                    Toast.makeText(this,"Enter bank label and masked account identifier.",Toast.LENGTH_LONG).show();
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
                                Toast.makeText(OwnerEarningsActivity.this,"Al Rajhi payout tracking saved in Azure.",Toast.LENGTH_LONG).show();
                                loadAzure();
                            });
                        }
                        @Override public void err(String message){
                            runOnUiThread(()->Toast.makeText(OwnerEarningsActivity.this,"Could not save payout tracking: "+message,Toast.LENGTH_LONG).show());
                        }
                    });
                }catch(Exception e){
                    Toast.makeText(this,"Invalid payout tracking data.",Toast.LENGTH_LONG).show();
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

        new AlertDialog.Builder(this)
            .setTitle("Record Google Payout Received")
            .setMessage("Use this only after the Google Play payout actually appears in your Al Rajhi account.")
            .setView(box)
            .setPositiveButton("Record Received",(d,w)->{
                String a=amount.getText().toString().trim();
                String ref=reference.getText().toString().trim();
                if(a.isEmpty()||ref.length()<3){
                    Toast.makeText(this,"Enter the real USD amount and payout reference.",Toast.LENGTH_LONG).show();
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
                                Toast.makeText(OwnerEarningsActivity.this,"Google payout recorded as received.",Toast.LENGTH_LONG).show();
                                loadAzure();
                            });
                        }
                        @Override public void err(String message){
                            runOnUiThread(()->Toast.makeText(OwnerEarningsActivity.this,"Payout not recorded: "+message,Toast.LENGTH_LONG).show());
                        }
                    });
                }catch(Exception e){
                    Toast.makeText(this,"Enter a valid payout amount.",Toast.LENGTH_LONG).show();
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
                            new AlertDialog.Builder(OwnerEarningsActivity.this)
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
                        new AlertDialog.Builder(OwnerEarningsActivity.this)
                            .setTitle("Google Payout History")
                            .setMessage(out.toString())
                            .setPositiveButton("OK",null).show();
                    }catch(Exception e){
                        Toast.makeText(OwnerEarningsActivity.this,"Payout history could not be displayed.",Toast.LENGTH_LONG).show();
                    }
                });
            }
            @Override public void err(String message){
                runOnUiThread(()->Toast.makeText(OwnerEarningsActivity.this,"Payout history unavailable: "+message,Toast.LENGTH_LONG).show());
            }
        });
    }

    private void openPlayConsole(){
        try{
            Intent i=new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/console/"));
            startActivity(i);
        }catch(Exception e){
            Toast.makeText(this,"Open Play Console in your browser and go to Settings → Payments profile.",Toast.LENGTH_LONG).show();
        }
    }
}
