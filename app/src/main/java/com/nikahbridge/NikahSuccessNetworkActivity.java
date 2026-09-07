package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Real, opt-in Nikah Success Network.
 * Members can privately record a completed Nikah, optionally share an
 * anonymized success story, and optionally offer/request peer mentorship.
 * No fabricated stories, members, counts, or outcomes are shown.
 */
public class NikahSuccessNetworkActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db;
    private LinearLayout root; private TextView status; private EditText story;
    private CheckBox completed, shareStory, mentorOptIn;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();render();load();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView txt(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(8),dp(4),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String s,boolean fill){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);return b;}
    private EditText area(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setGravity(48|3);e.setMinHeight(dp(120));e.setPadding(dp(14),dp(10),dp(14),dp(10));GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(dp(14));g.setStroke(dp(1),Color.rgb(205,215,211));e.setBackground(g);return e;}

    private void render(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(light);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(24),dp(20),dp(32));sc.addView(root);setContentView(sc);
        root.addView(txt("❤️ Nikah Success Network",28,true));
        root.addView(txt("A voluntary post-Nikah community for members who choose to record their real outcome, share an anonymized experience, or offer peer support. Participation is optional and does not expose your private profile.",15,false));
        status=txt("Checking your real account…",17,true);root.addView(status);
        completed=new CheckBox(this);completed.setText("My Nikah has genuinely been completed");root.addView(completed);
        shareStory=new CheckBox(this);shareStory.setText("Share my story anonymously with the community");root.addView(shareStory);
        story=area("Optional: share a short lesson or experience. Do not include names, phone numbers, addresses, financial details or other private information.");root.addView(story,new LinearLayout.LayoutParams(-1,dp(130)));story.setVisibility(View.GONE);
        shareStory.setOnCheckedChangeListener((b,c)->story.setVisibility(c?View.VISIBLE:View.GONE));
        mentorOptIn=new CheckBox(this);mentorOptIn.setText("I am willing to offer voluntary peer mentorship to serious members");root.addView(mentorOptIn);
        Button save=btn("Save My Real Network Choice",true);root.addView(save,new LinearLayout.LayoutParams(-1,dp(62)));save.setOnClickListener(v->save());
        root.addView(txt("Mentorship is peer support, not professional, legal or religious advice. Never share passwords, payment information or private contact details here.",13,false));
        Button mentors=btn("Find Available Peer Mentors",false);root.addView(mentors,new LinearLayout.LayoutParams(-1,dp(62)));mentors.setOnClickListener(v->findMentors());
        Button back=btn("Back",false);root.addView(back,new LinearLayout.LayoutParams(-1,dp(62)));back.setOnClickListener(v->finish());
    }
    private void load(){
        if(auth.getCurrentUser()==null){status.setText("Sign in is required.");return;}
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->{
            completed.setChecked(Boolean.TRUE.equals(d.getBoolean("nikahCompleted")));
            mentorOptIn.setChecked(Boolean.TRUE.equals(d.getBoolean("nikahMentorOptIn")));
            String existing=d.getString("nikahSuccessStory"); if(existing!=null){story.setText(existing);shareStory.setChecked(!existing.trim().isEmpty());}
            status.setText(Boolean.TRUE.equals(d.getBoolean("nikahCompleted"))?"Your account records Nikah completed. Thank you for helping the community grow honestly.":"Your Nikah outcome is not marked as completed.");
        }).addOnFailureListener(e->status.setText("Could not load your real account status."));
    }
    private void save(){
        if(auth.getCurrentUser()==null){Toast.makeText(this,"Sign in is required.",Toast.LENGTH_LONG).show();return;}
        if(shareStory.isChecked()&&!completed.isChecked()){Toast.makeText(this,"A success story can only be shared after you confirm a genuine completed Nikah.",Toast.LENGTH_LONG).show();return;}
        String text=story.getText().toString().trim();
        if(shareStory.isChecked()&&text.length()<20){Toast.makeText(this,"Please write a meaningful short story or turn story sharing off.",Toast.LENGTH_LONG).show();return;}
        Map<String,Object> data=new HashMap<>();data.put("nikahCompleted",completed.isChecked());data.put("nikahMentorOptIn",mentorOptIn.isChecked());data.put("nikahSuccessStory",shareStory.isChecked()?text:"");data.put("nikahNetworkUpdatedAt",FieldValue.serverTimestamp());
        db.collection("users").document(auth.getUid()).set(data,SetOptions.merge()).addOnSuccessListener(v->{
            if(completed.isChecked()&&shareStory.isChecked()){
                Map<String,Object> publicStory=new HashMap<>();publicStory.put("authorUid",auth.getUid());publicStory.put("story",text);publicStory.put("approved",false);publicStory.put("createdAt",FieldValue.serverTimestamp());
                db.collection("nikahSuccessStories").add(publicStory).addOnSuccessListener(x->Toast.makeText(this,"Your real outcome and story choice were saved. The story remains hidden until platform review.",Toast.LENGTH_LONG).show()).addOnFailureListener(x->Toast.makeText(this,"Your outcome was saved, but the story could not be submitted for review.",Toast.LENGTH_LONG).show());
            } else Toast.makeText(this,"Your real network choice was saved securely.",Toast.LENGTH_LONG).show();
            status.setText(completed.isChecked()?"Nikah completed is recorded for your account.":"Your network choice is recorded.");
        }).addOnFailureListener(e->Toast.makeText(this,"Could not save. Please check your connection and try again.",Toast.LENGTH_LONG).show());
    }
    private void findMentors(){
        if(auth.getCurrentUser()==null){Toast.makeText(this,"Sign in is required.",Toast.LENGTH_LONG).show();return;}
        status.setText("Checking real peer mentors…");
        db.collection("nikahMentors").whereEqualTo("active",true).limit(20).get().addOnSuccessListener(q->{
            if(q.isEmpty()){status.setText("No peer mentors are currently available. No fake profiles are shown.");return;}
            root.addView(txt("Available peer mentors",19,true),root.indexOfChild(status)+1);
            for(DocumentSnapshot d:q.getDocuments()){
                String display=d.getString("displayName");if(display==null||display.trim().isEmpty())display="Verified peer mentor";
                Button request=btn("Request support from "+display,false);root.addView(request,new LinearLayout.LayoutParams(-1,dp(58)));String mentorUid=d.getId();String mentorName=display;request.setOnClickListener(v->requestMentor(mentorUid,mentorName));
            }
        }).addOnFailureListener(e->status.setText("Peer mentor service is unavailable right now. No fake mentors are shown."));
    }
    private void requestMentor(String mentorUid,String mentorName){
        Map<String,Object> r=new HashMap<>();r.put("requesterUid",auth.getUid());r.put("mentorUid",mentorUid);r.put("mentorNameSnapshot",mentorName);r.put("status","pending");r.put("createdAt",FieldValue.serverTimestamp());
        db.collection("nikahMentorshipRequests").add(r).addOnSuccessListener(v->Toast.makeText(this,"Real mentorship request submitted.",Toast.LENGTH_LONG).show()).addOnFailureListener(e->Toast.makeText(this,"Could not submit the request. Please try again.",Toast.LENGTH_LONG).show());
    }
}
