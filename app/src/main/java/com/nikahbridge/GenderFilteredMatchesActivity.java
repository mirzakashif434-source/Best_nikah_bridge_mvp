package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenderFilteredMatchesActivity extends Activity {
    private LinearLayout root;
    private boolean premiumActive=false;
    private final java.util.ArrayDeque<LinearLayout> hiddenCards=new java.util.ArrayDeque<>();
    private final Handler presenceHandler=new Handler(Looper.getMainLooper());
    private final Runnable presencePulse=new Runnable(){public void run(){presenceHeartbeat();presenceHandler.postDelayed(this,60000);}};
    private int dp(int v){return Premium2030Ui.dp(this,v);}
    @Override protected void onCreate(Bundle state){super.onCreate(state);AzureAuthManager.bindActivity(this);show();}
    @Override protected void onResume(){super.onResume();presenceHandler.removeCallbacks(presencePulse);presenceHandler.post(presencePulse);}
    @Override protected void onPause(){presenceHandler.removeCallbacks(presencePulse);super.onPause();}
    private void presenceHeartbeat(){
        if(!AzureAuthManager.hasAccount(this))return;
        AzureApiClient.post("/presence/heartbeat","{}",new AzureApiClient.Callback(){
            public void ok(int code,String body){}
            public void err(String message){}
        });
    }
    private void show(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Premium2030Ui.CREAM);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(30));sc.addView(root);setContentView(sc);
        root.addView(Premium2030Ui.title(this,"Discover"));
        root.addView(Premium2030Ui.subtitle(this,"Serious people • Real intentions • Azure profiles"));
        root.addView(Premium2030Ui.heroLine(this,"Meaningful matches for a brighter halal future"));
        load();
        Button back=Premium2030Ui.secondary(this,"Back");Premium2030Ui.addButton(root,back);back.setOnClickListener(v->finish());
    }
    private void load(){
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject response=new JSONObject(body);
                    premiumActive=response.optBoolean("premium",false);
                    JSONArray a=response.optJSONArray("matches");
                    if(a==null||a.length()==0){root.addView(Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,"No eligible real profiles are available yet."));return;}
                    for(int i=0;i<a.length();i++){
                        JSONObject m=a.optJSONObject(i);if(m==null)continue;
                        final String receiverId=m.optString("userId","");
                        final String displayName=m.optString("displayName","Member");
                        final String conversationId=m.optString("conversationId","");
                        LinearLayout card=Premium2030Ui.card(GenderFilteredMatchesActivity.this);
                        FrameLayout photoFrame=new FrameLayout(GenderFilteredMatchesActivity.this);
                        PrivacyPhotoView privateView=new PrivacyPhotoView(GenderFilteredMatchesActivity.this);
                        ImageView photo=new ImageView(GenderFilteredMatchesActivity.this);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        photoFrame.addView(privateView,new FrameLayout.LayoutParams(-1,dp(230)));
                        photoFrame.addView(photo,new FrameLayout.LayoutParams(-1,dp(230)));
                        card.addView(photoFrame,new LinearLayout.LayoutParams(-1,dp(230)));
                        String photoId=m.optString("photoId","");
                        boolean blurred=m.optBoolean("photoBlurred",photoId.isEmpty());
                        if(!blurred&&!photoId.isEmpty()&&!receiverId.isEmpty()){
                            String path="/matches/"+Uri.encode(receiverId)+"/photos/"+Uri.encode(photoId)+"/content";
                            ProfilePhotoLoader.loadAzure(path,photo,()->photo.setVisibility(View.INVISIBLE));
                        }else photo.setVisibility(View.INVISIBLE);
                        boolean online=m.optBoolean("isOnline",false);
                        card.addView(Premium2030Ui.chip(GenderFilteredMatchesActivity.this,online?"🟢 ONLINE":"REAL MATCH"));
                        TextView memberHeading=Premium2030Ui.section(GenderFilteredMatchesActivity.this,displayName+" • "+m.optInt("age",0));
                        LanguageManager.protectUserContent(memberHeading);
                        card.addView(memberHeading);
                        StringBuilder details=new StringBuilder();
                        if(!m.optString("country","").isEmpty())details.append(m.optString("country")).append("\n");
                        details.append("Compatibility ").append(m.optInt("compatibilityScore",0)).append("/100");
                        if(!m.optString("marriageTimeline","").isEmpty())details.append("\nTimeline: ").append(m.optString("marriageTimeline"));
                        TextView d=Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,details.toString());
                        d.setGravity(android.view.Gravity.START);d.setPadding(0,0,0,dp(6));card.addView(d);
                        JSONArray reasons=m.optJSONArray("whyWeMatched");
                        if(reasons!=null&&reasons.length()>0){
                            StringBuilder why=new StringBuilder("Why we matched");
                            for(int j=0;j<reasons.length();j++)why.append("\n✓ ").append(reasons.optString(j));
                            TextView w=Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,why.toString());
                            w.setGravity(android.view.Gravity.START);card.addView(w);
                        }
                        LinearLayout actions=new LinearLayout(GenderFilteredMatchesActivity.this);
                        actions.setOrientation(LinearLayout.HORIZONTAL);
                        Button like=Premium2030Ui.secondary(GenderFilteredMatchesActivity.this,"✅");
                        Button pass=Premium2030Ui.secondary(GenderFilteredMatchesActivity.this,"❎");
                        Button rewind=Premium2030Ui.secondary(GenderFilteredMatchesActivity.this,"↩️");
                        Button messageBtn=Premium2030Ui.secondary(GenderFilteredMatchesActivity.this,"💬");
                        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,dp(54),1f);ap.setMargins(dp(2),dp(6),dp(2),0);
                        actions.addView(like,ap);actions.addView(pass,ap);actions.addView(rewind,ap);actions.addView(messageBtn,ap);
                        card.addView(actions,new LinearLayout.LayoutParams(-1,-2));
                        like.setOnClickListener(v->sendLike(receiverId,like,card));
                        pass.setOnClickListener(v->passCard(card));
                        rewind.setOnClickListener(v->rewindLast());
                        messageBtn.setOnClickListener(v->openPremiumMessage(receiverId,conversationId));

                        Button interest=Premium2030Ui.primary(GenderFilteredMatchesActivity.this,"Send Interest");
                        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54));lp.setMargins(0,dp(8),0,0);card.addView(interest,lp);
                        interest.setOnClickListener(v->sendInterest(receiverId,displayName,interest));
                        Button familySuggest=Premium2030Ui.secondary(GenderFilteredMatchesActivity.this,"Suggest to Family Circle");
                        LinearLayout.LayoutParams flp=new LinearLayout.LayoutParams(-1,dp(52));flp.setMargins(0,dp(6),0,0);card.addView(familySuggest,flp);
                        familySuggest.setOnClickListener(v->suggestToFamilyCircle(receiverId,familySuggest));
                        root.addView(card);
                    }
                }catch(Exception e){root.addView(Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,"Azure matches could not be read."));}
            });}
            public void err(String message){runOnUiThread(()->root.addView(Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,"Could not load real Azure matches: "+message)));}
        });
    }
    private void sendLike(String receiverId,Button button,LinearLayout card){
        if(receiverId==null||receiverId.trim().isEmpty()){LanguageManager.toast(this,"This profile cannot receive a like yet.",Toast.LENGTH_LONG).show();return;}
        try{
            JSONObject b=new JSONObject().put("toUid",receiverId);
            button.setEnabled(false);button.setText("Liking…");
            AzureApiClient.post("/likes",b.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->{
                    try{
                        JSONObject o=new JSONObject(body);
                        button.setText("♥ Liked");
                        int remaining=o.optInt("remaining",0);
                        LanguageManager.toast(GenderFilteredMatchesActivity.this,"Liked • "+remaining+" likes remaining in 24 hours",Toast.LENGTH_SHORT).show();
                        hideCard(card);
                    }catch(Exception e){button.setText("♥ Liked");}
                });}
                public void err(String m){runOnUiThread(()->{
                    button.setEnabled(true);button.setText("♡ Like");
                    if(m!=null&&m.contains("ROLLING_24H_LIKE_LIMIT_REACHED")){
                        LanguageManager.dialog(GenderFilteredMatchesActivity.this)
                            .setTitle("24-hour like limit reached")
                            .setMessage("You can send up to 20 likes in any rolling 24-hour period. A like becomes available again after the oldest one passes 24 hours.")
                            .setPositiveButton("OK",null).show();
                    }else LanguageManager.toast(GenderFilteredMatchesActivity.this,"Like could not be sent.",Toast.LENGTH_LONG).show();
                });}
            });
        }catch(Exception e){button.setEnabled(true);button.setText("♡ Like");}
    }

    private void hideCard(LinearLayout card){
        if(card==null||card.getVisibility()!=View.VISIBLE)return;
        hiddenCards.push(card);card.setVisibility(View.GONE);
    }

    private void passCard(LinearLayout card){
        hideCard(card);
        LanguageManager.toast(this,"Passed. You can use ↩️ Back if your plan includes rewind.",Toast.LENGTH_SHORT).show();
    }

    private void rewindLast(){
        if(!premiumActive){
            LanguageManager.dialog(this)
                .setTitle("Upgrade to use Back")
                .setMessage("↩️ Back / Rewind is available with an upgraded plan so you can return to the last profile you liked or passed.")
                .setPositiveButton("Upgrade",(d,w)->startActivity(new android.content.Intent(this,PremiumPlansActivity.class)))
                .setNegativeButton("Not now",null).show();
            return;
        }
        LinearLayout previous=hiddenCards.poll();
        if(previous==null){LanguageManager.toast(this,"No previous profile to restore.",Toast.LENGTH_SHORT).show();return;}
        previous.setVisibility(View.VISIBLE);
        LanguageManager.toast(this,"Previous profile restored.",Toast.LENGTH_SHORT).show();
    }

    private void openPremiumMessage(String receiverId,String knownConversationId){
        if(!premiumActive){
            LanguageManager.dialog(this)
                .setTitle("Upgrade to Message")
                .setMessage("💬 Messaging from the Live Matches screen is available after upgrade.")
                .setPositiveButton("Upgrade",(d,w)->startActivity(new android.content.Intent(this,PremiumPlansActivity.class)))
                .setNegativeButton("Not now",null).show();
            return;
        }
        AzureApiClient.get("/matches/"+Uri.encode(receiverId)+"/message-access",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    String cid=new JSONObject(body).optString("conversationId",knownConversationId);
                    if(cid==null||cid.isEmpty()){LanguageManager.toast(GenderFilteredMatchesActivity.this,"Safe mutual chat is not ready yet.",Toast.LENGTH_LONG).show();return;}
                    android.content.Intent i=new android.content.Intent(GenderFilteredMatchesActivity.this,SafeCommunicationActivity.class);
                    i.putExtra("conversationId",cid);
                    startActivity(i);
                }catch(Exception e){LanguageManager.toast(GenderFilteredMatchesActivity.this,"Could not open safe chat.",Toast.LENGTH_LONG).show();}
            });}
            public void err(String m){runOnUiThread(()->{
                if(m!=null&&m.contains("PREMIUM_REQUIRED_FOR_MATCH_MESSAGE")){
                    startActivity(new android.content.Intent(GenderFilteredMatchesActivity.this,PremiumPlansActivity.class));
                }else if(m!=null&&m.contains("MUTUAL_CHAT_REQUIRED")){
                    LanguageManager.dialog(GenderFilteredMatchesActivity.this)
                        .setTitle("Mutual connection required")
                        .setMessage("For safety, messaging starts only after a real mutual connection. Your upgrade remains active; send/accept interest first, then chat.")
                        .setPositiveButton("OK",null).show();
                }else LanguageManager.toast(GenderFilteredMatchesActivity.this,"Message access unavailable.",Toast.LENGTH_LONG).show();
            });}
        });
    }

    private void suggestToFamilyCircle(String receiverId,Button button){
        if(receiverId==null||receiverId.trim().isEmpty()){LanguageManager.toast(this,"This profile cannot be suggested yet.",Toast.LENGTH_LONG).show();return;}
        try{
            JSONObject body=new JSONObject().put("suggestedUserId",receiverId).put("note","");
            button.setEnabled(false);button.setText("Suggesting…");
            AzureApiClient.post("/family-circle/suggestions",body.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String response){runOnUiThread(()->{button.setText("Suggested to Family Circle");LanguageManager.toast(GenderFilteredMatchesActivity.this,"Match suggestion sent to Family Circle.",Toast.LENGTH_SHORT).show();});}
                public void err(String message){runOnUiThread(()->{button.setEnabled(true);button.setText("Suggest to Family Circle");LanguageManager.toast(GenderFilteredMatchesActivity.this,"Family Circle suggestion could not be sent.",Toast.LENGTH_LONG).show();});}
            });
        }catch(Exception e){button.setEnabled(true);button.setText("Suggest to Family Circle");}
    }

    private void sendInterest(String receiverId,String name,Button button){
        if(receiverId==null||receiverId.trim().isEmpty()){LanguageManager.toast(this,"This match cannot receive an interest yet.",Toast.LENGTH_LONG).show();return;}
        try{
            JSONObject body=new JSONObject().put("receiverUserId",receiverId);
            button.setEnabled(false);button.setText("Sending…");
            AzureApiClient.post("/interests",body.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String response){runOnUiThread(()->{button.setText("Interest Sent");LanguageManager.toast(GenderFilteredMatchesActivity.this,"Interest Sent",Toast.LENGTH_SHORT).show();});}
                public void err(String message){runOnUiThread(()->{
                    button.setEnabled(true);button.setText("Send Interest");
                    if(message!=null&&message.contains("FREE_DAILY_INTEREST_LIMIT")){
                        LanguageManager.dialog(GenderFilteredMatchesActivity.this)
                            .setTitle("Daily free interest limit reached")
                            .setMessage("Free members can send up to 3 new interests per day. Serious Nikah Plus unlocks unlimited interests.")
                            .setPositiveButton("View Serious Nikah Plus",(d,w)->startActivity(new android.content.Intent(GenderFilteredMatchesActivity.this,SeriousNikahPlusActivity.class)))
                            .setNegativeButton("Not now",null).show();
                    }else LanguageManager.toast(GenderFilteredMatchesActivity.this,"Interest could not be sent: "+message,Toast.LENGTH_LONG).show();
                });}
            });
        }catch(Exception e){button.setEnabled(true);button.setText("Send Interest");}
    }
}
