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
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(95,108,103), light=Color.rgb(247,250,249);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        AzureAuthManager.bindActivity(this);
        home();
    }

    private void base(){
        ScrollView s=new ScrollView(this); s.setFillViewport(true); s.setBackgroundColor(light);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(22,24,22,32);
        s.addView(root); setContentView(s);
    }
    private TextView text(String v,int size,boolean bold){
        TextView t=new TextView(this); t.setText(v); t.setTextSize(size); t.setTextColor(bold?dark:gray); t.setPadding(6,8,6,12);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private void title(String v){TextView t=text(v,28,true);t.setGravity(Gravity.CENTER);root.addView(t);}
    private int dp(int value){
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private Button button(String label,boolean filled){
        Button b=new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(filled?Color.WHITE:green);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(56));
        b.setMinimumHeight(dp(56));
        b.setPadding(dp(12),0,dp(12),0);
        GradientDrawable g=new GradientDrawable();
        g.setColor(filled?green:Color.WHITE);
        g.setCornerRadius(dp(18));
        if(!filled)g.setStroke(dp(2),green);
        b.setBackground(g);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(62));
        lp.setMargins(0,dp(4),0,dp(4));
        root.addView(b,lp);
        return b;
    }

    private EditText input(String hint){
        EditText e=new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setMinHeight(dp(56));
        e.setPadding(dp(12),0,dp(12),0);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(62));
        lp.setMargins(0,dp(4),0,dp(4));
        root.addView(e,lp);
        return e;
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    private void home(){
        base(); title("Best Nikah Bridge");
        TextView authStatus=text("Verifying real Azure token with the production API…",15,false); root.addView(authStatus);
        AzureApiClient.get("/auth/azure/me",new AzureApiClient.Callback(){ public void ok(int code,String body){runOnUiThread(()->authStatus.setText("Azure token verified by production API."));} public void err(String m){runOnUiThread(()->authStatus.setText("Azure token verification failed: "+m));} });
        root.addView(text("Azure External ID • Azure API • PostgreSQL • Azure Storage\nReal Azure session — no demo data.",16,false));

        Button profile=button("My Real Azure Profile",true);
        Button matches=button("Real Compatibility Matches",true);
        Button interests=button("Mutual Interests",true);
        Button family=button("Family / Wali Connect",true);
        Button privacy=button("Privacy Controls",true);
        Button verification=button("Verification Status",false);
        Button safety=button("Safety Reports",false);
        Button ai=button("Azure AI Nikah Assistant",false);
        Button wallet=button("Azure Wallet",false);
        Button delete=button("Permanently Delete Account",false);
        Button out=button("Sign out of Azure",false);

        profile.setOnClickListener(v->profile());
        matches.setOnClickListener(v->matches());
        interests.setOnClickListener(v->interests());
        family.setOnClickListener(v->familyWali());
        privacy.setOnClickListener(v->privacy());
        verification.setOnClickListener(v->verification());
        safety.setOnClickListener(v->safety());
        ai.setOnClickListener(v->ai());
        wallet.setOnClickListener(v->startActivity(new Intent(this,AzureWalletActivity.class)));
        delete.setOnClickListener(v->deleteAccount());
        out.setOnClickListener(v->signOut());
    }

    private void profile(){
        base(); title("My Real Azure Profile");
        TextView status=text("Loading secure profile…",15,false); root.addView(status);
        AzureApiClient.get("/profile",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONObject p=new JSONObject(body).optJSONObject("profile");
                    if(p==null){status.setText("No profile yet. Create your real profile below.");}
                    else status.setText("Email: "+p.optString("email")+"\nName: "+p.optString("display_name")+"\nGender: "+p.optString("gender")+"\nCountry: "+p.optString("country")+"\nCity: "+p.optString("city")+"\nProfile completed: "+p.optBoolean("profile_completed"));\n                    name.setText(p.optString("display_name",""));\n                    dob.setText(p.optString("date_of_birth",""));\n                    gender.setText(p.optString("gender",""));\n                    country.setText(p.optString("country",""));\n                    city.setText(p.optString("city",""));\n                    bio.setText(p.optString("bio",""));
                }catch(Exception e){status.setText("Profile response could not be read.");}
            });}
            public void err(String m){runOnUiThread(()->status.setText("Profile error: "+m));}
        });
        EditText name=input("Display name");
        EditText dob=input("Date of birth YYYY-MM-DD");
        EditText gender=input("Gender: male or female");
        EditText country=input("Country");
        EditText city=input("City");
        EditText bio=input("Short bio");
        Button save=button("Save Real Profile",true),back=button("Back",false);
        save.setOnClickListener(v->{
            try{
                JSONObject b=new JSONObject();
                b.put("displayName",name.getText().toString().trim());
                b.put("dateOfBirth",dob.getText().toString().trim());
                b.put("gender",gender.getText().toString().trim().toLowerCase());
                b.put("country",country.getText().toString().trim());
                b.put("city",city.getText().toString().trim());
                b.put("bio",bio.getText().toString().trim());
                b.put("profileCompleted",true); b.put("isVisible",true);
                AzureApiClient.put("/profile",b.toString(),new AzureApiClient.Callback(){
                    public void ok(int code,String body){runOnUiThread(()->toast("Real Azure profile saved."));}
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
            public void ok(int code,String body){runOnUiThread(()->out.setText(body));}
            public void err(String m){runOnUiThread(()->out.setText("Interests unavailable: "+m));}
        });
        EditText uid=input("Real recipient user ID");
        Button send=button("Send Real Interest",true);
        send.setOnClickListener(v->{try{
            String receiverId=uid.getText().toString().trim();\n            if(receiverId.isEmpty()){uid.setError("Real recipient user ID required");uid.requestFocus();return;}\n            JSONObject b=new JSONObject();b.put("receiverUserId",receiverId);
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
            String waliName=name.getText().toString().trim(); String waliEmail=email.getText().toString().trim(); String waliPhone=phone.getText().toString().trim();\n            if(waliName.length()<2){name.setError("Wali full name required");name.requestFocus();return;}\n            if(waliEmail.isEmpty() && waliPhone.isEmpty()){email.setError("Email or E.164 phone required");email.requestFocus();return;}\n            JSONObject b=new JSONObject();b.put("waliName",waliName);b.put("waliEmail",waliEmail);b.put("waliPhoneE164",waliPhone);
            AzureApiClient.post("/family-links",b.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->{toast("Real Wali connection request saved in Azure.");loadFamilyLinks(out);});}
                public void err(String m){runOnUiThread(()->toast("Wali connection failed: "+m));}
            });
        }catch(Exception e){toast("Wali information is invalid.");}});

        sectionTitle("Wali Verification");
        EditText linkId=input("Family link ID");
        Button verify=button("Verify This Wali Account",false);
        verify.setOnClickListener(v->{String id=linkId.getText().toString().trim();if(id.isEmpty()){linkId.setError("Family link ID required");return;}
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
            public void ok(int code,String body){runOnUiThread(()->out.setText("Azure Family/Wali data:\n"+body));}
            public void err(String m){runOnUiThread(()->out.setText("Family/Wali unavailable: "+m));}
        });
    }

    private void privacy(){
        base(); title("Privacy Controls");
        TextView out=text("Loading real privacy settings…",15,false);root.addView(out);
        AzureApiClient.get("/privacy",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->out.setText(body));}
            public void err(String m){runOnUiThread(()->out.setText("Privacy unavailable: "+m));}
        });
        CheckBox discover=new CheckBox(this);discover.setText("Profile discoverable");root.addView(discover);
        CheckBox city=new CheckBox(this);city.setText("Show city");root.addView(city);
        CheckBox photo=new CheckBox(this);photo.setText("Show photo to matches");root.addView(photo);
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
            public void ok(int code,String body){runOnUiThread(()->out.setText(body));}
            public void err(String m){runOnUiThread(()->out.setText("Verification unavailable: "+m));}
        });
        Button back=button("Back",false);back.setOnClickListener(v->home());
    }

    private void safety(){
        base(); title("Safety Reports");
        TextView out=text("Loading your real Azure safety reports…",15,false);root.addView(out);
        AzureApiClient.get("/safety/reports/mine",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->out.setText(body));}
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
            String question=q.getText().toString().trim();\n            if(question.isEmpty()){q.setError("Nikah question required");q.requestFocus();return;}\n            JSONObject msg=new JSONObject();msg.put("role","user");msg.put("content",question);
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
        new AlertDialog.Builder(this)
            .setTitle("Delete Azure account data permanently?")
            .setMessage("This permanently removes your app profile, photos, verification documents and related records from Azure storage/database. Your Azure External ID sign-in identity is not deleted by this app endpoint.")
            .setPositiveButton("Delete", (d,w)->{
                AzureApiClient.delete("/account",null,new AzureApiClient.Callback(){
                    public void ok(int code,String body){
                        runOnUiThread(()->{
                            AzureAuthManager.removeCurrentAccount(AzureHomeActivity.this,ok->{
                                Toast.makeText(AzureHomeActivity.this,"Azure account data deleted.",Toast.LENGTH_LONG).show();
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
        new AlertDialog.Builder(this).setTitle("Sign out of Azure").setMessage("This removes the Azure account from this app's MSAL cache. Your account and Azure data are not deleted.").setPositiveButton("Sign out",(d,w)->{
            AzureAuthManager.removeCurrentAccount(this,ok->{startActivity(new Intent(this,WelcomeActivity.class));finish();});
        }).setNegativeButton("Cancel",null).show();
    }
}
