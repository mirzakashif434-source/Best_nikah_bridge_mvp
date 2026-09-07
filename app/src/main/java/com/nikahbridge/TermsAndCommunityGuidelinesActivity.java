package com.nikahbridge;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Real Terms + Community Guidelines acceptance gate for user-generated content.
 * Acceptance is stored against the authenticated Firebase user before profile
 * photos or community posts can be created.
 */
public class TermsAndCommunityGuidelinesActivity extends Activity {
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(95,108,103), light=Color.rgb(247,250,249);
    private CheckBox agree;
    private Button accept;
    private static final String TERMS_VERSION="2026-09-07-v1";

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(dp(4),dp(7),dp(4),dp(7));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        if(FirebaseAuth.getInstance().getCurrentUser()==null){finish();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(20),dp(22),dp(28));root.setBackgroundColor(light);
        ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        TextView title=text("Terms & Community Guidelines",27,true);title.setGravity(Gravity.CENTER);content.addView(title,new LinearLayout.LayoutParams(-1,dp(65)));
        content.addView(text("Best Nikah Bridge is a serious Muslim matrimonial service. It is not a dating app. These rules apply before you create or upload user-generated content.",16,false));
        content.addView(text("You agree that:\n\n• You will use Best Nikah Bridge only for genuine marriage/Nikah intentions.\n• You will provide truthful information and will not impersonate another person.\n• You will not post or upload sexual, exploitative, abusive, hateful, fraudulent, or illegal content.\n• You will not request money, passwords, OTPs, banking details, or other sensitive credentials from members.\n• You will not use the service for scams, spam, harassment, stalking, promotion, or unwanted contact.\n• You will not upload another person's photo or private material without permission.\n• Community posts and profile photos may be reviewed, reported, blocked, removed, or restricted when necessary for safety and policy enforcement.\n• Use Report and Block tools when you see harmful content.\n• We may preserve limited safety/legal records where required by law or necessary to prevent fraud and abuse.\n• Accounts that violate these rules may be restricted, suspended, or removed.\n\nChild sexual abuse and exploitation material is strictly prohibited. Any suspected CSAM/CSAE is handled according to applicable law and platform safety procedures.",15,false));
        content.addView(text("Current policy version: "+TERMS_VERSION,13,false));
        agree=new CheckBox(this);agree.setText("I have read and agree to the Terms & Community Guidelines.");agree.setTextSize(15);content.addView(agree,new LinearLayout.LayoutParams(-1,dp(60)));
        accept=new Button(this);accept.setText("Accept & Continue");accept.setAllCaps(false);accept.setTextSize(16);accept.setTextColor(Color.WHITE);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(green);bg.setCornerRadius(dp(18));accept.setBackground(bg);content.addView(accept,new LinearLayout.LayoutParams(-1,dp(62)));
        Button privacy=new Button(this);privacy.setText("View Privacy Policy");privacy.setAllCaps(false);privacy.setTextColor(green);content.addView(privacy,new LinearLayout.LayoutParams(-1,dp(54)));
        Button back=new Button(this);back.setText("Back");back.setAllCaps(false);back.setTextColor(green);content.addView(back,new LinearLayout.LayoutParams(-1,dp(54)));
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));setContentView(root);

        accept.setOnClickListener(v->saveAcceptance());
        privacy.setOnClickListener(v->{android.content.Intent i=new android.content.Intent(this,PrivacyControlCenterActivity.class);startActivity(i);});
        back.setOnClickListener(v->finish());
    }

    private void saveAcceptance(){
        if(!agree.isChecked()){Toast.makeText(this,"Please read and accept the Terms & Community Guidelines.",Toast.LENGTH_LONG).show();return;}
        String uid=FirebaseAuth.getInstance().getCurrentUser().getUid();accept.setEnabled(false);accept.setText("Saving securely…");
        Map<String,Object> data=new HashMap<>();data.put("termsAccepted",true);data.put("termsVersion",TERMS_VERSION);data.put("termsAcceptedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("users").document(uid).set(data, SetOptions.merge())
                .addOnSuccessListener(v->{setResult(RESULT_OK);Toast.makeText(this,"Terms accepted. You can now create community content and upload profile photos.",Toast.LENGTH_LONG).show();finish();})
                .addOnFailureListener(e->{accept.setEnabled(true);accept.setText("Accept & Continue");Toast.makeText(this,"Could not save acceptance. Please try again.",Toast.LENGTH_LONG).show();});
    }
}
