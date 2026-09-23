package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

/** Real opt-in Azure Nikah Success Network. No fabricated stories, mentors, counts or outcomes. */
public class NikahSuccessNetworkActivity extends Activity {
    private LinearLayout root,mentorList;private TextView status;private EditText story;private CheckBox completed,shareStory,mentorOptIn;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(8),dp(4),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private EditText area(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setGravity(Gravity.TOP|Gravity.START);e.setMinHeight(dp(120));e.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(14));g.setStroke(dp(1),Color.rgb(205,215,211));e.setBackground(g);return e;}

    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(light);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));sc.addView(root);setContentView(sc);
        root.addView(txt("❤️ Nikah Success Network",28,true));root.addView(txt("A voluntary post-Nikah network backed by real Azure account records. Participation is optional.",15,false));
        status=txt("Checking your real Azure account…",17,true);root.addView(status);
        completed=new CheckBox(this);completed.setText("My Nikah has genuinely been completed");root.addView(completed);
        shareStory=new CheckBox(this);shareStory.setText("Save my anonymized story for future moderated sharing");root.addView(shareStory);
        story=area("Optional story. Do not include names, phone numbers, addresses or financial/private details.");root.addView(story,new LinearLayout.LayoutParams(-1,dp(130)));story.setVisibility(View.GONE);
        shareStory.setOnCheckedChangeListener((b,c)->story.setVisibility(c?View.VISIBLE:View.GONE));
        mentorOptIn=new CheckBox(this);mentorOptIn.setText("I am willing to offer voluntary peer mentorship");root.addView(mentorOptIn);
        Button save=btn("Save My Real Network Choice",true);root.addView(save,new LinearLayout.LayoutParams(-1,dp(62)));save.setOnClickListener(v->save());
        root.addView(txt("Story sharing remains private/pending until a real moderation/publication flow approves it. Peer mentorship is not professional, legal or religious advice.",13,false));
        Button mentors=btn("Find Available Peer Mentors",false);root.addView(mentors,new LinearLayout.LayoutParams(-1,dp(62)));mentors.setOnClickListener(v->findMentors());
        mentorList=new LinearLayout(this);mentorList.setOrientation(LinearLayout.VERTICAL);root.addView(mentorList);
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }

    private void load(){
        AzureApiClient.get("/settings/nikah_success_network",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject v=new JSONObject(body).optJSONObject("value");if(v==null){status.setText("No Nikah outcome is recorded yet.");return;}
                    completed.setChecked(v.optBoolean("nikahCompleted",false));mentorOptIn.setChecked(v.optBoolean("mentorOptIn",false));shareStory.setChecked(v.optBoolean("shareStory",false));story.setText(v.optString("successStory",""));
                    status.setText(completed.isChecked()?"Your Azure account records Nikah completed.":"Your Nikah outcome is not marked completed.");
                }catch(Exception e){status.setText("Azure network status could not be read.");}
            });}
            public void err(String m){runOnUiThread(()->status.setText("Could not load Azure network status."));}
        });
    }

    private void save(){
        if(shareStory.isChecked()&&!completed.isChecked()){toast("A success story can only be saved after you confirm a genuine completed Nikah.");return;}
        String text=story.getText().toString().trim();if(shareStory.isChecked()&&text.length()<20){toast("Please write a meaningful short story or turn story sharing off.");return;}
        try{
            JSONObject v=new JSONObject().put("nikahCompleted",completed.isChecked()).put("mentorOptIn",mentorOptIn.isChecked()).put("shareStory",shareStory.isChecked()).put("successStory",shareStory.isChecked()?text:"").put("storyStatus",shareStory.isChecked()?"pending_review":"private");
            AzureApiClient.put("/settings/nikah_success_network",v.toString(),new AzureApiClient.Callback(){
                public void ok(int c,String b){runOnUiThread(()->{status.setText(completed.isChecked()?"Nikah completed is recorded in Azure.":"Your network choice is recorded in Azure.");toast("Your real network choice was saved securely.");});}
                public void err(String m){runOnUiThread(()->toast("Could not save Azure network choice: "+m));}
            });
        }catch(Exception e){toast("Network choice is invalid.");}
    }

    private void findMentors(){
        status.setText("Checking real Azure peer mentors…");mentorList.removeAllViews();
        AzureApiClient.get("/success-network/mentors",new AzureApiClient.Callback(){
            public void ok(int c,String b){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(b).optJSONArray("mentors");if(a==null||a.length()==0){status.setText("No opted-in real peer mentors are currently available.");return;}
                    status.setText("Available real Azure peer mentors");
                    for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);String id=m.optString("userId",""),name=m.optString("displayName","Peer mentor");Button req=btn("Request support from "+name,false);req.setOnClickListener(v->requestMentor(id));mentorList.addView(req,new LinearLayout.LayoutParams(-1,dp(58)));}
                }catch(Exception e){status.setText("Azure mentor list could not be read.");}
            });}
            public void err(String m){runOnUiThread(()->status.setText("Peer mentor service is unavailable."));}
        });
    }
    private void requestMentor(String id){
        try{
            JSONObject b=new JSONObject().put("mentorUserId",id);
            AzureApiClient.post("/success-network/mentor-requests",b.toString(),new AzureApiClient.Callback(){public void ok(int c,String x){runOnUiThread(()->toast("Real Azure mentorship request submitted."));}public void err(String m){runOnUiThread(()->toast("Mentorship request failed: "+m));}});
        }catch(Exception e){toast("Invalid mentor request.");}
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
