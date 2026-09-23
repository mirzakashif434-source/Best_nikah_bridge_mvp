package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real Azure admin verification queue/review screen. Backend enforces admin/moderator role. */
public class VerificationAdminActivity extends Activity {
    private LinearLayout list;
    private TextView status;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        AzureAuthManager.bindActivity(this);
        build();
        load();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView t(String s){
        TextView x=new TextView(this);x.setText(s);x.setTextSize(16);x.setTextColor(Color.rgb(30,45,41));x.setPadding(dp(8),dp(12),dp(8),dp(12));return x;
    }
    private Button b(String s){Button x=new Button(this);x.setText(s);x.setAllCaps(false);return x;}

    private void build(){
        ScrollView sc=new ScrollView(this);
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(18),dp(20),dp(18),dp(25));sc.addView(r);setContentView(sc);
        r.addView(t("Identity Verification — Azure Admin Review"));
        status=t("Status: loading…");r.addView(status);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);r.addView(list);
        Button refresh=b("Refresh Queue");refresh.setOnClickListener(v->load());r.addView(refresh,new LinearLayout.LayoutParams(-1,dp(58)));
        Button back=b("Back");back.setOnClickListener(v->finish());r.addView(back,new LinearLayout.LayoutParams(-1,dp(58)));
    }

    private void load(){
        if(!AzureAuthManager.hasAccount(this)){status.setText("Status: Azure sign in required");return;}
        status.setText("Status: loading pending Azure verification requests…");
        list.removeAllViews();
        AzureApiClient.get("/admin/verifications",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                runOnUiThread(()->{
                    try{
                        JSONArray items=new JSONObject(body).optJSONArray("items");
                        list.removeAllViews();
                        if(items==null||items.length()==0){status.setText("Status: no pending verification requests");return;}
                        for(int i=0;i<items.length();i++) add(items.optJSONObject(i));
                        status.setText("Status: Azure pending queue loaded");
                    }catch(Exception e){status.setText("Status: verification response could not be read");}
                });
            }
            @Override public void err(String message){runOnUiThread(()->status.setText("Status: admin access required or Azure queue unavailable")); }
        });
    }

    private void add(JSONObject item){
        if(item==null)return;
        String id=item.optString("id","");
        String user=item.optString("display_name","");
        String email=item.optString("email","");
        String type=item.optString("verification_type","");
        String submitted=item.optString("submitted_at",item.optString("created_at",""));

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(8),dp(8),dp(8),dp(14));
        row.addView(t("Member: "+(user.isEmpty()?email:user)+"\nType: "+type+"\nSubmitted: "+submitted));
        Button ok=b("Approve / Verify");
        Button no=b("Reject");
        row.addView(ok,new LinearLayout.LayoutParams(-1,dp(54)));
        row.addView(no,new LinearLayout.LayoutParams(-1,dp(54)));
        list.addView(row);
        ok.setOnClickListener(v->review(id,"approved"));
        no.setOnClickListener(v->review(id,"rejected"));
    }

    private void review(String id,String decision){
        if(id.isEmpty()){LanguageManager.toast(this,"Verification ID missing.",Toast.LENGTH_LONG).show();return;}
        try{
            JSONObject body=new JSONObject().put("decision",decision);
            AzureApiClient.patch("/admin/verifications/"+id,body.toString(),new AzureApiClient.Callback(){
                @Override public void ok(int code,String response){runOnUiThread(()->{LanguageManager.toast(VerificationAdminActivity.this,"Verification review saved in Azure.",Toast.LENGTH_LONG).show();load();});}
                @Override public void err(String message){runOnUiThread(()->LanguageManager.toast(VerificationAdminActivity.this,"Review failed: "+message,Toast.LENGTH_LONG).show());}
            });
        }catch(Exception e){LanguageManager.toast(this,"Invalid review request.",Toast.LENGTH_LONG).show();}
    }
}
