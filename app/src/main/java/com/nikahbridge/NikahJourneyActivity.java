package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real Azure journey tracker derived only from production API records. */
public class NikahJourneyActivity extends Activity {
    private LinearLayout root,stages;private TextView summary;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);
    private boolean profileReady,blueprintReady,verified,hasMatches,hasConversation,hasFamily;

    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}

    private void render(){
        ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);sc.addView(root);setContentView(sc);
        root.addView(txt("My Nikah Journey",27,true));root.addView(txt("Progress is based only on real Azure profile, verification, matching, conversation and family records.",15,false));
        summary=txt("Checking your real Azure progress…",18,true);root.addView(summary);
        stages=new LinearLayout(this);stages.setOrientation(LinearLayout.VERTICAL);root.addView(stages);
        Button refresh=btn("Refresh My Journey",true);root.addView(refresh,new LinearLayout.LayoutParams(-1,dp(62)));refresh.setOnClickListener(v->load());
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }

    private void load(){
        profileReady=blueprintReady=verified=hasMatches=hasConversation=hasFamily=false;stages.removeAllViews();summary.setText("Checking your real Azure progress…");
        AzureApiClient.get("/profile",new AzureApiClient.Callback(){
            public void ok(int c,String b){try{JSONObject p=new JSONObject(b).optJSONObject("profile");profileReady=p!=null&&p.optBoolean("profile_completed",false);}catch(Exception ignored){}loadLiving();}
            public void err(String m){loadLiving();}
        });
    }
    private void loadLiving(){
        AzureApiClient.get("/compatibility/living",new AzureApiClient.Callback(){
            public void ok(int c,String b){try{JSONObject v=new JSONObject(b).optJSONObject("living");blueprintReady=v!=null&&!v.optString("marriage_timeline","").isEmpty()&&!v.optString("family_involvement","").isEmpty()&&!v.optString("children_expectation","").isEmpty();}catch(Exception ignored){}loadVerification();}
            public void err(String m){loadVerification();}
        });
    }
    private void loadVerification(){
        AzureApiClient.get("/verification",new AzureApiClient.Callback(){
            public void ok(int c,String b){try{JSONArray a=new JSONObject(b).optJSONArray("verifications");if(a!=null)for(int i=0;i<a.length();i++)if("approved".equalsIgnoreCase(a.optJSONObject(i).optString("status")))verified=true;}catch(Exception ignored){}loadMatches();}
            public void err(String m){loadMatches();}
        });
    }
    private void loadMatches(){
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int c,String b){try{hasMatches=new JSONObject(b).optInt("count",0)>0;}catch(Exception ignored){}loadConversations();}
            public void err(String m){loadConversations();}
        });
    }
    private void loadConversations(){
        AzureApiClient.get("/conversations",new AzureApiClient.Callback(){
            public void ok(int c,String b){try{JSONArray a=new JSONObject(b).optJSONArray("conversations");hasConversation=a!=null&&a.length()>0;}catch(Exception ignored){}loadFamily();}
            public void err(String m){loadFamily();}
        });
    }
    private void loadFamily(){
        AzureApiClient.get("/family-links",new AzureApiClient.Callback(){
            public void ok(int c,String b){try{JSONArray a=new JSONObject(b).optJSONArray("familyLinks");hasFamily=a!=null&&a.length()>0;}catch(Exception ignored){}show();}
            public void err(String m){show();}
        });
    }

    private void show(){
        runOnUiThread(()->{
            stages.removeAllViews();int done=0;
            done+=add("1. Prepare your profile",profileReady,"Your Azure profile is completed.");
            done+=add("2. Define marriage expectations",blueprintReady,"Timeline, family involvement and children expectations are recorded.");
            done+=add("3. Build trust",verified,"An approved Azure identity verification is recorded.");
            done+=add("4. Review real matches",hasMatches,"At least one real Azure compatibility match is available.");
            done+=add("5. Have a mutual conversation",hasConversation,"A real mutual Azure conversation exists.");
            done+=add("6. Involve family / Wali",hasFamily,"A real family/Wali link exists in Azure.");
            add("7. Prepare for Nikah",false,"Never auto-completed. This requires real-life mutual agreement and appropriate family/legal/religious steps.");
            summary.setText("Journey progress: "+done+" / 7 stages recorded as complete\n\nThis is a progress aid, not a prediction or guarantee of marriage.");
        });
    }
    private int add(String title,boolean complete,String detail){stages.addView(txt((complete?"✓ ":"○ ")+title+"\n"+detail,16,complete));return complete?1:0;}
}
