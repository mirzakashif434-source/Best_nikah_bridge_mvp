package com.nikahbridge;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.*;

/** Production feature hub. Uses real Firebase data and never fabricates members, likes, photos or rewards. */
public class ProductionMainActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseFunctions fn;
    private FirebaseStorage storage;
    private LinearLayout root;
    private boolean urdu=false;
    private static final int PICK_PHOTO=901;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(90,105,100), light=Color.rgb(247,250,249);

    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();fn=FirebaseFunctions.getInstance();storage=FirebaseStorage.getInstance();if(auth.getCurrentUser()==null)login();else home();}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView t(String x,int z,boolean b){TextView v=new TextView(this);v.setText(x);v.setTextSize(z);v.setTextColor(b?dark:gray);v.setPadding(6,8,6,10);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private void title(String x){TextView v=t(x,27,true);v.setGravity(17);root.addView(v);}
    private Button bt(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,62);p.setMargins(0,5,0,5);root.addView(b,p);return b;}
    private EditText in(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);root.addView(e,new LinearLayout.LayoutParams(-1,62));return e;}
    private void toast(String x){Toast.makeText(this,x,Toast.LENGTH_LONG).show();}
    private String val(DocumentSnapshot d,String k){Object x=d.get(k);return x==null?"":String.valueOf(x);}

    private void login(){base();title(urdu?"بہترین نکاح برج":"Best Nikah Bridge");root.addView(t(urdu?"حقیقی Firebase اکاؤنٹ — سنجیدہ نکاح، dating نہیں":"Real Firebase account — serious Nikah, not dating.",16,false));EditText e=in("Email"),p=in("Password");Button s=bt(urdu?"سائن اِن":"Sign in",true);s.setOnClickListener(v->auth.signInWithEmailAndPassword(e.getText().toString().trim(),p.getText().toString()).addOnSuccessListener(x->home()).addOnFailureListener(x->toast("Sign in failed.")));}

    private void home(){
        base();title(urdu?"بہترین نکاح برج":"Best Nikah Bridge");root.addView(t(urdu?"تمام نئے production features حقیقی Firebase/AdMob services سے منسلک ہیں۔ کوئی demo data نہیں۔":"Production feature hub — real Firebase/AdMob services only. No demo data.",15,false));
        Button lang=bt(urdu?"English":"اردو / Urdu",false);lang.setOnClickListener(v->{urdu=!urdu;home();});
        Button core=bt(urdu?"اصل نکاح برج کھولیں":"Open Main Nikah Bridge",true);core.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));
        Button photo=bt(urdu?"حقیقی Profile Photo":"Real Profile Photo Upload",true);photo.setOnClickListener(v->photo());
        Button likes=bt(urdu?"روزانہ 20 Likes":"20 Daily Likes — Server Enforced",true);likes.setOnClickListener(v->likes());
        Button family=bt(urdu?"Family / Wali Bridge":"Family / Wali Bridge — Real",true);family.setOnClickListener(v->family());
        Button ads=bt(urdu?"2 Ads → 2 Messages":"2 Rewarded Ads → 2 Message Credits",true);ads.setOnClickListener(v->rewarded());
        Button admin=bt(urdu?"Admin Moderation":"Admin Moderation Dashboard",false);admin.setOnClickListener(v->admin());
        root.addView(t(urdu?"Privacy: صرف authenticated user اپنا data upload/change کر سکتا ہے۔ Admin actions Firebase custom claim سے محفوظ ہیں۔":"Privacy: only authenticated users can upload/change their own data. Admin actions require the Firebase custom claim admin=true.",14,false));
        Button out=bt(urdu?"Sign out":"Sign out",false);out.setOnClickListener(v->{auth.signOut();login();});
    }

    private void photo(){
        base();title("Real Profile Photo");root.addView(t("The selected image is uploaded to Firebase Storage under your authenticated UID and its real download URL is saved to your users document. No placeholder image is created.",15,false));
        Button pick=bt("Choose Photo",true);pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_PHOTO);});
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->{if(!val(d,"photoUrl").isEmpty())root.addView(t("Current profile photo: uploaded",15,true));});
        Button back=bt("Back",false);back.setOnClickListener(v->home());
    }
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=PICK_PHOTO||result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}StorageReference ref=storage.getReference().child("profilePhotos/"+auth.getUid()+"/profile.jpg");ref.putFile(uri).continueWithTask(t->{if(!t.isSuccessful()&&t.getException()!=null)throw t.getException();return ref.getDownloadUrl();}).addOnSuccessListener(url->{Map<String,Object> m=new HashMap<>();m.put("photoUrl",url.toString());m.put("photoUpdatedAt",FieldValue.serverTimestamp());db.collection("users").document(auth.getUid()).set(m,SetOptions.merge()).addOnSuccessListener(x->toast("Real profile photo uploaded successfully."));}).addOnFailureListener(e->toast("Photo upload failed. No fake photo was added."));}

    private void likes(){
        base();title("20 Daily Likes");root.addView(t("The Firebase callable function is authoritative: one authenticated active member can send at most 20 likes per UTC day. The counter cannot be reset by the client.",15,false));
        db.collection("users").whereEqualTo("profileActive",true).whereEqualTo("discoverable",true).limit(60).get().addOnSuccessListener(q->{for(DocumentSnapshot d:q){if(d.getId().equals(auth.getUid()))continue;LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.addView(t(val(d,"name")+" • "+val(d,"age")+" • "+val(d,"country"),18,true));Button b=bt("Like",true);card.addView(b);root.addView(card);b.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("toUid",d.getId());fn.getHttpsCallable("sendLike").call(x).addOnSuccessListener(r->{b.setEnabled(false);toast("Real like sent. Remaining today: "+String.valueOf(((Map)r.getData()).get("remaining")));}).addOnFailureListener(e->toast("Like blocked by the real 20/day safety limit."));}}).addOnFailureListener(e->toast("Could not load real profiles."));Button back=bt("Back",false);back.setOnClickListener(v->home());
    }

    private void family(){
        base();title("Family / Wali Bridge");root.addView(t("Family participation is tied to a real active mutual connection and real Firebase family bridge records. No fake Wali accounts are created.",15,false));
        EditText cid=in("Mutual connection ID"),wali=in("Family/Wali Firebase UID");Button create=bt("Create Real Family Bridge",true);create.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("connectionId",cid.getText().toString().trim());x.put("familyUid",wali.getText().toString().trim());fn.getHttpsCallable("createFamilyBridge").call(x).addOnSuccessListener(r->{String id=String.valueOf(((Map)r.getData()).get("bridgeId"));toast("Family Bridge created: "+id);}).addOnFailureListener(e->toast("Family Bridge rejected by backend safety rules."));});
        EditText bid=in("Existing Family Bridge ID"),q=in("Question for family/Wali");Button send=bt("Send Real Family Question",true);send.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("bridgeId",bid.getText().toString().trim());x.put("text",q.getText().toString().trim());fn.getHttpsCallable("sendFamilyQuestion").call(x).addOnSuccessListener(r->toast("Question sent securely." )).addOnFailureListener(e->toast("Question blocked by Family Bridge permissions."));});
        EditText consent=in("Bridge ID for consent");Button c=bt("Pause / Resume My Family Consent",false);c.setOnClickListener(v->{new AlertDialog.Builder(this).setTitle("Family consent").setMessage("Enable consent for this Family Bridge?").setPositiveButton("Enable",(d,w)->setConsent(consent.getText().toString(),true)).setNegativeButton("Pause",(d,w)->setConsent(consent.getText().toString(),false)).setNeutralButton("Cancel",null).show();});Button back=bt("Back",false);back.setOnClickListener(v->home());
    }
    private void setConsent(String id,boolean enabled){Map<String,Object>x=new HashMap<>();x.put("bridgeId",id.trim());x.put("enabled",enabled);fn.getHttpsCallable("setFamilyConsent").call(x).addOnSuccessListener(r->toast(enabled?"Family consent enabled.":"Family consent paused.")).addOnFailureListener(e->toast("Consent change rejected."));}

    private void rewarded(){
        base();title("2 Rewarded Ads → 2 Message Credits");root.addView(t("Only a completed production AdMob rewarded ad can request one real message credit. The server limits rewarded claims to two per UTC day. No demo ad ID and no fake reward are used.",15,false));TextView status=t("Checking production AdMob configuration…",15,false);root.addView(status);int id=getResources().getIdentifier("admob_rewarded_unit_id","string",getPackageName());String unit="";if(id!=0)unit=getString(id);if(unit.trim().isEmpty()){status.setText("AdMob production rewarded unit is not configured. No reward is granted. Add the real AdMob rewarded unit ID before monetization.");}else{MobileAds.initialize(this,s->loadReward(unit,status));}Button back=bt("Back",false);back.setOnClickListener(v->home());
    }
    private void loadReward(String unit,TextView status){RewardedAd.load(this,unit,new AdRequest.Builder().build(),new RewardedAdLoadCallback(){@Override public void onAdLoaded(RewardedAd ad){status.setText("Production rewarded ad ready. Watch it completely to claim one real credit.");ad.show(ProductionMainActivity.this,(RewardItem item)->claimReward());}@Override public void onAdFailedToLoad(LoadAdError e){status.setText("Production AdMob unavailable. No fake reward was granted.");}});}
    private void claimReward(){Map<String,Object>x=new HashMap<>();x.put("source","rewarded_ad");fn.getHttpsCallable("claimRewardedMessageCredit").call(x).addOnSuccessListener(r->toast("One real message credit granted.")).addOnFailureListener(e->toast("Reward could not be verified; no credit granted."));}

    private void admin(){
        base();title("Admin Moderation Dashboard");root.addView(t("Only Firebase Auth users with custom claim admin=true can change moderation or verification state. Normal members receive permission-denied from the server.",15,false));
        db.collection("moderationQueue").whereEqualTo("status","pending").limit(50).get().addOnSuccessListener(q->{root.addView(t("Pending reports: "+q.size(),18,true));for(DocumentSnapshot d:q){root.addView(t(d.getId()+" • "+val(d,"reason"),16,true));Button b=bt("Mark Reviewed",false);b.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("reportId",d.getId());x.put("decision","reviewed");fn.getHttpsCallable("setModerationDecision").call(x).addOnSuccessListener(r->{b.setEnabled(false);toast("Moderation decision saved securely.");}).addOnFailureListener(e->toast("Admin permission required."));});}}).addOnFailureListener(e->toast("Admin access required or queue unavailable."));
        db.collection("verifications").whereEqualTo("status","pending").limit(50).get().addOnSuccessListener(q->{root.addView(t("Pending verification requests: "+q.size(),18,true));for(DocumentSnapshot d:q){String uid=val(d,"userUid");root.addView(t("Verification: "+uid,16,true));Button b=bt("Approve Verified",false);b.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("userUid",uid);x.put("status","verified");fn.getHttpsCallable("setVerificationStatus").call(x).addOnSuccessListener(r->{b.setEnabled(false);toast("Verification status saved securely.");}).addOnFailureListener(e->toast("Admin permission required."));});}});
        Button back=bt("Back",false);back.setOnClickListener(v->home());
    }
}
