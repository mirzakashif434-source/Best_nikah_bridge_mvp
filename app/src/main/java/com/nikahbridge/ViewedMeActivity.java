package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Real Azure "Viewed Me" screen.
 * Shows only explicit profile opens recorded in Azure PostgreSQL.
 */
public class ViewedMeActivity extends Activity {
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

        TextView title=Premium2030Ui.title(this,"Viewed Me");
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        root.addView(Premium2030Ui.subtitle(this,
                "Real members who explicitly opened your profile • Azure recorded • No demo views"));
        root.addView(Premium2030Ui.heroLine(this,"See genuine profile interest with privacy respected"));

        status=Premium2030Ui.subtitle(this,"Loading real Azure profile views…");
        status.setGravity(Gravity.START);
        root.addView(status);

        list=new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list,new LinearLayout.LayoutParams(-1,-2));

        refresh=Premium2030Ui.primary(this,"Refresh Viewed Me");
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
        status.setText("Loading real Azure profile views…");
        list.removeAllViews();

        AzureApiClient.get("/profile-views/received",new AzureApiClient.Callback(){
            @Override public void ok(int code,String body){
                runOnUiThread(()->{
                    refresh.setEnabled(true);
                    refresh.setText("Refresh Viewed Me");
                    try{
                        JSONObject response=new JSONObject(body);
                        JSONArray viewers=response.optJSONArray("viewers");
                        int count=response.optInt("count",viewers==null?0:viewers.length());
                        status.setText(count==1?"1 real member viewed your profile":count+" real members viewed your profile");
                        if(viewers==null||viewers.length()==0){
                            showEmpty();
                            return;
                        }
                        for(int i=0;i<viewers.length();i++){
                            JSONObject item=viewers.optJSONObject(i);
                            if(item!=null)addViewerCard(item);
                        }
                    }catch(Exception e){
                        status.setText("Viewed Me data could not be displayed.");
                    }
                });
            }

            @Override public void err(String message){
                runOnUiThread(()->{
                    refresh.setEnabled(true);
                    refresh.setText("Refresh Viewed Me");
                    String m=message==null?"":message;
                    if(m.contains("AZURE_SIGN_IN_REQUIRED")||m.contains("AZURE_INTERACTION_REQUIRED")||m.contains("401")){
                        showSignInRecovery();
                    }else{
                        status.setText("Viewed Me is temporarily unavailable. Check your connection and tap Refresh.");
                    }
                });
            }
        });
    }

    private void showSignInRecovery(){
        list.removeAllViews();
        status.setText("Azure sign in is required to load your real profile views.");
        Button signIn=Premium2030Ui.primary(this,"Sign in with Azure");
        list.addView(signIn,new LinearLayout.LayoutParams(-1,dp(58)));
        signIn.setOnClickListener(v->{
            signIn.setEnabled(false);
            signIn.setText("Opening Azure Sign In…");
            AzureAuthManager.acquireTokenInteractive(this,new AzureAuthManager.Callback(){
                @Override public void ok(String accessToken){runOnUiThread(ViewedMeActivity.this::load);}
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
        card.addView(Premium2030Ui.section(this,"No profile views yet"));
        card.addView(Premium2030Ui.subtitle(this,
                "When a real member explicitly opens your profile, they will appear here."));
        list.addView(card);
    }

    private void addViewerCard(JSONObject item){
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
        final boolean iLiked=item.optBoolean("i_liked",false);
        final boolean likedMe=item.optBoolean("liked_me",false);
        final int viewCount=Math.max(1,item.optInt("view_count",1));

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
        details.append(details.length()>0?"\n":"").append("Profile opens: ").append(viewCount);
        if(iLiked&&likedMe)details.append("\n♥ Mutual like");
        else if(likedMe)details.append("\n♥ This member liked you");
        else if(iLiked)details.append("\nYou liked this member");

        TextView detailText=Premium2030Ui.subtitle(this,details.toString());
        detailText.setGravity(Gravity.START);
        card.addView(detailText);

        Button view=Premium2030Ui.secondary(this,"View Profile");
        card.addView(view,new LinearLayout.LayoutParams(-1,dp(52)));
        view.setOnClickListener(v->{
            ProfileViewTracker.record(ViewedMeActivity.this,userId);
            showProfile(name,age,country,city,intention,education,family,identityVerified,photoVerified);
        });

        Button like=Premium2030Ui.primary(this,iLiked?"♥ Liked":"Like");
        card.addView(like,new LinearLayout.LayoutParams(-1,dp(54)));
        if(iLiked)like.setEnabled(false);
        else like.setOnClickListener(v->sendLike(userId,like));

        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(6),0,dp(6));
        list.addView(card,lp);
    }

    private void showProfile(String name,int age,String country,String city,String intention,
                             String education,String family,boolean identityVerified,boolean photoVerified){
        StringBuilder body=new StringBuilder();
        if(age>0)body.append("Age: ").append(age).append("\n");
        if(!country.isEmpty())body.append("Country: ").append(country).append("\n");
        if(!city.isEmpty())body.append("City: ").append(city).append("\n");
        if(!intention.isEmpty())body.append("Nikah intention: ").append(intention).append("\n");
        if(!education.isEmpty())body.append("Education: ").append(education).append("\n");
        if(!family.isEmpty())body.append("Family involvement: ").append(family).append("\n");
        body.append("\nIdentity verified: ").append(identityVerified?"Yes":"Not yet");
        body.append("\nPhoto verified: ").append(photoVerified?"Yes":"Not yet");
        body.append("\n\nOnly privacy-safe Azure profile fields are shown.");

        LanguageManager.dialog(this)
                .setTitle(name)
                .setMessage(body.toString())
                .setPositiveButton("Close",null)
                .show();
    }

    private void sendLike(String userId,Button button){
        if(userId==null||userId.trim().isEmpty()){
            LanguageManager.toast(this,"This profile cannot receive a like right now.",Toast.LENGTH_LONG).show();
            return;
        }
        try{
            button.setEnabled(false);
            button.setText("Liking…");
            JSONObject body=new JSONObject().put("toUid",userId);
            AzureApiClient.post("/likes",body.toString(),new AzureApiClient.Callback(){
                @Override public void ok(int code,String response){
                    runOnUiThread(()->{
                        button.setText("♥ Liked");
                        button.setEnabled(false);
                        LanguageManager.toast(ViewedMeActivity.this,"Like sent securely in Azure.",Toast.LENGTH_LONG).show();
                    });
                }
                @Override public void err(String message){
                    runOnUiThread(()->{
                        button.setEnabled(true);
                        button.setText("Like");
                        if(message!=null&&message.contains("ROLLING_24H_LIKE_LIMIT_REACHED")){
                            LanguageManager.dialog(ViewedMeActivity.this)
                                    .setTitle("24-hour like limit reached")
                                    .setMessage("You can send up to 20 likes in any rolling 24-hour period.")
                                    .setPositiveButton("OK",null)
                                    .show();
                        }else{
                            LanguageManager.toast(ViewedMeActivity.this,"Like could not be sent. Please try again.",Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }catch(Exception e){
            button.setEnabled(true);
            button.setText("Like");
        }
    }
}
