package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class GenderFilteredMatchesActivity extends Activity {
    private LinearLayout root;
    private int dp(int v){return Premium2030Ui.dp(this,v);}
    @Override protected void onCreate(Bundle state){super.onCreate(state);AzureAuthManager.bindActivity(this);show();}
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
                    JSONArray a=new JSONObject(body).optJSONArray("matches");
                    if(a==null||a.length()==0){root.addView(Premium2030Ui.subtitle(GenderFilteredMatchesActivity.this,"No eligible real profiles are available yet."));return;}
                    for(int i=0;i<a.length();i++){
                        JSONObject m=a.optJSONObject(i);if(m==null)continue;
                        final String receiverId=m.optString("userId","");
                        final String displayName=m.optString("displayName","Member");
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
                        card.addView(Premium2030Ui.chip(GenderFilteredMatchesActivity.this,"REAL MATCH"));
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
                public void err(String message){runOnUiThread(()->{button.setEnabled(true);button.setText("Send Interest");LanguageManager.toast(GenderFilteredMatchesActivity.this,"Interest could not be sent: "+message,Toast.LENGTH_LONG).show();});}
            });
        }catch(Exception e){button.setEnabled(true);button.setText("Send Interest");}
    }
}
