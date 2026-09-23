package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real Azure Block + Unblock management. No demo/local-only block state. */
public class BlockedMembersActivity extends Activity {
    private LinearLayout list;
    private TextView status;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        build();
        loadBlocked();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(dp(8),dp(7),dp(8),dp(7));
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private Button button(String label,boolean filled){
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(filled?Color.WHITE:green);
        GradientDrawable g=new GradientDrawable();g.setColor(filled?green:Color.WHITE);g.setCornerRadius(dp(18));if(!filled)g.setStroke(dp(1),green);b.setBackground(g);return b;
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(20),dp(16),dp(28));root.setBackgroundColor(Color.rgb(247,250,249));
        scroll.addView(root);setContentView(scroll);
        TextView title=text("Blocked Members",27,true);title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,dp(55)));
        root.addView(text("Manage members you blocked. Unblock is a real authenticated Azure server-side safety action.",15,false));
        status=text("Status: loading…",14,false);root.addView(status);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);root.addView(list);

        Button refresh=button("Refresh",false);refresh.setOnClickListener(v->loadBlocked());root.addView(refresh,new LinearLayout.LayoutParams(-1,dp(54)));
        Button back=button("Back",false);back.setOnClickListener(v->finish());LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(54));bp.setMargins(0,dp(8),0,0);root.addView(back,bp);
    }

    private void loadBlocked(){
        if(!AzureAuthManager.hasAccount(this)){status.setText("Status: Azure sign in required");return;}
        status.setText("Status: loading real Azure blocked members…");list.removeAllViews();
        AzureApiClient.get("/blocks",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                runOnUiThread(()->{
                    try{
                        JSONArray a=new JSONObject(body).optJSONArray("blocks");
                        list.removeAllViews();
                        if(a==null||a.length()==0){list.addView(text("No members are currently blocked by you.",15,false));status.setText("Status: Azure block list loaded");return;}
                        for(int i=0;i<a.length();i++) addRow(a.optJSONObject(i));
                        status.setText("Status: Azure block list loaded");
                    }catch(Exception e){status.setText("Status: block response could not be read");}
                });
            }
            @Override public void err(String message){runOnUiThread(()->status.setText("Status: could not load Azure blocked members: "+message));}
        });
    }

    private void addRow(JSONObject item){
        if(item==null)return;
        String userId=item.optString("user_id","");
        if(userId.isEmpty())userId=item.optString("blocked_user_id","");
        String name=item.optString("display_name","");
        if(userId.isEmpty())return;

        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(12),dp(10),dp(12),dp(10));
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(14));row.setBackground(bg);
        row.addView(text(name.isEmpty()?"Blocked member":name,16,true));
        row.addView(text("Member ID: "+userId,12,false));
        Button unblock=button("Unblock",true);row.addView(unblock,new LinearLayout.LayoutParams(-1,dp(50)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(5));list.addView(row,lp);
        final String target=userId;
        unblock.setOnClickListener(v->unblock(target,row,unblock));
    }

    private void unblock(String userId,LinearLayout row,Button button){
        button.setEnabled(false);button.setText("Unblocking…");
        AzureApiClient.delete("/blocks/"+userId,"{}",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){runOnUiThread(()->{list.removeView(row);Toast.makeText(BlockedMembersActivity.this,"Member unblocked securely in Azure.",Toast.LENGTH_LONG).show();status.setText("Status: real Azure block removed");});}
            @Override public void err(String message){runOnUiThread(()->{button.setEnabled(true);button.setText("Unblock");Toast.makeText(BlockedMembersActivity.this,"Unblock failed: "+message,Toast.LENGTH_LONG).show();});}
        });
    }
}
