package com.nikahbridge;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Real Azure "I Liked" screen.
 * Shows only authenticated user's active likes stored in Azure PostgreSQL.
 */
public class ILikedActivity extends Activity {
    private LinearLayout root,list;
    private TextView status;
    private Button refresh;
    private int dp(int v){return Premium2030Ui.dp(this,v);}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        build();
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(Premium2030Ui.CREAM);

        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(30));
        root.setBackgroundColor(Premium2030Ui.CREAM);
        scroll.addView(root);
        setContentView(scroll);

        ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(18),dp(20),dp(18),dp(30)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(scroll);

        TextView title=Premium2030Ui.title(this,"I Liked");
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        root.addView(Premium2030Ui.subtitle(this,
                "Profiles you genuinely liked • Real Azure history • No demo data"));
        root.addView(Premium2030Ui.heroLine(this,"Keep your intentions clear and respectful"));

        status=Premium2030Ui.subtitle(this,"Loading your real Azure likes…");
        status.setGravity(Gravity.START);
        root.addView(status);

        list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list,new LinearLayout.LayoutParams(-1,-2));

        refresh=Premium2030Ui.primary(this,"Refresh I Liked");
        Premium2030Ui.addButton(root,refresh);
        refresh.setOnClickListener(v->load());

        Button back=Premium2030Ui.secondary(this,"Back");
        Premium2030Ui.addButton(root,back);
        back.setOnClickListener(v->finish());

        load();
    }

    private void load(){
        refresh.setEnabled(false);
        refresh.setText("Refreshing…");
        status.setText("Loading your real Azure likes…");
        list.removeAllViews();

        AzureApiClient.get("/likes/sent",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                runOnUiThread(()->{
                    refresh.setEnabled(true);
                    refresh.setText("Refresh I Liked");
                    try{
                        JSONObject response=new JSONObject(body);
                        JSONArray likes=response.optJSONArray("likes");
                        int count=response.optInt("count",likes==null?0:likes.length());
                        status.setText(count==1?"You currently like 1 real profile":"You currently like "+count+" real profiles");
                        if(likes==null||likes.length()==0){
                            showEmpty();
                            return;
                        }
                        for(int i=0;i<likes.length();i++){
                            JSONObject item=likes.optJSONObject(i);
                            if(item!=null)addLikeCard(item);
                        }
                    }catch(Exception e){
                        status.setText("I Liked data could not be displayed.");
                    }
                });
            }

            @Override public void err(String message){
                runOnUiThread(()->{
                    refresh.setEnabled(true);
                    refresh.setText("Refresh I Liked");
                    String m=message==null?"":message;
                    if(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("AZURE_INTERACTION_REQUIRED")||m.contains("401")){
                        showSignInRecovery();
                    }else{
                        status.setText("I Liked is temporarily unavailable. Check your connection and tap Refresh.");
                    }
                });
            }
        });
    }

    private void showSignInRecovery(){
        list.removeAllViews();
        status.setText("Azure sign in is required to load your real sent likes.");
        Button signIn=Premium2030Ui.primary(this,"Sign in with Azure");
        list.addView(signIn,new LinearLayout.LayoutParams(-1,dp(58)));
        signIn.setOnClickListener(v->{
            signIn.setEnabled(false);
            signIn.setText("Opening Azure Sign In…");
            AzureAuthManager.acquireTokenInteractive(this,new AzureAuthManager.Callback(){
                @Override public void ok(String accessToken){runOnUiThread(ILikedActivity.this::load);}
                @Override public void err(String message){runOnUiThread(()->{
                    signIn.setEnabled(true);
                    signIn.setText("Sign in with Azure");
                    status.setText("Azure sign in was not completed. Please try again.");
                });}
            });
        });
    }

    private void showEmpty(){
        LinearLayout card=Premium2030Ui.card(this);
        card.addView(Premium2030Ui.section(this,"No active likes yet"));
        card.addView(Premium2030Ui.subtitle(this,
                "Profiles you like from Discover will appear here automatically."));
        list.addView(card);
    }

    private void addLikeCard(JSONObject item){
        final String userId=item.optString("user_id","");
        final String name=item.optString("display_name","Member");
        final int age=item.optInt("age",0);
        final String country=item.optString("country","");
        final String city=item.optString("city","");
        final String intention=item.optString("marriage_intention","");
        final String education=item.optString("education","");
        final String family=item.optString("family_involvement","");
        final boolean online=item.optBoolean("is_online",false);
        final boolean photoVerified=item.optBoolean("photo_verified",false);
        final boolean identityVerified=item.optBoolean("identity_verified",false);
        final boolean mutual=item.optBoolean("mutual_like",false);

        LinearLayout card=Premium2030Ui.card(this);

        String badge=identityVerified?"✓ ID VERIFIED":photoVerified?"✓ PHOTO VERIFIED":"REAL AZURE MEMBER";
        if(online)badge+=" • 🟢 ONLINE";
        card.addView(Premium2030Ui.chip(this,badge));

        TextView heading=Premium2030Ui.section(this,name+(age>0?" • "+age:""));
        LanguageManager.protectUserContent(heading);
        card.addView(heading);

        StringBuilder details=new StringBuilder();
        if(!country.isEmpty())details.append(country);
        if(!city.isEmpty())details.append(details.length()>0?" • ":"").append(city);
        if(!intention.isEmpty())details.append(details.length()>0?"\n":"").append("Nikah intention: ").append(intention);
        details.append(details.length()>0?"\n":"").append(mutual?"♥ Mutual like":"Like sent");
        TextView detailText=Premium2030Ui.subtitle(this,details.toString());
        detailText.setGravity(Gravity.START);
        card.addView(detailText);

        Button view=Premium2030Ui.secondary(this,"View Profile");
        card.addView(view,new LinearLayout.LayoutParams(-1,dp(52)));
        view.setOnClickListener(v->showProfile(name,age,country,city,intention,education,family,identityVerified,photoVerified,mutual));

        Button cancel=Premium2030Ui.secondary(this,"Cancel Like");
        card.addView(cancel,new LinearLayout.LayoutParams(-1,dp(52)));
        cancel.setOnClickListener(v->confirmCancel(userId,name,card,cancel));

        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(6),0,dp(6));
        list.addView(card,lp);
    }

    private void showProfile(String name,int age,String country,String city,String intention,
                             String education,String family,boolean identityVerified,
                             boolean photoVerified,boolean mutual){
        StringBuilder body=new StringBuilder();
        if(age>0)body.append("Age: ").append(age).append("\n");
        if(!country.isEmpty())body.append("Country: ").append(country).append("\n");
        if(!city.isEmpty())body.append("City: ").append(city).append("\n");
        if(!intention.isEmpty())body.append("Nikah intention: ").append(intention).append("\n");
        if(!education.isEmpty())body.append("Education: ").append(education).append("\n");
        if(!family.isEmpty())body.append("Family involvement: ").append(family).append("\n");
        body.append("\nIdentity verified: ").append(identityVerified?"Yes":"Not yet");
        body.append("\nPhoto verified: ").append(photoVerified?"Yes":"Not yet");
        body.append("\nLike status: ").append(mutual?"Mutual like":"Sent");
        body.append("\n\nOnly privacy-safe Azure profile fields are shown.");

        LanguageManager.dialog(this)
                .setTitle(name)
                .setMessage(body.toString())
                .setPositiveButton("Close",null)
                .show();
    }

    private void confirmCancel(String userId,String name,LinearLayout card,Button button){
        if(userId==null||userId.trim().isEmpty()){
            LanguageManager.toast(this,"This like cannot be changed right now.",Toast.LENGTH_LONG).show();
            return;
        }
        LanguageManager.dialog(this)
                .setTitle("Cancel Like?")
                .setMessage("Remove your active like for "+name+"? This does not delete either profile or any account data.")
                .setNegativeButton("Keep Like",null)
                .setPositiveButton("Cancel Like",(d,w)->cancelLike(userId,card,button))
                .show();
    }

    private void cancelLike(String userId,LinearLayout card,Button button){
        button.setEnabled(false);
        button.setText("Cancelling…");
        AzureApiClient.delete("/likes/"+Uri.encode(userId),"{}",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                runOnUiThread(()->{
                    list.removeView(card);
                    LanguageManager.toast(ILikedActivity.this,"Like cancelled securely in Azure.",Toast.LENGTH_LONG).show();
                    load();
                });
            }
            @Override public void err(String message){
                runOnUiThread(()->{
                    button.setEnabled(true);
                    button.setText("Cancel Like");
                    if(message!=null&&message.contains("ACTIVE_LIKE_NOT_FOUND")){
                        LanguageManager.toast(ILikedActivity.this,"This like is already inactive.",Toast.LENGTH_LONG).show();
                        load();
                    }else{
                        LanguageManager.toast(ILikedActivity.this,"Like could not be cancelled. Please try again.",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
