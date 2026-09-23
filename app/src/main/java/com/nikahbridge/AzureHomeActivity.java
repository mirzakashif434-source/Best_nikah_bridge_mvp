package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AzureHomeActivity extends Activity {
    private LinearLayout root;
    private final int green=Premium2030Ui.GREEN, dark=Premium2030Ui.TEXT, gray=Premium2030Ui.MUTED, light=Premium2030Ui.CREAM;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        home();
    }

    private void base(){
        ScrollView s=new ScrollView(this); s.setFillViewport(true); s.setVerticalScrollBarEnabled(false); s.setBackgroundColor(light);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(18),dp(20),dp(18),dp(32));
        s.addView(root); setContentView(s);
    }
    private TextView text(String v,int size,boolean bold){
        TextView t=new TextView(this); t.setText(v); t.setTextSize(size); t.setTextColor(bold?dark:gray); t.setPadding(6,8,6,12);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private void title(String v){root.addView(Premium2030Ui.title(this,v));}
    private int dp(int value){
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Button button(String label,boolean filled){
        Button b=filled?Premium2030Ui.primary(this,label):Premium2030Ui.secondary(this,label);
        Premium2030Ui.addButton(root,b);
        return b;
    }

    private EditText input(String hint){
        EditText e=new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setTextColor(dark);
        e.setHintTextColor(gray);
        e.setMinHeight(dp(56));
        e.setPadding(dp(14),0,dp(14),0);
        e.setBackground(Premium2030Ui.outlined(this,Color.WHITE,Premium2030Ui.GOLD_SOFT,16));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));
        lp.setMargins(0,dp(4),0,dp(4));
        root.addView(e,lp);
        return e;
    }
    private void toast(String s){LanguageManager.toast(this,s,Toast.LENGTH_LONG).show();}
    private String valueOrEmpty(JSONObject o,String key){
        if(o==null || !o.has(key) || o.isNull(key)) return "";
        return o.optString(key,"");
    }

    private void home(){
        base(); title("Best Nikah Bridge");
        root.addView(Premium2030Ui.subtitle(this,"Faith • Family • Trust • A Brighter Tomorrow"));
        root.addView(Premium2030Ui.heroLine(this,"Meaningful Matches • Verified Profiles • Private & Safe"));
        TextView authStatus=text("Verifying secure Azure session…",14,false); root.addView(authStatus);
        AzureApiClient.get("/auth/azure/me",new AzureApiClient.Callback(){ public void ok(int code,String body){runOnUiThread(()->authStatus.setText("✓ Secure Azure session verified"));} public void err(String m){runOnUiThread(()->authStatus.setText("Azure token verification failed: "+m));} });
        root.addView(Premium2030Ui.section(this,"Your Nikah Journey"));

        Button profile=button("My Real Azure Profile",true);
        Button matches=button("Real Compatibility Matches",true);
        Button seriousPlus=button("Serious Nikah Plus",true);
        Button interests=button("Mutual Interests",true);
        Button family=button("Family / Wali Connect",true);
        Button privacy=button("Privacy Controls",true);
        Button verification=button("Verification Status",false);
        Button safety=button("Safety Reports",false);
        Button ai=button("Azure AI Nikah Assistant",false);
        Button ownerAnalytics=button("Owner Live Analytics",false);
        Button ownerWallet=button("Owner Wallet / Payout",false);
        Button wallet=button("Azure Wallet",false);
        Button delete=button("Permanently Delete Account",false);
        Button out=button("Sign out of Azure",false);

        profile.setOnClickListener(v->profile());
        matches.setOnClickListener(v->matches());
        seriousPlus.setOnClickListener(v->startActivity(new Intent(this,SeriousNikahPlusActivity.class)));
        interests.setOnClickListener(v->interests());
        family.setOnClickListener(v->familyWali());
        privacy.setOnClickListener(v->privacy());
        verification.setOnClickListener(v->verification());
        safety.setOnClickListener(v->safety());
        ai.setOnClickListener(v->ai());
        ownerAnalytics.setOnClickListener(v->startActivity(new Intent(this,OwnerLiveAnalyticsActivity.class)));
        ownerWallet.setOnClickListener(v->startActivity(new Intent(this,OwnerEarningsActivity.class)));
        wallet.setOnClickListener(v->startActivity(new Intent(this,AzureWalletActivity.class)));
        delete.setOnClickListener(v->deleteAccount());
        out.setOnClickListener(v->signOut());
    }

    private void profile(){
        base(); title("My Real Azure Profile");
        TextView status=text("Loading secure profile…",15,false); root.addView(status);
        EditText name=input("Display name");
        EditText dob=input("Date of birth YYYY-MM-DD");
        EditText gender=input("Gender: male or female");
        EditText country=input("Country");
        EditText city=input("City");
        EditText education=input("Education");
        EditText familyInvolvement=input("Family involvement style");
        EditText bio=input("Short bio");
        AzureApiClient.get("/profile",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject p=new JSONObject(body).optJSONObject("profile");
                    if(p==null){status.setText("No profile yet. Create your real profile below.");}
                    else status.setText("Email: "+valueOrEmpty(p,"email")+"\nName: "+valueOrEmpty(p,"display_name")+"\nGender: "+valueOrEmpty(p,"gender")+"\nCountry: "+valueOrEmpty(p,"country")+"\nCity: "+valueOrEmpty(p,"city")+"\nProfile completed: "+p.optBoolean("profile_completed"));
                    name.setText(valueOrEmpty(p,"display_name"));
                    dob.setText(valueOrEmpty(p,"date_of_birth"));
                    gender.setText(valueOrEmpty(p,"gender"));
                    country.setText(valueOrEmpty(p,"country"));
                    city.setText(valueOrEmpty(p,"city"));
                    education.setText(valueOrEmpty(p,"education"));
                    familyInvolvement.setText(valueOrEmpty(p,"family_involvement"));
                    bio.setText(valueOrEmpty(p,"bio"));
                }catch(Exception e){status.setText("Profile response could not be read.");}
            });}
            public void err(String m){runOnUiThread(()->status.setText("Profile error: "+m));}
        });
        Button save=button("Save Real Profile",true),back=button("Back",false);
        save.setOnClickListener(v->{
            try{
                String displayName=name.getText().toString().trim();
                String birthDate=dob.getText().toString().trim();
                String profileGender=gender.getText().toString().trim().toLowerCase(java.util.Locale.US);

                if(displayName.length()<2){LanguageManager.setError(name,"Display name is required");name.requestFocus();return;}
                if(!birthDate.matches("\\d{4}-\\d{2}-\\d{2}")){LanguageManager.setError(dob,"Use YYYY-MM-DD, for example 1990-05-21");dob.requestFocus();return;}
                java.text.SimpleDateFormat df=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US);
                df.setLenient(false);
                java.util.Date parsedBirth;
                try{parsedBirth=df.parse(birthDate);}catch(Exception ex){LanguageManager.setError(dob,"Enter a real calendar date");dob.requestFocus();return;}
                java.util.Calendar today=java.util.Calendar.getInstance();
                java.util.Calendar birth=java.util.Calendar.getInstance();
                birth.setTime(parsedBirth);
                int age=today.get(java.util.Calendar.YEAR)-birth.get(java.util.Calendar.YEAR);
                if(today.get(java.util.Calendar.DAY_OF_YEAR)<birth.get(java.util.Calendar.DAY_OF_YEAR)) age--;
                if(age<18||age>100){LanguageManager.setError(dob,"Age must be between 18 and 100");dob.requestFocus();return;}
                if(!"male".equals(profileGender)&&!"female".equals(profileGender)){LanguageManager.setError(gender,"Enter male or female");gender.requestFocus();return;}

                JSONObject b=new JSONObject();
                b.put("displayName",displayName);
                b.put("dateOfBirth",birthDate);
                b.put("gender",profileGender);
                b.put("country",country.getText().toString().trim());
                b.put("city",city.getText().toString().trim());
                b.put("education",education.getText().toString().trim());
                b.put("familyInvolvement",familyInvolvement.getText().toString().trim());
                b.put("bio",bio.getText().toString().trim());
                b.put("profileCompleted",true); b.put("isVisible",true);
                AzureApiClient.put("/profile",b.toString(),new AzureApiClient.Callback(){
                    public void ok(int code,String body){runOnUiThread(()->{toast("Real Azure profile saved.");profile();});}
                    public void err(String m){runOnUiThread(()->toast("Profile save failed: "+m));}
                });
            }catch(Exception e){toast("Invalid profile data.");}
        });
        back.setOnClickListener(v->home());
    }

    private void matches(){
        base(); title("Real Compatibility Matches");
        TextView out=text("Loading real matches from Azure PostgreSQL…",15,false);root.addView(out);
        AzureApiClient.get("/matches",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject j=new JSONObject(body); JSONArray a=j.optJSONArray("matches");
                    if(a==null||a.length()==0){out.setText("No real matches are available yet.");return;}
                    StringBuilder s=new StringBuilder();
                    for(int i=0;i<Math.min(a.length(),50);i++){
                        JSONObject m=a.getJSONObject(i);
                        s.append("\n").append(i+1).append(". ").append(m.optString("displayName"))
                         .append(" • ").append(m.optInt("age")).append("\n")
                         .append("Compatibility: ").append(m.optInt("compatibilityScore")).append("/100\n")
                         .append("Why matched: ").append(m.optJSONArray("whyWeMatched")).append("\n");
                    }
                    out.setText(s.toString());
                }catch(Exception e){out.setText("Match response could not be read.");}
            });}
            public void err(String m){runOnUiThread(()->{
                if(m!=null && m.contains("HTTP 409") && m.contains("PROFILE_NOT_READY")){
                    out.setText("Your real profile is not ready yet. Complete your profile first; then real compatibility matching will become available.");
                    Button complete=button("Complete My Real Profile",true);
                    complete.setOnClickListener(v->profile());
                }else{
                    out.setText("Matches unavailable: "+m);
                }
            });}
        });
        Button back=button("Back",false);back.setOnClickListener(v->home());
    }

    private void interests(){
        base(); title("Mutual Interests");
        TextView out=text("Loading secure interests…",15,false);root.addView(out);
        AzureApiClient.get("/interests",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("interests");
                    if(a==null||a.length()==0){out.setText("No interests yet.");return;}
                    StringBuilder s=new StringBuilder();
                    for(int i=0;i<Math.min(a.length(),100);i++){
                        JSONObject x=a.optJSONObject(i);if(x==null)continue;
                        s.append(x.optString("direction","").equals("received")?"Received from: ":"Sent to: ")
                         .append(x.optString("display_name","Member"))
                         .append("\nStatus: ").append(x.optString("status","pending"))
                         .append("\nCountry/City: ").append(x.optString("country","")).append(" ").append(x.optString("city",""))
                         .append("\n\n");
                    }
                    out.setText(s.toString());
                }catch(Exception e){out.setText("Interests could not be displayed.");}
            });}
            public void err(String m){runOnUiThread(()->out.setText("Interests unavailable: "+m));}
        });
        EditText uid=input("Real recipient user ID");
        Button send=button("Send Real Interest",true);
        send.setOnClickListener(v->{try{
            String receiverId=uid.getText().toString().trim();
            if(receiverId.isEmpty()){LanguageManager.setError(uid,"Real recipient user ID required");uid.requestFocus();return;}
            JSONObject b=new JSONObject();b.put("receiverUserId",receiverId);
            AzureApiClient.post("/interests",b.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->toast("Real interest sent."));}
                public void err(String m){runOnUiThread(()->toast("Interest rejected: "+m));}
            });
        }catch(Exception e){toast("Invalid recipient.");}});
        Button back=button("Back",false);back.setOnClickListener(v->home());
    }

    private void familyWali(){
        base(); title("Family / Wali Connect");
        TextView out=text("Loading your real Family/Wali links from Azure PostgreSQL…",15,false);root.addView(out);
        loadFamilyLinks(out);

        sectionTitle("Create / Update Wali Connection");
        EditText name=input("Wali full name");
        EditText email=input("Wali email (optional)");
        EditText phone=input("Wali phone E.164 (optional)");
        Button create=button("Connect Real Wali",true);
        create.setOnClickListener(v->{try{
            String waliName=name.getText().toString().trim(); String waliEmail=email.getText().toString().trim(); String waliPhone=phone.getText().toString().trim();
            if(waliName.length()<2){LanguageManager.setError(name,"Wali full name required");name.requestFocus();return;}
            if(waliEmail.isEmpty() && waliPhone.isEmpty()){LanguageManager.setError(email,"Email or E.164 phone required");email.requestFocus();return;}
            JSONObject b=new JSONObject();b.put("waliName",waliName);b.put("waliEmail",waliEmail);b.put("waliPhoneE164",waliPhone);
            AzureApiClient.post("/family-links",b.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->{toast("Real Wali connection request saved in Azure.");loadFamilyLinks(out);});}
                public void err(String m){runOnUiThread(()->toast("Wali connection failed: "+m));}
            });
        }catch(Exception e){toast("Wali information is invalid.");}});

        sectionTitle("Wali Verification");
        EditText linkId=input("Family link ID");
        Button verify=button("Verify This Wali Account",false);
        verify.setOnClickListener(v->{String id=linkId.getText().toString().trim();if(id.isEmpty()){LanguageManager.setError(linkId,"Family link ID required");return;}
            AzureApiClient.post("/family-links/"+id+"/verify","{}",new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->{toast("Wali identity verified in Azure.");loadFamilyLinks(out);});}
                public void err(String m){runOnUiThread(()->toast("Wali verification failed: "+m));}
            });
        });

        Button back=button("Back",false);back.setOnClickListener(v->home());
    }

    private void sectionTitle(String v){root.addView(text(v,19,true));}

    private void loadFamilyLinks(TextView out){
        AzureApiClient.get("/family-links",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("familyLinks");
                    if(a==null||a.length()==0){out.setText("No Family/Wali connection yet.");return;}
                    StringBuilder s=new StringBuilder();
                    for(int i=0;i<a.length();i++){
                        JSONObject x=a.optJSONObject(i);if(x==null)continue;
                        s.append(x.optString("wali_name","Wali"))
                         .append("\nStatus: ").append(x.optString("status","pending"));
                        String email=x.optString("wali_email",""),phone=x.optString("wali_phone_e164","");
                        if(!email.isEmpty())s.append("\nEmail: ").append(email);
                        if(!phone.isEmpty())s.append("\nPhone: ").append(phone);
                        s.append("\nLink ID: ").append(x.optString("id","")).append("\n\n");
                    }
                    out.setText(s.toString());
                }catch(Exception e){out.setText("Family/Wali data could not be displayed.");}
            });}
            public void err(String m){runOnUiThread(()->out.setText("Family/Wali unavailable: "+m));}
        });
    }

    private void privacy(){
        base(); title("Privacy Controls");
        TextView out=text("Loading real privacy settings…",15,false);root.addView(out);
        CheckBox discover=new CheckBox(this);discover.setText("Profile discoverable");root.addView(discover);
        CheckBox city=new CheckBox(this);city.setText("Show city");root.addView(city);
        CheckBox photo=new CheckBox(this);photo.setText("Show photo to matches");root.addView(photo);
        AzureApiClient.get("/privacy",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject p=new JSONObject(body).optJSONObject("privacy");
                    if(p==null){out.setText("Privacy settings are ready.");return;}
                    discover.setChecked(p.optBoolean("profile_discoverable",true));
                    city.setChecked(p.optBoolean("show_city",true));
                    photo.setChecked(p.optBoolean("show_photo_to_matches",true));
                    out.setText("Current privacy settings loaded.");
                }catch(Exception e){out.setText("Privacy settings could not be displayed.");}
            });}
            public void err(String m){runOnUiThread(()->out.setText("Privacy unavailable: "+m));}
        });
        Button save=button("Save Privacy Controls",true);
        save.setOnClickListener(v->{try{
            JSONObject b=new JSONObject();b.put("profileDiscoverable",discover.isChecked());b.put("showCity",city.isChecked());b.put("showPhotoToMatches",photo.isChecked());
            AzureApiClient.patch("/privacy",b.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->toast("Privacy controls saved in Azure."));}
                public void err(String m){runOnUiThread(()->toast("Privacy save failed: "+m));}
            });
        }catch(Exception e){toast("Privacy data invalid.");}});
        Button back=button("Back",false);back.setOnClickListener(v->home());
    }

    private void verification(){
        base(); title("Real Verification");
        TextView out=text("Loading Azure verification status…",15,false);root.addView(out);
        AzureApiClient.get("/verification",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject o=new JSONObject(body);
                    JSONArray a=o.optJSONArray("verifications");
                    if(a==null)a=o.optJSONArray("items");
                    if(a==null||a.length()==0){out.setText("No verification submission yet.");return;}
                    StringBuilder s=new StringBuilder();
                    for(int i=0;i<a.length();i++){
                        JSONObject x=a.optJSONObject(i);if(x==null)continue;
                        s.append("Type: ").append(x.optString("type",x.optString("verification_type","Verification")))
                         .append("\nStatus: ").append(x.optString("status","pending"))
                         .append("\nSubmitted: ").append(x.optString("created_at",x.optString("submitted_at","")))
                         .append("\n\n");
                    }
                    out.setText(s.toString());
                }catch(Exception e){out.setText("Verification status could not be displayed.");}
            });}
            public void err(String m){runOnUiThread(()->out.setText("Verification unavailable: "+m));}
        });
        Button back=button("Back",false);back.setOnClickListener(v->home());
    }

    private void safety(){
        base(); title("Safety Reports");
        TextView out=text("Loading your real Azure safety reports…",15,false);root.addView(out);
        AzureApiClient.get("/safety/reports/mine",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("reports");
                    if(a==null||a.length()==0){out.setText("No safety reports submitted.");return;}
                    StringBuilder s=new StringBuilder();
                    for(int i=0;i<a.length();i++){
                        JSONObject x=a.optJSONObject(i);if(x==null)continue;
                        s.append("Reason: ").append(x.optString("reason",""))
                         .append("\nStatus: ").append(x.optString("status","pending"))
                         .append("\nDate: ").append(x.optString("created_at",""))
                         .append("\n\n");
                    }
                    out.setText(s.toString());
                }catch(Exception e){out.setText("Safety reports could not be displayed.");}
            });}
            public void err(String m){runOnUiThread(()->out.setText("Safety data unavailable: "+m));}
        });
        Button report=button("Create Safety Report",true);
        report.setOnClickListener(v->createReport());
        Button back=button("Back",false);back.setOnClickListener(v->home());
    }

    private void createReport(){
        EditText uid=input("Reported real user ID");
        EditText reason=input("Reason: harassment / scam / impersonation / inappropriate_content / unsafe_request / other");
        EditText details=input("Details");
        Button send=button("Submit Real Report",true);
        send.setOnClickListener(v->{try{
            JSONObject b=new JSONObject();b.put("reportedUserId",uid.getText().toString().trim());b.put("reason",reason.getText().toString().trim());b.put("details",details.getText().toString().trim());
            AzureApiClient.post("/safety/reports",b.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->toast("Safety report securely stored in Azure."));}
                public void err(String m){runOnUiThread(()->toast("Report rejected: "+m));}
            });
        }catch(Exception e){toast("Invalid report.");}});
    }

    private void ai(){
        base(); title("Azure AI Nikah Assistant");
        EditText q=input("Ask your real Nikah question");
        TextView answer=text("",15,false);root.addView(answer);
        Button ask=button("Ask Azure AI",true),back=button("Back",false);
        ask.setOnClickListener(v->{try{
            String question=q.getText().toString().trim();
            if(question.isEmpty()){LanguageManager.setError(q,"Nikah question required");q.requestFocus();return;}
            JSONObject msg=new JSONObject();msg.put("role","user");msg.put("content",question);
            JSONArray messages=new JSONArray();messages.put(msg);JSONObject b=new JSONObject();b.put("messages",messages);
            answer.setText("Azure AI is responding…");
            AzureApiClient.post("/ai/nikah-assistant",b.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->{try{answer.setText(new JSONObject(body).optJSONObject("assistant").optString("content"));}catch(Exception e){answer.setText(body);}});}
                public void err(String m){runOnUiThread(()->answer.setText("Azure AI unavailable: "+m));}
            });
        }catch(Exception e){answer.setText("Please enter a clear question.");}});
        back.setOnClickListener(v->home());
    }

    private void deleteAccount(){
        LanguageManager.dialog(this)
            .setTitle("Delete Azure account data permanently?")
            .setMessage("This permanently removes your app profile, photos, verification documents and related records from Azure storage/database. Your Azure External ID sign-in identity is not deleted by this app endpoint.")
            .setPositiveButton("Delete", (d,w)->{
                AzureApiClient.delete("/account",null,new AzureApiClient.Callback(){
                    public void ok(int code,String body){
                        runOnUiThread(()->{
                            AzureAuthManager.removeCurrentAccount(AzureHomeActivity.this,ok->{
                                LanguageManager.toast(AzureHomeActivity.this,"Azure account data deleted.",Toast.LENGTH_LONG).show();
                                startActivity(new Intent(AzureHomeActivity.this,WelcomeActivity.class));
                                finish();
                            });
                        });
                    }
                    public void err(String m){runOnUiThread(()->toast("Account deletion failed: "+m));}
                });
            }).setNegativeButton("Cancel",null).show();
    }

    private void signOut(){
        LanguageManager.dialog(this).setTitle("Sign out of Azure").setMessage("This removes the Azure account from this app's MSAL cache. Your account and Azure data are not deleted.").setPositiveButton("Sign out",(d,w)->{
            AzureAuthManager.removeCurrentAccount(this,ok->{startActivity(new Intent(this,WelcomeActivity.class));finish();});
        }).setNegativeButton("Cancel",null).show();
    }
}
