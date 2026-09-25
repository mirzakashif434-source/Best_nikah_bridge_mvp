package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

public class OwnerLiveAnalyticsActivity extends Activity {
    private LinearLayout root,dynamic;
    private TextView status;
    private Button wallet,signIn,refresh;
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        build();
        load();
    }

    private LinearLayout card(String title,String body){
        LinearLayout c=Premium2030Ui.card(this);
        c.addView(Premium2030Ui.section(this,title));
        TextView b=Premium2030Ui.subtitle(this,body);b.setGravity(Gravity.START);b.setTextIsSelectable(true);c.addView(b);
        dynamic.addView(c);
        return c;
    }

    private Button button(String label,boolean primary){
        Button b=primary?Premium2030Ui.primary(this,label):Premium2030Ui.secondary(this,label);
        Premium2030Ui.addButton(root,b);return b;
    }

    private void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);sc.setBackgroundColor(Premium2030Ui.CREAM);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));sc.addView(root);setContentView(sc);
        ViewCompat.setOnApplyWindowInsetsListener(sc,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(sc);

        root.addView(Premium2030Ui.title(this,"Owner Live Analytics"));
        root.addView(Premium2030Ui.subtitle(this,"Real users • real activity • real upgrades • real payout tracking"));
        root.addView(Premium2030Ui.heroLine(this,"Your business at a glance"));

        status=Premium2030Ui.subtitle(this,"Loading secure owner analytics…");
        status.setGravity(Gravity.START);root.addView(status);

        dynamic=new LinearLayout(this);dynamic.setOrientation(LinearLayout.VERTICAL);root.addView(dynamic,new LinearLayout.LayoutParams(-1,-2));

        refresh=button("Refresh Live Dashboard",true);
        signIn=button("Sign in with Owner / Admin Azure Account",false);
        wallet=button("Open Owner Wallet / Payout",true);
        Button back=button("Back",false);
        signIn.setVisibility(android.view.View.GONE);
        wallet.setVisibility(android.view.View.GONE);
        refresh.setOnClickListener(v->load());
        signIn.setOnClickListener(v->{
            signIn.setEnabled(false);
            signIn.setText("Opening Owner Azure Sign In…");
            AzureAuthManager.acquireTokenInteractive(this,new AzureAuthManager.Callback(){
                @Override public void ok(String accessToken){runOnUiThread(()->{
                    signIn.setEnabled(true);
                    signIn.setText("Sign in with Owner / Admin Azure Account");
                    loadAuthorized();
                });}
                @Override public void err(String message){runOnUiThread(()->{
                    signIn.setEnabled(true);
                    signIn.setText("Sign in with Owner / Admin Azure Account");
                    status.setText("Owner Azure sign in was not completed. Please try again.");
                });}
            });
        });
        wallet.setOnClickListener(v->startActivity(new Intent(this,OwnerEarningsActivity.class)));
        back.setOnClickListener(v->finish());
    }

    private String money(double v){return String.format(java.util.Locale.US,"%.2f",v);}

    private boolean authError(String message){
        if(message==null) return false;
        return message.contains("AZURE_SIGN_IN_REQUIRED") || message.contains("AZURE_AUTH") || message.contains("SIGN_IN") || message.contains("401");
    }

    private void showOwnerSignIn(){
        status.setText("Please sign in with the owner/admin Azure account.");
        signIn.setVisibility(android.view.View.VISIBLE);
    }

    private void load(){
        status.setText("Checking secure owner Azure session…");
        refresh.setEnabled(false);
        refresh.setText("Checking Owner Session…");
        AzureAuthManager.acquireToken(this,new AzureAuthManager.Callback(){
            @Override public void ok(String accessToken){runOnUiThread(()->loadAuthorized());}
            @Override public void err(String message){runOnUiThread(()->{
                refresh.setEnabled(true);
                refresh.setText("Refresh Live Dashboard");
                dynamic.removeAllViews();
                wallet.setVisibility(android.view.View.GONE);
                showOwnerSignIn();
            });}
        });
    }

    private void loadAuthorized(){
        status.setText("Loading live Azure analytics…");
        refresh.setEnabled(false);refresh.setText("Loading Live Dashboard…");
        dynamic.removeAllViews();
        wallet.setVisibility(android.view.View.GONE);
        signIn.setVisibility(android.view.View.GONE);
        AzureApiClient.get("/owner/live-analytics",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                refresh.setEnabled(true);refresh.setText("Refresh Live Dashboard");
                signIn.setVisibility(android.view.View.GONE);
                wallet.setVisibility(android.view.View.VISIBLE);
                render(body);
            });}
            public void err(String m){runOnUiThread(()->{
                refresh.setEnabled(true);refresh.setText("Refresh Live Dashboard");
                wallet.setVisibility(android.view.View.GONE);
                if(m!=null&&(m.contains("ADMIN_REQUIRED")||m.contains("403"))){
                    status.setText("This Azure account is signed in, but owner/admin access is not enabled.");
                }else if(authError(m)){
                    AzureAuthManager.clearCachedToken();
                    showOwnerSignIn();
                }else if(m!=null&&m.contains("404")){
                    status.setText("Owner analytics route could not be reached. Tap Refresh Live Dashboard.");
                }else if(m!=null&&(m.contains("500")||m.contains("502")||m.contains("503")||m.contains("504"))){
                    status.setText("Owner analytics service is temporarily unavailable. Tap Refresh Live Dashboard.");
                }else{
                    status.setText("Owner analytics unavailable. Check your connection and tap Refresh Live Dashboard.");
                }
            });}
        });
    }

    private void render(String body){
        try{
            JSONObject d=new JSONObject(body);
            JSONObject users=d.optJSONObject("users"),profiles=d.optJSONObject("profiles"),engagement=d.optJSONObject("engagement"),
                    premium=d.optJSONObject("premium"),revenue=d.optJSONObject("revenue"),payout=d.optJSONObject("payout"),safety=d.optJSONObject("safety");
            if(users==null||profiles==null||engagement==null||premium==null||revenue==null||payout==null||safety==null){
                status.setText("Owner analytics response is incomplete.");return;
            }
            dynamic.removeAllViews();
            status.setText("Updated: "+d.optString("generatedAt","now"));

            card("Users","Total registered: "+users.optInt("total")+"\nNew today: "+users.optInt("newToday")+"\nNew last 7 days: "+users.optInt("new7d")+"\nOnline now: "+users.optInt("onlineNow")+"\nSeen in last 24h: "+users.optInt("seen24h"));
            card("Verification","Completed profiles: "+profiles.optInt("completed")+"\nVerified photos: "+profiles.optInt("verifiedPhotos")+"\nPending photo reviews: "+profiles.optInt("pendingPhotoReviews"));
            card("Activity","Likes last 24h: "+engagement.optInt("likes24h")+"\nMessages last 24h: "+engagement.optInt("messages24h")+"\nMutual chats: "+engagement.optInt("mutualChats")+"\nTotal likes: "+engagement.optInt("totalLikes")+"\nTotal messages: "+engagement.optInt("totalMessages"));
            card("Premium Members","Active paid: "+premium.optInt("activePaid")+"\nBasic 20 SAR: "+premium.optInt("basic20")+"\nPlus 40 SAR: "+premium.optInt("plus40")+"\nVIP 60 SAR: "+premium.optInt("vip60"));
            card("Revenue","Today upgrades: "+revenue.optInt("todayUpgrades")+"\nToday gross: "+money(revenue.optDouble("todayGrossSar"))+" SAR\nThis month upgrades: "+revenue.optInt("monthUpgrades")+"\nThis month gross: "+money(revenue.optDouble("monthGrossSar"))+" SAR\nTotal verified upgrades: "+revenue.optInt("totalVerifiedUpgrades")+"\nTotal gross plan value: "+money(revenue.optDouble("totalGrossSar"))+" SAR");
            card("Payout / Money","Available USD tracked: $"+money(payout.optDouble("availableUsd"))+"\nPending USD tracked: $"+money(payout.optDouble("pendingUsd"))+"\nRecorded paid USD: $"+money(payout.optDouble("settledUsd"))+"\n\nActual Google Play bank payout is controlled by Google. Use Owner Wallet to track Al Rajhi payout records.");
            card("Safety","Open safety reports: "+safety.optInt("openReports")+"\nPrivate chat text is NOT shown here.");

            JSONArray recent=d.optJSONArray("recentUsers");StringBuilder r=new StringBuilder();
            if(recent!=null)for(int i=0;i<Math.min(20,recent.length());i++){
                JSONObject u=recent.optJSONObject(i);if(u==null)continue;
                r.append(u.optBoolean("online")?"🟢 ":"⚪ ").append(u.optString("displayName","Member"));
                if(u.optBoolean("photoVerified"))r.append(" ✅");
                String plan=u.optString("planKey","");if(!plan.isEmpty())r.append(" • ").append(plan.replace("premium_",""));
                r.append("\nJoined: ").append(u.optString("joinedAt",""));
                if(!u.optString("lastSeenAt","").isEmpty())r.append("\nLast seen: ").append(u.optString("lastSeenAt"));
                r.append("\n\n");
            }
            card("Recent / Online Members",r.length()==0?"No recent member activity yet.":r.toString());
        }catch(Exception e){status.setText("Owner analytics response could not be displayed.");}
    }
}
