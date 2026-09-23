package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

/** Trust Passport built only from real Azure/PostgreSQL evidence. */
public class TrustPassportActivity extends Activity {
    private LinearLayout root;
    private LinearLayout results;
    private Button checkButton;
    private final int green=Color.rgb(18,103,82),dark=Color.rgb(30,45,41),gray=Color.rgb(85,100,95),light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);render();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void base(){
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setClipToPadding(false);s.setBackgroundColor(light);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(22),dp(20),dp(30));root.setBackgroundColor(light);
        s.addView(root);setContentView(s);
        ViewCompat.setOnApplyWindowInsetsListener(s,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(20),dp(22),dp(20),dp(30)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(s);
    }
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(dp(6),dp(8),dp(6),dp(10));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(dp(18));if(!fill)g.setStroke(dp(2),green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);root.addView(e,new LinearLayout.LayoutParams(-1,dp(62)));return e;}

    private void render(){
        base();root.addView(txt("Trust Passport",27,true));
        root.addView(txt("A transparent evidence card built from real Azure verification, photo and profile records. No fabricated badge or hidden personality score.",15,false));
        EditText id=input("Real matched member ID");checkButton=btn("View Real Trust Passport",true);checkButton.setOnClickListener(v->load(id.getText().toString().trim()));
        Button family=btn("Family Bridge 2.0",true);family.setOnClickListener(v->{
            if(!AzureAuthManager.hasAccount(this)){
                LanguageManager.dialog(this)
                    .setTitle("Sign in required")
                    .setMessage("Sign in with Azure to open your real Family Bridge.")
                    .setPositiveButton("Sign in",(d,w)->startActivity(new Intent(this,AzureExternalAuthActivity.class)))
                    .setNegativeButton("Not now",null)
                    .show();
            }else startActivity(new Intent(this,FamilyBridge2Activity.class));
        });
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
        results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);root.addView(results);
    }

    private void load(String id){
        if(!AzureAuthManager.hasAccount(this)){
            LanguageManager.dialog(this)
                .setTitle("Sign in required")
                .setMessage("Sign in with Azure to view real Trust Passport evidence.")
                .setPositiveButton("Sign in",(d,w)->startActivity(new Intent(this,AzureExternalAuthActivity.class)))
                .setNegativeButton("Not now",null)
                .show();
            return;
        }
        if(id.isEmpty()){toast("Enter a real matched member ID.");return;}
        results.removeAllViews();
        checkButton.setEnabled(false);
        checkButton.setText("Loading real Trust Passport…");
        AzureApiClient.get("/matches/"+id,new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                checkButton.setEnabled(true);checkButton.setText("View Real Trust Passport");
                try{
                    JSONObject m=new JSONObject(body).optJSONObject("match");if(m==null){toast("Trust evidence unavailable.");return;}
                    results.addView(txt("Real member",20,true));
                    results.addView(txt(m.optString("displayName","Member")+" • Age "+m.optInt("age",0)+" • "+m.optString("country",""),17,true));
                    String intent=m.optString("marriageIntention","");
                    addEvidence("Marriage intent",intent.isEmpty()?"NOT STATED":intent,!intent.isEmpty());
                    addEvidence("Identity verification",m.optBoolean("identityVerified",false)?"APPROVED — Azure verification record exists":"NOT APPROVED — no approved verification record",m.optBoolean("identityVerified",false));
                    addEvidence("Profile photo",m.optBoolean("photoPresent",false)?"APPROVED PHOTO PRESENT":"NO APPROVED PHOTO RECORDED",m.optBoolean("photoPresent",false));
                    addEvidence("Profile readiness","Readiness value: "+m.optInt("readinessScore",0)+"/100",m.optInt("readinessScore",0)>0);
                    addEvidence("Compatibility context","Azure compatibility: "+m.optInt("compatibilityScore",0)+"/100",true);
                    JSONArray reasons=m.optJSONArray("whyWeMatched");if(reasons!=null&&reasons.length()>0){StringBuilder s=new StringBuilder();for(int i=0;i<reasons.length();i++)s.append("• ").append(reasons.optString(i)).append("\n");results.addView(txt("Recorded match factors\n"+s,15,false));}
                    addEvidence("Safety limitation","Trust Passport never overrides blocks, reports, verification, or privacy controls.",true);
                    results.addView(txt("Trust Passport is evidence, not a guarantee of character, honesty, compatibility, or marriage outcome.",14,false));
                }catch(Exception e){toast("Azure Trust Passport response could not be read.");}
            });}
            public void err(String message){runOnUiThread(()->{checkButton.setEnabled(true);checkButton.setText("View Real Trust Passport");toast("Could not load Trust Passport: "+message);});}
        });
    }
    private void addEvidence(String label,String value,boolean positive){TextView t=txt((positive?"✓ ":"• ")+label+"\n"+value,16,true);t.setPadding(dp(10),dp(12),dp(10),dp(12));results.addView(t);}
    private void toast(String x){LanguageManager.toast(this,x,Toast.LENGTH_LONG).show();}
}
