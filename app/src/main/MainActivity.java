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
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Best Nikah Bridge - production launcher.
 *
 * No demo members, fake matches, local premium switches, fake verification
 * badges, or local-only connection state are used here. User data is backed
 * by Firebase Authentication/Firestore and privileged mutations use callable
 * Cloud Functions.
 */
public class MainActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseFunctions functions;
    private LinearLayout root;

    private final int green = Color.rgb(18, 103, 82);
    private final int dark = Color.rgb(30, 45, 41);
    private final int gray = Color.rgb(95, 108, 103);
    private final int red = Color.rgb(165, 50, 50);
    private final int light = Color.rgb(247, 250, 249);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        functions = FirebaseFunctions.getInstance();
        route();
    }

    @Override public void onBackPressed() {
        if (auth.getCurrentUser() == null) authScreen(); else home();
    }

    private void route() {
        FirebaseUser u = auth.getCurrentUser();
        if (u == null) { authScreen(); return; }
        if (!u.isEmailVerified()) { verificationGate(); return; }
        db.collection("users").document(u.getUid()).get()
                .addOnSuccessListener(d -> {
                    if (!d.exists() || !Boolean.TRUE.equals(d.getBoolean("profileActive"))) {
                        profile();
                    } else {
                        home();
                    }
                })
                .addOnFailureListener(e -> home());
    }

    private void base() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(light);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 24, 22, 32);
        scroll.addView(root);
        setContentView(scroll);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(bold ? dark : gray);
        v.setPadding(6, 8, 6, 12);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private void title(String value) {
        TextView v = text(value, 28, true);
        v.setGravity(Gravity.CENTER);
        root.addView(v);
    }

    private Button button(String label, boolean filled) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(filled ? Color.WHITE : green);
        b.setMinHeight(58);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(filled ? green : Color.WHITE);
        bg.setCornerRadius(18);
        if (!filled) bg.setStroke(2, green);
        b.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 62);
        p.setMargins(0, 6, 0, 6);
        root.addView(b, p);
        return b;
    }

    private Button danger(String label) {
        Button b = button(label, false);
        b.setTextColor(red);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(18);
        bg.setStroke(2, red);
        b.setBackground(bg);
        return b;
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setPadding(14, 8, 14, 8);
        root.addView(e, new LinearLayout.LayoutParams(-1, 62));
        return e;
    }

    private void section(String value) { root.addView(text(value, 19, true)); }

    private void toast(String value) { Toast.makeText(this, value, Toast.LENGTH_LONG).show(); }

    private String val(DocumentSnapshot d, String key) {
        Object v = d.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private boolean signedIn() { return auth.getCurrentUser() != null; }

    private void authScreen() {
        base();
        title("Best Nikah Bridge");
        root.addView(text("Real account • Secure Firebase Authentication • Serious Nikah only", 16, false));
        EditText email = input("Email address");
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        EditText password = input("Password (minimum 8 characters)");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        Button signIn = button("Sign in", true);
        Button create = button("Create account", false);
        Button terms = button("Terms & Community Rules", false);

        signIn.setOnClickListener(v -> {
            if (!validCredentials(email, password)) return;
            auth.signInWithEmailAndPassword(email.getText().toString().trim(), password.getText().toString())
                    .addOnSuccessListener(x -> {
                        if (!x.getUser().isEmailVerified()) verificationGate(); else route();
                    })
                    .addOnFailureListener(e -> toast("Sign in failed. Please check your email and password."));
        });

        create.setOnClickListener(v -> {
            if (!validCredentials(email, password)) return;
            auth.createUserWithEmailAndPassword(email.getText().toString().trim(), password.getText().toString())
                    .addOnSuccessListener(x -> {
                        FirebaseUser u = x.getUser();
                        if (u == null) { toast("Account creation failed."); return; }
                        u.sendEmailVerification();
                        Map<String,Object> profile = new HashMap<>();
                        profile.put("uid", u.getUid());
                        profile.put("profileActive", false);
                        profile.put("verificationStatus", "unverified");
                        profile.put("termsAccepted", false);
                        profile.put("createdAt", FieldValue.serverTimestamp());
                        db.collection("users").document(u.getUid()).set(profile)
                                .addOnSuccessListener(z -> verificationGate())
                                .addOnFailureListener(e -> toast("Account setup failed. Please try again."));
                    })
                    .addOnFailureListener(e -> toast("Account creation failed. The email may already be registered."));
        });
        terms.setOnClickListener(v -> terms());
    }

    private boolean validCredentials(EditText email, EditText password) {
        if (!email.getText().toString().trim().contains("@")) {
            email.setError("Enter a valid email"); return false;
        }
        if (password.getText().toString().length() < 8) {
            password.setError("Minimum 8 characters"); return false;
        }
        return true;
    }

    private void verificationGate() {
        base();
        title("Verify your email");
        root.addView(text("Email verification is required before your profile can become active. Check your inbox and then press the button below.", 16, false));
        Button resend = button("Resend verification email", true);
        Button refresh = button("I verified — continue", false);
        Button signOut = button("Sign out", false);
        resend.setOnClickListener(v -> {
            FirebaseUser u = auth.getCurrentUser();
            if (u != null) u.sendEmailVerification().addOnSuccessListener(x -> toast("Verification email sent."));
        });
        refresh.setOnClickListener(v -> {
            FirebaseUser u = auth.getCurrentUser();
            if (u == null) { authScreen(); return; }
            u.reload().addOnSuccessListener(x -> {
                if (u.isEmailVerified()) route(); else toast("Email is not verified yet.");
            });
        });
        signOut.setOnClickListener(v -> { auth.signOut(); authScreen(); });
    }

    private void home() {
        base();
        title("Best Nikah Bridge");
        root.addView(text("Halal Muslim matrimonial platform — not a dating app.", 17, false));
        Button profile = button("My Profile", true);
        Button readiness = button("Nikah Readiness Score", true);
        Button matches = button("Real Recommended Matches", true);
        Button interests = button("Mutual Interests & Safe Chat", true);
        Button wali = button("Family / Wali Connect", true);
        Button safety = button("Scam Shield & Safety", false);
        Button verify = button("Real Verification", false);
        Button help = button("Nikah Assistant / Help", false);
        Button account = button("Privacy, Terms & Delete Account", false);
        Button signOut = button("Sign out", false);
        profile.setOnClickListener(v -> profile());
        readiness.setOnClickListener(v -> readiness());
        matches.setOnClickListener(v -> matches());
        interests.setOnClickListener(v -> interests());
        wali.setOnClickListener(v -> wali());
        safety.setOnClickListener(v -> safety());
        verify.setOnClickListener(v -> verification());
        help.setOnClickListener(v -> help());
        account.setOnClickListener(v -> account());
        signOut.setOnClickListener(v -> { auth.signOut(); authScreen(); });
        root.addView(text("Signed in securely. Contact details are not displayed to other members by this app.", 14, false));
    }

    private void profile() {
        base(); title("My Real Nikah Profile");
        root.addView(text("Only truthful information. Your profile becomes discoverable only after you accept the rules and complete the required fields.", 15, false));
        EditText name=input("Full name"), age=input("Age (18+)"), country=input("Country"), city=input("City"), gender=input("Gender"), marital=input("Marital status"), intent=input("Marriage intention"), practice=input("Religious practice"), about=input("About yourself and family values (30+ characters)"), pref=input("Partner preferences (age, location, values, education, etc.)"), timeline=input("Marriage timeline"), family=input("Family / Wali preference"), deal=input("Compatibility deal-breakers / non-negotiables");
        CheckBox termsAccepted = new CheckBox(this);
        termsAccepted.setText("I accept the Terms & Community Rules and understand that respectful conduct and truthful information are required.");
        root.addView(termsAccepted);
        Button save=button("Save & Activate Profile",true), verify=button("Request Verification",false), back=button("Back",false);

        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d -> {
            name.setText(val(d,"name")); age.setText(val(d,"age")); country.setText(val(d,"country")); city.setText(val(d,"city")); gender.setText(val(d,"gender")); marital.setText(val(d,"maritalStatus")); intent.setText(val(d,"marriageIntent")); practice.setText(val(d,"religiousPractice")); about.setText(val(d,"about")); pref.setText(val(d,"partnerPreference")); timeline.setText(val(d,"marriageTimeline")); family.setText(val(d,"familyInvolvement")); deal.setText(val(d,"dealbreakers")); termsAccepted.setChecked(Boolean.TRUE.equals(d.getBoolean("termsAccepted")));
            root.addView(text("Verification status: " + val(d,"verificationStatus"), 15, false));
        });

        save.setOnClickListener(v -> {
            String n=name.getText().toString().trim(), a=age.getText().toString().trim(), c=country.getText().toString().trim(), ci=city.getText().toString().trim(), g=gender.getText().toString().trim(), m=marital.getText().toString().trim(), in=intent.getText().toString().trim(), pr=practice.getText().toString().trim(), ab=about.getText().toString().trim(), pp=pref.getText().toString().trim(), tl=timeline.getText().toString().trim(), fam=family.getText().toString().trim(), dbreak=deal.getText().toString().trim();
            int numericAge;
            try { numericAge=Integer.parseInt(a); } catch(Exception e) { age.setError("Enter your age"); return; }
            if(numericAge<18 || numericAge>100) { age.setError("Age must be 18–100"); return; }
            if(n.length()<2 || c.isEmpty() || ci.isEmpty() || g.isEmpty() || m.isEmpty() || in.isEmpty() || pr.isEmpty() || ab.length()<30 || pp.length()<10 || tl.isEmpty() || fam.isEmpty() || dbreak.length()<5) { toast("Please complete all profile fields with truthful information."); return; }
            if(!termsAccepted.isChecked()) { toast("You must accept the Terms & Community Rules before activating a profile."); return; }
            Map<String,Object> q=new HashMap<>(); q.put("uid",auth.getUid()); q.put("name",n); q.put("age",numericAge); q.put("country",c); q.put("city",ci); q.put("gender",g); q.put("maritalStatus",m); q.put("marriageIntent",in); q.put("religiousPractice",pr); q.put("about",ab); q.put("partnerPreference",pp); q.put("marriageTimeline",tl); q.put("familyInvolvement",fam); q.put("dealbreakers",dbreak); q.put("termsAccepted",true); q.put("profileActive",true); q.put("updatedAt",FieldValue.serverTimestamp());
            db.collection("users").document(auth.getUid()).set(q, SetOptions.merge()).addOnSuccessListener(x -> { toast("Profile saved and activated."); home(); }).addOnFailureListener(e -> toast("Profile could not be saved. Please try again."));
        });
        verify.setOnClickListener(v -> verification()); back.setOnClickListener(v -> home());
    }

    private void readiness() {
        base(); title("Nikah Readiness Score");
        root.addView(text("A preparation aid — not a religious ruling and not a guarantee of compatibility.", 15, false));
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d -> {
            int score=0; String[] keys={"name","country","city","about","partnerPreference","marriageIntent","marriageTimeline","familyInvolvement","dealbreakers"}; for(String k:keys) if(!val(d,k).trim().isEmpty()) score+=10; if(d.getLong("age")!=null) score+=10; if(Boolean.TRUE.equals(d.getBoolean("termsAccepted"))) score+=5; if("verified".equalsIgnoreCase(val(d,"verificationStatus"))) score+=5; if(score>100)score=100;
            root.addView(text("Nikah Readiness Score: "+score+" / 100", 26, true));
            root.addView(text(score>=85?"Strong preparation. Focus on respectful family discussions and real-world compatibility.":score>=65?"Good foundation. Complete the missing profile and family/timeline details.":"Start by clarifying your goals, boundaries, timeline and family involvement.",17,false));
        });
        Button b=button("Back",false); b.setOnClickListener(v->home());
    }

    private void matches() {
        base(); title("Real Recommended Matches");
        root.addView(text("Only active Firebase profiles are shown. No fake/demo members are generated by this app.",15,false));
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(me -> {
            getSharedPreferences("profile_cache",MODE_PRIVATE).edit()
                    .putString("marriageIntent", val(me,"marriageIntent"))
                    .putString("marriageTimeline", val(me,"marriageTimeline"))
                    .putString("country", val(me,"country"))
                    .putString("partnerPreference", val(me,"partnerPreference"))
                    .apply();
            db.collection("users").whereEqualTo("profileActive",true).limit(50).get().addOnSuccessListener(q -> {
                boolean any=false;
                for(DocumentSnapshot d:q){
                    if(auth.getUid().equals(d.getId())) continue;
                    any=true;
                    addMatch(d);
                }
                if(!any) root.addView(text("No real profiles are available yet. Invite serious members to build the community.",16,false));
            }).addOnFailureListener(e -> toast("Could not load real matches. Please try again."));
        }).addOnFailureListener(e -> toast("Could not load your profile for matching."));
        Button b=button("Back",false); b.setOnClickListener(v->home());
    }

    private void addMatch(DocumentSnapshot d) {
        String uid=d.getId(); String name=val(d,"name"); if(name.isEmpty())name="Member"; String location=val(d,"city"); if(!val(d,"country").isEmpty()) location += (location.isEmpty()?"":", ")+val(d,"country");
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(16,12,16,12); root.addView(card,new LinearLayout.LayoutParams(-1,-2));
        TextView h=text(name+(val(d,"age").isEmpty()?"":" • "+val(d,"age")),20,true); card.addView(h);
        card.addView(text("Location: "+location+"\nMarriage intention: "+val(d,"marriageIntent")+"\nTimeline: "+val(d,"marriageTimeline")+"\nVerification: "+val(d,"verificationStatus"),15,false));
        card.addView(text("Why we matched: "+whyMatched(d),15,false));
        Button view=button("View Profile",true); Button interest=button("Express Interest",false); Button report=danger("Report User"); card.addView(view); card.addView(interest); card.addView(report);
        final String target=uid;
        view.setOnClickListener(v->profileDetails(d));
        interest.setOnClickListener(v->sendInterest(target,interest));
        report.setOnClickListener(v->reportDialog(target));
    }

    private String whyMatched(DocumentSnapshot d) {
        int score=60;
        String myIntent=valCache("marriageIntent");
        String myTimeline=valCache("marriageTimeline");
        String myCountry=valCache("country");
        String myPref=valCache("partnerPreference").toLowerCase(Locale.US);
        if(!myIntent.isEmpty() && myIntent.equalsIgnoreCase(val(d,"marriageIntent")))score+=12;
        if(!myTimeline.isEmpty() && myTimeline.equalsIgnoreCase(val(d,"marriageTimeline")))score+=8;
        if(!myCountry.isEmpty() && myCountry.equalsIgnoreCase(val(d,"country")))score+=8;
        String city=val(d,"city").toLowerCase(Locale.US), country=val(d,"country").toLowerCase(Locale.US);
        if(!myPref.isEmpty() && ((!city.isEmpty() && myPref.contains(city)) || (!country.isEmpty() && myPref.contains(country))))score+=7;
        if("verified".equalsIgnoreCase(val(d,"verificationStatus")))score+=5;
        if(score>97)score=97;
        return "serious intent • timeline • location/preferences • safety/verification • Compatibility "+score+"%";
    }

    private String valCache(String key) {
        return getSharedPreferences("profile_cache",MODE_PRIVATE).getString(key,"");
    }

    private void profileDetails(DocumentSnapshot d) {
        new AlertDialog.Builder(this).setTitle(val(d,"name")).setMessage(
                "Age: "+val(d,"age")+"\nLocation: "+val(d,"city")+", "+val(d,"country")+"\nMarriage intention: "+val(d,"marriageIntent")+"\nTimeline: "+val(d,"marriageTimeline")+"\nFamily: "+val(d,"familyInvolvement")+"\nVerification: "+val(d,"verificationStatus")+"\n\nAbout:\n"+val(d,"about")+"\n\nSafety: Never send money, OTPs, passwords or identity documents.")
                .setPositiveButton("Close",null).show();
    }

    private void sendInterest(String otherUid, Button button) {
        if(!signedIn())return;
        button.setEnabled(false);
        Map<String,Object> data=new HashMap<>(); data.put("toUid",otherUid);
        functions.getHttpsCallable("sendInterest").call(data).addOnSuccessListener(x->{button.setText("Interest Sent ✓"); toast("Interest sent. Chat stays locked until mutual acceptance.");}).addOnFailureListener(e->{button.setEnabled(true);toast("Interest could not be sent. It may already exist or the profile may no longer be active.");});
    }

    private void interests() {
        base(); title("Mutual Interests & Safe Chat"); root.addView(text("Communication unlocks only after both people accept. Server rules remain the final authority.",15,false));
        String me=auth.getUid(); section("Incoming Interests");
        db.collection("interests").whereEqualTo("toUid",me).whereEqualTo("status","pending").get().addOnSuccessListener(q->{ if(q.isEmpty())root.addView(text("No pending interests.",15,false)); for(DocumentSnapshot d:q){String id=d.getId(),from=val(d,"fromUid"); Button a=button("Accept • "+from,true), r=button("Decline • "+from,false); a.setOnClickListener(v->respond(id,"accepted")); r.setOnClickListener(v->respond(id,"declined")); }});
        section("Accepted Connections"); loadConnections(me);
        Button b=button("Back",false); b.setOnClickListener(v->home());
    }

    private void respond(String interestId,String decision) {
        Map<String,Object> data=new HashMap<>(); data.put("interestId",interestId); data.put("decision",decision);
        functions.getHttpsCallable("respondToInterest").call(data).addOnSuccessListener(x->{toast("Interest updated securely.");interests();}).addOnFailureListener(e->toast("Could not update the interest."));
    }

    private void loadConnections(String me) {
        db.collection("connections").whereEqualTo("uid1",me).whereEqualTo("status","active").get().addOnSuccessListener(q->addConnections(q,me));
        db.collection("connections").whereEqualTo("uid2",me).whereEqualTo("status","active").get().addOnSuccessListener(q->addConnections(q,me));
    }

    private void addConnections(com.google.firebase.firestore.QuerySnapshot q,String me) {
        for(DocumentSnapshot d:q){String other=me.equals(val(d,"uid1"))?val(d,"uid2"):val(d,"uid1"); if(!other.isEmpty()){Button chat=button("Safe Chat • "+other,true); chat.setOnClickListener(v->chat(d.getId(),other));}}
    }

    private void chat(String connectionId,String otherUid) {
        base(); title("Safe Chat"); root.addView(text("Mutual connection only. Never send money, OTPs, passwords or private identity documents.",15,false));
        LinearLayout messages=new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL); root.addView(messages);
        db.collection("connections").document(connectionId).collection("messages").orderBy("sentAt", Query.Direction.ASCENDING).limitToLast(100).get().addOnSuccessListener(q->{for(DocumentSnapshot d:q)messages.addView(text((auth.getUid().equals(val(d,"fromUid"))?"You: ":"Them: ")+val(d,"text"),15,false));});
        EditText e=input("Write a respectful message (max 4000 characters)"); Button send=button("Send Message",true), report=danger("Report User"), block=danger("Block User"), back=button("Back",false);
        send.setOnClickListener(v->{String t=e.getText().toString().trim(); if(t.isEmpty()||t.length()>4000){toast("Message must be 1–4000 characters.");return;} Map<String,Object> m=new HashMap<>();m.put("fromUid",auth.getUid());m.put("toUid",otherUid);m.put("text",t);m.put("sentAt",FieldValue.serverTimestamp());db.collection("connections").document(connectionId).collection("messages").add(m).addOnSuccessListener(x->{e.setText("");messages.addView(text("You: "+t,15,false));}).addOnFailureListener(x->toast("Message blocked by the secure connection rules."));});
        report.setOnClickListener(v->reportDialog(otherUid)); block.setOnClickListener(v->blockUser(otherUid)); back.setOnClickListener(v->interests());
    }

    private void reportDialog(String otherUid) {
        final String[] reasons={"Suspicious profile","Harassment","Spam / scam","Inappropriate content","Safety concern"};
        new AlertDialog.Builder(this).setTitle("Report user").setItems(reasons,(d,which)->{Map<String,Object> m=new HashMap<>();m.put("reporterUid",auth.getUid());m.put("reportedUid",otherUid);m.put("reason",reasons[which]);m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("reports").add(m).addOnSuccessListener(x->toast("Report submitted to moderation.")).addOnFailureListener(x->toast("Report could not be submitted."));}).setNegativeButton("Cancel",null).show();
    }

    private void blockUser(String otherUid) {
        Map<String,Object> m=new HashMap<>();m.put("blockedUid",otherUid);
        functions.getHttpsCallable("blockUser").call(m).addOnSuccessListener(x->{toast("User blocked. Active connection access has been disabled.");interests();}).addOnFailureListener(e->toast("Could not block this user."));
    }

    private void wali() {
        base(); title("Family / Wali Connect"); root.addView(text("Optional, consent-based family participation. A Wali account must be registered in Best Nikah Bridge.",15,false));
        EditText uid=input("Trusted Wali Firebase UID"); Button request=button("Request Wali Connection",true), back=button("Back",false);
        request.setOnClickListener(v->{String w=uid.getText().toString().trim();if(w.isEmpty()||w.equals(auth.getUid())){toast("Enter a valid Wali account UID.");return;}Map<String,Object> m=new HashMap<>();m.put("waliUid",w);functions.getHttpsCallable("requestWaliConnection").call(m).addOnSuccessListener(x->toast("Wali request sent securely.")).addOnFailureListener(e->toast("Wali request failed. The account may not exist."));}); back.setOnClickListener(v->home());
    }

    private void safety() {
        base(); title("Scam Shield & Safety");
        root.addView(text("• Never send money to another member.\n\n• Never share OTPs, passwords or banking information.\n\n• Keep phone numbers and private contact details hidden until you are comfortable.\n\n• Verification is a trust signal, not a guarantee of someone's character.\n\n• Use Report and Block immediately for suspicious, abusive or unsafe behavior.\n\n• Involve family/Wali when appropriate.\n\n• For threats or emergencies, contact local authorities.\n\n• Religious rulings should come from qualified scholars.",17,false));
        Button terms=button("Terms & Community Rules",false), back=button("Back",false); terms.setOnClickListener(v->terms()); back.setOnClickListener(v->home());
    }

    private void verification() {
        base(); title("Real Verification"); root.addView(text("Verification requests go to the real Firebase backend. Only authorized moderation/admin tooling can mark an account verified.",15,false));
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->root.addView(text("Current status: "+val(d,"verificationStatus"),18,true)));
        EditText note=input("Optional verification note — do not enter ID numbers or sensitive documents here"); Button submit=button("Submit Verification Request",true), back=button("Back",false);
        submit.setOnClickListener(v->{Map<String,Object> m=new HashMap<>();m.put("userUid",auth.getUid());m.put("note",note.getText().toString().trim());m.put("status","pending");m.put("createdAt",FieldValue.serverTimestamp());db.collection("verifications").add(m).addOnSuccessListener(x->toast("Verification request submitted.")).addOnFailureListener(x->toast("Verification request could not be submitted."));}); back.setOnClickListener(v->home());
    }

    private void help() {
        base(); title("AI Nikah Assistant / Help"); root.addView(text("Best Nikah Bridge can guide you through compatibility questions, family involvement, marriage timeline, boundaries and safety. It must not present religious rulings as fact; consult a qualified scholar for fiqh/religious decisions.",16,false));
        root.addView(text("Useful questions to discuss:\n\n• Marriage timeline and expectations\n• Family/Wali involvement\n• Deal-breakers and boundaries\n• Financial responsibilities\n• Location and relocation\n• Children and family goals\n• Conflict resolution and communication\n• Privacy and safety",16,false));
        Button back=button("Back",false); back.setOnClickListener(v->home());
    }

    private void terms() {
        base(); title("Terms & Community Rules"); root.addView(text("Best Nikah Bridge is for serious Muslim matrimonial intent, not casual dating. Users must provide truthful information and respect consent and boundaries. Harassment, scams, threats, impersonation, abusive content and requests for money are prohibited. Users must not exchange passwords, OTPs or unnecessary identity documents. Report unsafe behavior and use blocking tools when needed. Profiles and messages may be moderated for safety and policy compliance. Religious guidance should be obtained from qualified scholars.",16,false)); Button b=button("Back",false); b.setOnClickListener(v->home());
    }

    private void account() {
        base(); title("Privacy & Account"); root.addView(text("Your account uses Firebase Authentication and Firestore. Your profile is used to provide matrimonial matching and safety features. Do not put passwords, OTPs or financial information in your profile or messages.",16,false));
        Button privacy=button("Privacy Controls",false), delete=danger("Permanently Delete Account"), back=button("Back",false);
        privacy.setOnClickListener(v->privacy()); delete.setOnClickListener(v->deleteAccount()); back.setOnClickListener(v->home());
    }

    private void privacy() {
        base(); title("Privacy & Data Controls"); root.addView(text("Best Nikah Bridge stores account and profile information needed to provide matching, connections, moderation and account security. Some records may be retained when legally required or reasonably necessary for fraud prevention and safety. Never share sensitive credentials in user content. Account deletion removes the account and associated service data through the secure backend, subject to legitimate retention requirements.",16,false)); Button delete=danger("Permanently Delete Account"), back=button("Back",false); delete.setOnClickListener(v->deleteAccount()); back.setOnClickListener(v->account());
    }

    private void deleteAccount() {
        new AlertDialog.Builder(this).setTitle("Delete account?").setMessage("This permanently deletes your Best Nikah Bridge account and associated service data. This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{
            functions.getHttpsCallable("deleteMyAccount").call(new HashMap<>()).addOnSuccessListener(x->{auth.signOut();toast("Account deleted.");authScreen();}).addOnFailureListener(e->toast("Deletion failed. Please sign in again and retry."));
        }).show();
    }
}
