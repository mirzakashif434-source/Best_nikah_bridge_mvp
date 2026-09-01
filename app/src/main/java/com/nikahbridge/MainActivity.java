package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Best Nikah Bridge - production Android entry point.
 * Real Firebase data only. No demo members, fake scores, fake verification,
 * local-only connections, or hard-coded match records.
 */
public class MainActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;
    private LinearLayout root;
    private ExecutorService aiExecutor;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(95,108,103), red=Color.rgb(165,50,50), light=Color.rgb(247,250,249);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();
        functions=FirebaseFunctions.getInstance();
        aiExecutor=Executors.newSingleThreadExecutor();
        route();
    }

    @Override protected void onDestroy(){
        if(aiExecutor!=null) aiExecutor.shutdownNow();
        super.onDestroy();
    }

    @Override public void onBackPressed(){ if(auth.getCurrentUser()==null) authScreen(); else home(); }

    private void route(){
        FirebaseUser u=auth.getCurrentUser();
        if(u==null){authScreen();return;}
        u.reload().addOnSuccessListener(x->{
            if(!u.isEmailVerified()){verificationGate();return;}
            db.collection("users").document(u.getUid()).get().addOnSuccessListener(d->{
                if(!d.exists() || !Boolean.TRUE.equals(d.getBoolean("profileActive"))) profile(); else home();
            }).addOnFailureListener(e->toast("Secure profile check failed. Please try again."));
        }).addOnFailureListener(e->toast("Secure account check failed. Please sign in again."));
    }

    private void base(){
        ScrollView s=new ScrollView(this); s.setFillViewport(true); s.setBackgroundColor(light);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(22,24,22,32); s.addView(root); setContentView(s);
    }
    private TextView text(String v,int size,boolean bold){
        TextView t=new TextView(this); t.setText(v); t.setTextSize(size); t.setTextColor(bold?dark:gray); t.setPadding(6,8,6,12);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private void title(String v){TextView t=text(v,28,true);t.setGravity(Gravity.CENTER);root.addView(t);}
    private Button button(String label,boolean filled){
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(filled?Color.WHITE:green);
        GradientDrawable g=new GradientDrawable();g.setColor(filled?green:Color.WHITE);g.setCornerRadius(18);if(!filled)g.setStroke(2,green);b.setBackground(g);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,62);p.setMargins(0,6,0,6);root.addView(b,p);return b;
    }
    private Button danger(String label){Button b=button(label,false);b.setTextColor(red);GradientDrawable g=new GradientDrawable();g.setColor(Color.WHITE);g.setCornerRadius(18);g.setStroke(2,red);b.setBackground(g);return b;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(16);e.setPadding(14,8,14,8);root.addView(e,new LinearLayout.LayoutParams(-1,62));return e;}
    private void section(String v){root.addView(text(v,19,true));}
    private void toast(String v){Toast.makeText(this,v,Toast.LENGTH_LONG).show();}
    private String val(DocumentSnapshot d,String key){Object v=d.get(key);return v==null?"":String.valueOf(v);}
    private boolean signedIn(){return auth.getCurrentUser()!=null;}
    private int intVal(DocumentSnapshot d,String key,int fallback){Long x=d.getLong(key);return x==null?fallback:x.intValue();}

    private void authScreen(){
        base(); title("Best Nikah Bridge");
        root.addView(text("Real Firebase account • Serious Nikah only • Not a dating app",16,false));
        EditText email=input("Email address"); email.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password=input("Password (minimum 8 characters)"); password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        Button sign=button("Sign in",true),create=button("Create account",false),forgot=button("Forgot password",false),terms=button("Terms & Community Rules",false);
        sign.setOnClickListener(v->{if(!valid(email,password))return;auth.signInWithEmailAndPassword(email.getText().toString().trim(),password.getText().toString()).addOnSuccessListener(x->route()).addOnFailureListener(e->toast("Sign in failed. Check your email and password."));});
        create.setOnClickListener(v->{if(!valid(email,password))return;auth.createUserWithEmailAndPassword(email.getText().toString().trim(),password.getText().toString()).addOnSuccessListener(x->{FirebaseUser u=x.getUser();if(u==null)return;u.sendEmailVerification();Map<String,Object> p=new HashMap<>();p.put("uid",u.getUid());p.put("profileActive",false);p.put("discoverable",false);p.put("verificationStatus","unverified");p.put("termsAccepted",false);p.put("intentConfirmed",false);p.put("createdAt",FieldValue.serverTimestamp());db.collection("users").document(u.getUid()).set(p).addOnSuccessListener(y->verificationGate()).addOnFailureListener(e->toast("Account setup failed."));}).addOnFailureListener(e->toast("Account creation failed. Email may already be registered."));});
        forgot.setOnClickListener(v->{String e=email.getText().toString().trim();if(!e.contains("@")){email.setError("Enter your email first");return;}auth.sendPasswordResetEmail(e).addOnSuccessListener(x->toast("Password reset email sent.")).addOnFailureListener(x->toast("Could not send reset email."));});
        terms.setOnClickListener(v->terms());
    }
    private boolean valid(EditText e,EditText p){if(!e.getText().toString().trim().contains("@")){e.setError("Valid email required");return false;}if(p.getText().toString().length()<8){p.setError("Minimum 8 characters");return false;}return true;}
    private void verificationGate(){
        base();title("Verify your email");root.addView(text("Email verification is required before your profile can become active.",16,false));
        Button resend=button("Resend verification email",true),refresh=button("I verified — continue",false),out=button("Sign out",false);
        resend.setOnClickListener(v->{FirebaseUser u=auth.getCurrentUser();if(u!=null)u.sendEmailVerification().addOnSuccessListener(x->toast("Verification email sent."));});
        refresh.setOnClickListener(v->{FirebaseUser u=auth.getCurrentUser();if(u==null){authScreen();return;}u.reload().addOnSuccessListener(x->{if(u.isEmailVerified())route();else toast("Email is not verified yet.");});});
        out.setOnClickListener(v->{auth.signOut();authScreen();});
    }

    private void home(){
        base();title("Best Nikah Bridge");
        root.addView(text("A serious Muslim marriage platform built around trust, compatibility, family and safe Nikah — not dating.",17,false));
        Button p=button("My Profile & Intent",true),r=button("Nikah Readiness Score",true),m=button("Compatibility Matches",true),i=button("Mutual Interests & Safe Chat",true),w=button("Family / Wali Connect",true),wallet=button("Wallet & Withdrawals",true),s=button("Scam Shield & Safety",false),v=button("Real Verification",false),h=button("AI Nikah Assistant",false),pr=button("Privacy Control Center",false),a=button("Delete Account / Sign out",false);
        p.setOnClickListener(x->profile());r.setOnClickListener(x->readiness());m.setOnClickListener(x->matches());i.setOnClickListener(x->interests());w.setOnClickListener(x->wali());wallet.setOnClickListener(x->startActivity(new android.content.Intent(this,WalletActivity.class)));s.setOnClickListener(x->safety());v.setOnClickListener(x->verification());h.setOnClickListener(x->help());pr.setOnClickListener(x->privacy());a.setOnClickListener(x->account());
        root.addView(text("Real Firebase data only. No demo members, fake match records or fake verification badges.",14,false));
    }

    private void profile(){
        base();title("My Real Nikah Profile");root.addView(text("Complete this honestly. Your profile becomes discoverable only after email verification, Terms acceptance and all required matching fields.",15,false));
        EditText name=input("Full name"),age=input("Age (18+)"),minAge=input("Preferred partner minimum age (18+)"),maxAge=input("Preferred partner maximum age (18+)"),country=input("Country"),city=input("City"),gender=input("Gender"),looking=input("Looking for (gender / preference)"),marital=input("Marital status"),intent=input("Marriage intention"),practice=input("Religious practice"),about=input("About yourself and family values (30+ characters)"),pref=input("Partner preferences: location, values, education, lifestyle, etc."),timeline=input("Marriage timeline"),family=input("Family / Wali involvement preference"),deal=input("Compatibility deal-breakers / non-negotiables (5+ characters)");
        CheckBox accepted=new CheckBox(this);accepted.setText("I accept the Terms & Community Rules and confirm this is a genuine marriage intention.");root.addView(accepted);
        Button save=button("Save & Activate Profile",true),verify=button("Request Verification",false),photo=button("Add / Update Real Profile Photo",false),back=button("Back",false);
        photo.setOnClickListener(v->startActivity(new android.content.Intent(this, ProfilePhotoActivity.class)));
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->{name.setText(val(d,"name"));age.setText(val(d,"age"));minAge.setText(val(d,"preferredMinAge"));maxAge.setText(val(d,"preferredMaxAge"));country.setText(val(d,"country"));city.setText(val(d,"city"));gender.setText(val(d,"gender"));looking.setText(val(d,"lookingFor"));marital.setText(val(d,"maritalStatus"));intent.setText(val(d,"marriageIntent"));practice.setText(val(d,"religiousPractice"));about.setText(val(d,"about"));pref.setText(val(d,"partnerPreference"));timeline.setText(val(d,"marriageTimeline"));family.setText(val(d,"familyInvolvement"));deal.setText(val(d,"dealbreakers"));accepted.setChecked(Boolean.TRUE.equals(d.getBoolean("termsAccepted")));root.addView(text("Verification status: "+val(d,"verificationStatus"),15,false));});
        save.setOnClickListener(v->{
            int a,lo,hi;try{a=Integer.parseInt(age.getText().toString().trim());lo=Integer.parseInt(minAge.getText().toString().trim());hi=Integer.parseInt(maxAge.getText().toString().trim());}catch(Exception e){toast("Age fields must contain numbers.");return;}
            if(a<18||a>100){age.setError("Age must be 18–100");return;}if(lo<18||hi>100||lo>hi){toast("Partner age range must be valid.");return;}
            String n=name.getText().toString().trim(),c=country.getText().toString().trim(),ci=city.getText().toString().trim(),g=gender.getText().toString().trim(),lf=looking.getText().toString().trim(),ms=marital.getText().toString().trim(),in=intent.getText().toString().trim(),rp=practice.getText().toString().trim(),ab=about.getText().toString().trim(),pp=pref.getText().toString().trim(),tl=timeline.getText().toString().trim(),fa=family.getText().toString().trim(),dbk=deal.getText().toString().trim();
            if(n.length()<2||c.isEmpty()||ci.isEmpty()||g.isEmpty()||lf.isEmpty()||ms.isEmpty()||in.isEmpty()||rp.isEmpty()||ab.length()<30||pp.length()<10||tl.isEmpty()||fa.isEmpty()||dbk.length()<5){toast("Complete every required profile and matching field.");return;}
            if(!accepted.isChecked()){toast("Accept the Terms and confirm genuine marriage intention first.");return;}
            FirebaseUser u=auth.getCurrentUser();if(u==null||!u.isEmailVerified()){toast("Verify your email first.");return;}
            Map<String,Object> q=new HashMap<>();q.put("uid",u.getUid());q.put("name",n);q.put("age",a);q.put("preferredMinAge",lo);q.put("preferredMaxAge",hi);q.put("country",c);q.put("city",ci);q.put("gender",g);q.put("lookingFor",lf);q.put("maritalStatus",ms);q.put("marriageIntent",in);q.put("religiousPractice",rp);q.put("about",ab);q.put("partnerPreference",pp);q.put("marriageTimeline",tl);q.put("familyInvolvement",fa);q.put("dealbreakers",dbk);q.put("termsAccepted",true);q.put("intentConfirmed",true);q.put("profileActive",true);q.put("discoverable",true);q.put("updatedAt",FieldValue.serverTimestamp());
            db.collection("users").document(u.getUid()).set(q,SetOptions.merge()).addOnSuccessListener(x->{toast("Real profile activated securely.");home();}).addOnFailureListener(e->toast("Profile could not be saved. Check your connection."));
        });
        verify.setOnClickListener(v->verification());back.setOnClickListener(v->home());
    }

    private void readiness(){
        base();title("Nikah Readiness Score");root.addView(text("A transparent preparation aid — not a religious ruling and never a guarantee of compatibility.",15,false));
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->{int score=0;String[] keys={"name","country","city","gender","lookingFor","maritalStatus","about","partnerPreference","marriageIntent","marriageTimeline","familyInvolvement","dealbreakers"};for(String k:keys)if(!val(d,k).trim().isEmpty())score+=6;if(d.getLong("age")!=null)score+=6;if(d.getLong("preferredMinAge")!=null&&d.getLong("preferredMaxAge")!=null)score+=6;if(Boolean.TRUE.equals(d.getBoolean("termsAccepted")))score+=4;if(Boolean.TRUE.equals(d.getBoolean("intentConfirmed")))score+=4;if("verified".equalsIgnoreCase(val(d,"verificationStatus")))score+=8;if(score>100)score=100;root.addView(text("Nikah Readiness: "+score+" / 100",27,true));root.addView(text(score>=85?"Strong preparation. Focus on family discussion and real-world compatibility.":score>=65?"Good foundation. Complete the remaining areas before moving quickly.":"Build your profile, boundaries, timeline and family involvement first.",17,false));});Button b=button("Back",false);b.setOnClickListener(v->home());
    }

    private void matches(){
        base();title("Compatibility Matches");root.addView(text("Real profiles only. We score reciprocal age/gender preferences, intent, timeline, location, values, deal-breakers and trust signals. A score is a guide, not a promise.",15,false));
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(me->{if(!Boolean.TRUE.equals(me.getBoolean("profileActive"))){toast("Complete your profile first.");profile();return;}loadBlocked(blocked->{db.collection("users").whereEqualTo("profileActive",true).whereEqualTo("discoverable",true).limit(100).get().addOnSuccessListener(q->{ArrayList<Match> list=new ArrayList<>();for(DocumentSnapshot d:q){if(auth.getUid().equals(d.getId())||blocked.contains(d.getId()))continue;int sc=score(me,d);if(sc>=45)list.add(new Match(d,sc));}list.sort((a,b)->Integer.compare(b.score,a.score));if(list.isEmpty())root.addView(text("No strong real matches are available yet. Keep your profile complete and check again as the community grows.",16,false));for(Match x:list)addMatch(me,x.doc,x.score);}).addOnFailureListener(e->toast("Could not load real matches."));});}).addOnFailureListener(e->toast("Could not load your profile."));
        Button b=button("Back",false);b.setOnClickListener(v->home());
    }
    private static class Match{DocumentSnapshot doc;int score;Match(DocumentSnapshot d,int s){doc=d;score=s;}}
    private void loadBlocked(final BlockCallback cb){Set<String> out=new HashSet<>();db.collection("blocks").whereEqualTo("blockerUid",auth.getUid()).get().addOnSuccessListener(a->{for(DocumentSnapshot d:a)out.add(val(d,"blockedUid"));db.collection("blocks").whereEqualTo("blockedUid",auth.getUid()).get().addOnSuccessListener(b->{for(DocumentSnapshot d:b)out.add(val(d,"blockerUid"));cb.done(out);}).addOnFailureListener(e->cb.done(out));}).addOnFailureListener(e->cb.done(out));}
    private interface BlockCallback{void done(Set<String> ids);}

    private int score(DocumentSnapshot me,DocumentSnapshot d){
        int s=0;int theirAge=intVal(d,"age",0),myMin=intVal(me,"preferredMinAge",0),myMax=intVal(me,"preferredMaxAge",101),theirMin=intVal(d,"preferredMinAge",0),theirMax=intVal(d,"preferredMaxAge",101),myAge=intVal(me,"age",0);
        String myIntent=norm(val(me,"marriageIntent")),theirIntent=norm(val(d,"marriageIntent")),myTimeline=norm(val(me,"marriageTimeline")),theirTimeline=norm(val(d,"marriageTimeline")),myCountry=norm(val(me,"country")),theirCountry=norm(val(d,"country")),myCity=norm(val(me,"city")),theirCity=norm(val(d,"city")),myGender=norm(val(me,"gender")),theirGender=norm(val(d,"gender")),myLooking=norm(val(me,"lookingFor")),theirLooking=norm(val(d,"lookingFor")),myPref=norm(val(me,"partnerPreference")),theirPref=norm(val(d,"partnerPreference")),myDeal=norm(val(me,"dealbreakers")),theirDeal=norm(val(d,"dealbreakers"));
        if(theirAge>=myMin&&theirAge<=myMax&&myAge>=theirMin&&myAge<=theirMax)s+=22; else return 0;
        if(compatible(myLooking,theirGender)&&compatible(theirLooking,myGender))s+=18; else return 0;
        if(!myIntent.isEmpty()&&myIntent.equals(theirIntent))s+=15;
        if(!myTimeline.isEmpty()&&myTimeline.equals(theirTimeline))s+=12;
        if(!myCountry.isEmpty()&&myCountry.equals(theirCountry))s+=8;
        if(!myCity.isEmpty()&&myCity.equals(theirCity))s+=6;
        if(keywordOverlap(myPref,theirPref))s+=7;
        if(!hasDealbreakConflict(myDeal,theirPref)&&!hasDealbreakConflict(theirDeal,myPref))s+=7;
        if("verified".equalsIgnoreCase(val(d,"verificationStatus")))s+=3;
        if(Boolean.TRUE.equals(d.getBoolean("intentConfirmed")))s+=2;
        return Math.min(100,s);
    }
    private String norm(String x){return x==null?"":x.toLowerCase(Locale.US).replace("/", " ").replace(","," ").replace("-"," ").trim();}
    private boolean compatible(String looking,String gender){if(looking.isEmpty()||gender.isEmpty())return false;if(looking.contains("any")||looking.contains("no preference"))return true;return looking.contains(gender)||gender.contains(looking);}
    private boolean keywordOverlap(String a,String b){if(a.isEmpty()||b.isEmpty())return false;String[] aa=a.split("\\s+");for(String x:aa)if(x.length()>3&&b.contains(x))return true;return false;}
    private boolean hasDealbreakConflict(String deal,String otherPref){if(deal.isEmpty()||otherPref.isEmpty())return false;String[] aa=deal.split("\\s+");for(String x:aa)if(x.length()>4&&otherPref.contains(x))return true;return false;}

    private void addMatch(DocumentSnapshot me,DocumentSnapshot d,int s){
        final String n=val(d,"name").isEmpty()?"Member":val(d,"name");
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(12,10,12,10);root.addView(card);
        android.widget.ImageView photo=new android.widget.ImageView(this);photo.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);photo.setVisibility(android.view.View.GONE);card.addView(photo,new LinearLayout.LayoutParams(-1,420));String photoUrl=val(d,"photoUrl");if(!photoUrl.trim().isEmpty()){photo.setVisibility(android.view.View.VISIBLE);ProfilePhotoLoader.load(photoUrl,photo);}
        card.addView(text(n+" • "+val(d,"age")+" • "+s+"% compatible",20,true));
        card.addView(text("Location: "+val(d,"city")+", "+val(d,"country")+"\nMarriage: "+val(d,"marriageIntent")+"\nTimeline: "+val(d,"marriageTimeline")+"\nFamily/Wali: "+val(d,"familyInvolvement")+"\nVerification: "+val(d,"verificationStatus"),15,false));
        card.addView(text("Why we matched: reciprocal age/gender preferences + marriage intent + timeline + location/values + deal-breaker checks + trust signals.",14,false));
        Button view=button("View Profile",true),interest=button("Express Interest",false),report=danger("Report / Block");card.addView(view);card.addView(interest);card.addView(report);
        view.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(n).setMessage("Age: "+val(d,"age")+"\nLocation: "+val(d,"city")+", "+val(d,"country")+"\nMarriage intention: "+val(d,"marriageIntent")+"\nTimeline: "+val(d,"marriageTimeline")+"\nFamily/Wali: "+val(d,"familyInvolvement")+"\nVerification: "+val(d,"verificationStatus")+"\n\nAbout:\n"+val(d,"about")+"\n\nSafety: never send money, OTPs, passwords or unnecessary identity documents.").setPositiveButton("Close",null).show());
        interest.setOnClickListener(v->sendInterest(d.getId(),interest));report.setOnClickListener(v->safetyAction(d.getId()));
    }

    private void sendInterest(String uid,Button b){if(!signedIn())return;b.setEnabled(false);Map<String,Object> m=new HashMap<>();m.put("toUid",uid);functions.getHttpsCallable("sendInterest").call(m).addOnSuccessListener(x->{b.setText("Interest Sent ✓");toast("Interest sent. Chat remains locked until mutual acceptance.");}).addOnFailureListener(e->{b.setEnabled(true);toast("Interest could not be sent. It may already exist or the profile changed.");});}

    private void interests(){
        base();title("Mutual Interests & Safe Chat");root.addView(text("Communication is locked until an explicit mutual connection is created by the trusted backend.",15,false));String me=auth.getUid();section("Incoming Interests");
        db.collection("interests").whereEqualTo("toUid",me).whereEqualTo("status","pending").get().addOnSuccessListener(q->{if(q.isEmpty())root.addView(text("No pending interests.",15,false));for(DocumentSnapshot d:q){String id=d.getId(),from=val(d,"fromUid");Button a=button("Accept interest",true),r=button("Decline",false);root.addView(text("From: "+from,15,true));a.setOnClickListener(v->respond(id,"accepted"));r.setOnClickListener(v->respond(id,"declined"));}});
        section("Active Connections");db.collection("connections").whereEqualTo("uid1",me).whereEqualTo("status","active").get().addOnSuccessListener(this::connectionButtons);db.collection("connections").whereEqualTo("uid2",me).whereEqualTo("status","active").get().addOnSuccessListener(this::connectionButtons);
        Button b=button("Back",false);b.setOnClickListener(v->home());
    }
    private void respond(String id,String decision){Map<String,Object> m=new HashMap<>();m.put("interestId",id);m.put("decision",decision);functions.getHttpsCallable("respondToInterest").call(m).addOnSuccessListener(x->{toast("Interest updated. Mutual chat unlocks only after acceptance.");interests();}).addOnFailureListener(e->toast("Could not update interest."));}
    private void connectionButtons(QuerySnapshot q){for(DocumentSnapshot d:q){String other=auth.getUid().equals(val(d,"uid1"))?val(d,"uid2"):val(d,"uid1");Button b=button("Safe Chat • "+other,true);b.setOnClickListener(x->chat(d.getId(),other));}}

    private void chat(String cid,String other){
        base();title("Safe Chat");root.addView(text("Mutual connection only. Never send money, OTPs, passwords or private documents.",15,false));LinearLayout msgs=new LinearLayout(this);msgs.setOrientation(LinearLayout.VERTICAL);root.addView(msgs);EditText e=input("Respectful message (max 4000 characters)");Button send=button("Send",true),report=button("Report / Block",false),back=button("Back",false);
        db.collection("connections").document(cid).get().addOnSuccessListener(c->{if(!c.exists()||!"active".equals(val(c,"status"))){toast("This connection is no longer active.");interests();return;}db.collection("connections").document(cid).collection("messages").orderBy("sentAt",Query.Direction.ASCENDING).limitToLast(100).get().addOnSuccessListener(q->{for(DocumentSnapshot d:q)msgs.addView(text((auth.getUid().equals(val(d,"fromUid"))?"You: ":"Them: ")+val(d,"text"),15,false));});});
        send.setOnClickListener(x->{String msg=e.getText().toString().trim();if(msg.isEmpty()||msg.length()>4000){toast("Message must be 1–4000 characters.");return;}Map<String,Object> m=new HashMap<>();m.put("fromUid",auth.getUid());m.put("toUid",other);m.put("text",msg);m.put("sentAt",FieldValue.serverTimestamp());db.collection("connections").document(cid).collection("messages").add(m).addOnSuccessListener(y->{e.setText("");msgs.addView(text("You: "+msg,15,false));}).addOnFailureListener(y->toast("Message blocked by secure rules."));});
        report.setOnClickListener(x->safetyAction(other));back.setOnClickListener(x->interests());
    }

    private void safetyAction(String uid){new AlertDialog.Builder(this).setTitle("Safety action").setItems(new String[]{"Report user","Block user","Cancel"},(d,w)->{if(w==0)report(uid);else if(w==1)block(uid);}).show();}
    private void report(String uid){final EditText e=inputDialog("Reason for report");new AlertDialog.Builder(this).setTitle("Report user").setView(e).setPositiveButton("Send report",(d,w)->{String reason=e.getText().toString().trim();if(reason.isEmpty())reason="Safety concern";Map<String,Object> m=new HashMap<>();m.put("reporterUid",auth.getUid());m.put("reportedUid",uid);m.put("reason",reason);m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("reports").add(m).addOnSuccessListener(x->toast("Report sent to moderation.")).addOnFailureListener(x->toast("Report could not be sent."));}).setNegativeButton("Cancel",null).show();}
    private EditText inputDialog(String hint){EditText e=new EditText(this);e.setHint(hint);e.setPadding(20,10,20,10);return e;}
    private void block(String uid){Map<String,Object> m=new HashMap<>();m.put("blockedUid",uid);functions.getHttpsCallable("blockUser").call(m).addOnSuccessListener(x->{toast("User blocked and active connection closed.");home();}).addOnFailureListener(e->toast("Could not block this user."));}

    private void wali(){
        base();title("Family / Wali Connect");root.addView(text("Optional, consent-based family involvement. The other person must have a real Best Nikah Bridge account.",15,false));
        EditText e=input("Wali Firebase UID");Button b=button("Request Wali Connection",true),back=button("Back",false);b.setOnClickListener(x->{String uid=e.getText().toString().trim();if(uid.isEmpty()){e.setError("Enter Wali UID");return;}Map<String,Object> m=new HashMap<>();m.put("waliUid",uid);functions.getHttpsCallable("requestWaliConnection").call(m).addOnSuccessListener(y->toast("Wali request sent securely.")).addOnFailureListener(y->toast("Wali request failed. The Wali must have a real account."));});back.setOnClickListener(x->home());
    }

    private void safety(){base();title("Scam Shield & Safety");root.addView(text("BEST NIKAH BRIDGE SAFETY RULES\n\n• Never send money, gift cards or crypto.\n• Never share OTPs, passwords or recovery codes.\n• Do not send identity documents in chat.\n• Verification is a trust signal, not a guarantee.\n• Keep communication inside the app until trust is established.\n• Involve family/Wali for serious discussions.\n• Report pressure, threats, financial requests or suspicious behavior.\n• Blocking immediately closes an active connection.\n• For threats or emergencies, contact local authorities.",17,false));Button b=button("Back",false);b.setOnClickListener(v->home());}

    private void verification(){
        base();title("Real Verification");root.addView(text("Verification is not a decorative badge. Requests are stored in Firebase and only authorized administration can approve or reject them.",15,false));EditText note=input("Verification note (do not enter sensitive ID numbers)");Button b=button("Submit Verification Request",true),back=button("Back",false);
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->root.addView(text("Current status: "+val(d,"verificationStatus"),18,true)));
        b.setOnClickListener(x->{Map<String,Object> m=new HashMap<>();m.put("userUid",auth.getUid());m.put("note",note.getText().toString().trim());m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("verifications").add(m).addOnSuccessListener(y->toast("Verification request submitted securely.")).addOnFailureListener(y->toast("Verification request failed."));});back.setOnClickListener(x->home());
    }

    private void help(){
        base();title("AI Nikah Assistant");root.addView(text("Real Gemini-powered guidance through Firebase AI Logic. It helps with respectful compatibility questions, family conversations, boundaries, safety and marriage planning. It does not issue religious rulings or decide who you should marry.",15,false));
        EditText q=input("Ask your Nikah question...");Button ask=button("Ask AI Nikah Assistant",true),quick=button("Generate compatibility questions",false),back=button("Back",false);TextView answer=text("",16,false);root.addView(answer);
        ask.setOnClickListener(v->{String prompt=q.getText().toString().trim();if(prompt.length()<4){q.setError("Ask a clear question");return;}runAI(prompt,answer);});
        quick.setOnClickListener(v->runAI("Create 8 respectful, practical pre-Nikah compatibility questions covering deen, character, finances, family expectations, children, location, conflict resolution and marriage timeline. Avoid judging or issuing a religious ruling.",answer));
        back.setOnClickListener(v->home());
    }
    private void runAI(String prompt,TextView answer){
        answer.setText("AI is preparing a response…");
        aiExecutor.execute(()->{
            try{
                GenerativeModel ai=FirebaseAI.getInstance(GenerativeBackend.agentPlatform("global")).generativeModel("gemini-3.7-flash");
                GenerativeModelFutures model=GenerativeModelFutures.from(ai);
                Content content=new Content.Builder().addText("You are the Best Nikah Bridge assistant. Be respectful, safety-first, family-aware and concise. This is a Muslim matrimonial app, not a dating app. Do not give fatwas or pretend to be a scholar. Encourage qualified scholars for religious rulings. Never request passwords, OTPs or identity documents. User request: "+prompt).build();
                ListenableFuture<GenerateContentResponse> future=model.generateContent(content);
                GenerateContentResponse response=future.get();
                String out=response.getText();
                if(out==null||out.trim().isEmpty())throw new IllegalStateException("Empty AI response");
                runOnUiThread(()->answer.setText(out));
            }catch(Exception e){runOnUiThread(()->answer.setText("AI is temporarily unavailable. Safety guidance: keep communication respectful, involve family/Wali, verify important claims independently, never send money or OTPs, and consult a qualified scholar for religious rulings."));}
        });
    }

    private void privacy(){
        base();title("Privacy Control Center");root.addView(text("Control whether your profile is discoverable and what location detail is shown to other members. Changes are stored in your real Firebase profile.",15,false));
        CheckBox discover=new CheckBox(this),cityVisible=new CheckBox(this);discover.setText("Show my profile in matching");cityVisible.setText("Show my city on my public profile");root.addView(discover);root.addView(cityVisible);Button save=button("Save Privacy Controls",true),back=button("Back",false);
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->{discover.setChecked(Boolean.TRUE.equals(d.getBoolean("discoverable")));cityVisible.setChecked(!Boolean.FALSE.equals(d.getBoolean("showCity")));});
        save.setOnClickListener(v->{Map<String,Object> m=new HashMap<>();m.put("discoverable",discover.isChecked());m.put("showCity",cityVisible.isChecked());m.put("privacyUpdatedAt",FieldValue.serverTimestamp());db.collection("users").document(auth.getUid()).set(m,SetOptions.merge()).addOnSuccessListener(x->toast("Privacy controls saved.")).addOnFailureListener(x->toast("Privacy controls could not be saved."));});
        back.setOnClickListener(v->home());
    }

    private void account(){base();title("Account & Privacy");root.addView(text("Account deletion is permanent. The trusted Firebase backend removes your profile and related account records. Sign out keeps the account.",16,false));Button d=button("Permanently Delete Account",true),out=button("Sign out",false),b=button("Back",false);d.setOnClickListener(x->new AlertDialog.Builder(this).setTitle("Delete account permanently?").setMessage("This cannot be undone. Your profile, interests and related records will be removed by the trusted backend.").setPositiveButton("Delete",(z,w)->deleteAccount()).setNegativeButton("Cancel",null).show());out.setOnClickListener(x->{auth.signOut();authScreen();});b.setOnClickListener(x->home());}
    private void deleteAccount(){functions.getHttpsCallable("deleteMyAccount").call(new HashMap<>()).addOnSuccessListener(x->{auth.signOut();toast("Account deleted permanently.");authScreen();}).addOnFailureListener(x->toast("Deletion failed. Please retry when online."));}

    private void terms(){new AlertDialog.Builder(this).setTitle("Terms & Community Rules").setMessage("Best Nikah Bridge is for serious marriage intentions. No harassment, scams, financial requests, impersonation, explicit content or misleading profiles. Respect privacy, family involvement and consent. Verification does not guarantee a person's character or intentions. Use the report and block tools when needed.").setPositiveButton("Close",null).show();}
}
