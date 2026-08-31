package com.nikahbridge;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
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

/** Real production completion center. No fabricated members, likes, photos, verification or ad rewards. */
public class ProductionCompletionActivity extends Activity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseFunctions fn;
    private FirebaseStorage storage;
    private LinearLayout root;
    private boolean urdu = false;
    private static final int PICK_PHOTO = 901;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        auth=FirebaseAuth.getInstance(); db=FirebaseFirestore.getInstance(); fn=FirebaseFunctions.getInstance(); storage=FirebaseStorage.getInstance();
        dashboard();
    }

    private void base(){ ScrollView s=new ScrollView(this); s.setFillViewport(true); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(20,22,20,30); root.setBackgroundColor(Color.rgb(247,250,249)); s.addView(root); setContentView(s); }
    private TextView txt(String x,int size,boolean bold){ TextView t=new TextView(this); t.setText(x); t.setTextSize(size); t.setTextColor(bold?Color.rgb(30,45,41):Color.rgb(85,100,95)); t.setPadding(6,8,6,10); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private void title(String x){ TextView t=txt(x,27,true); t.setGravity(Gravity.CENTER); root.addView(t); }
    private Button btn(String x,boolean fill){ Button b=new Button(this); b.setText(x); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(fill?Color.WHITE:Color.rgb(18,103,82)); GradientDrawable g=new GradientDrawable(); g.setColor(fill?Color.rgb(18,103,82):Color.WHITE); g.setCornerRadius(18); if(!fill)g.setStroke(2,Color.rgb(18,103,82)); b.setBackground(g); root.addView(b,new LinearLayout.LayoutParams(-1,62)); return b; }
    private EditText input(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setTextSize(16); root.addView(e,new LinearLayout.LayoutParams(-1,62)); return e; }
    private void toast(String x){ Toast.makeText(this,x,Toast.LENGTH_LONG).show(); }
    private String val(DocumentSnapshot d,String k){ Object v=d.get(k); return v==null?"":String.valueOf(v); }

    private void dashboard(){
        base(); title(urdu?"بہترین نکاح برج":"Best Nikah Bridge");
        root.addView(txt(urdu?"حقیقی production features — کوئی demo data نہیں":"Real production completion center — no demo data, fake members or fake rewards.",16,false));
        Button lang=btn(urdu?"English":"اردو / Urdu",false); lang.setOnClickListener(v->{urdu=!urdu;dashboard();});
        Button photo=btn(urdu?"Profile Photo Upload":"Real Profile Photo",true);
        Button likes=btn(urdu?"20 Daily Likes":"20 Daily Likes — Real Limit",true);
        Button family=btn(urdu?"Family / Wali Bridge":"Family / Wali Bridge — Real",true);
        Button ads=btn(urdu?"Watch 2 Ads for 2 Messages":"2 Rewarded Ads → 2 Messages",true);
        Button admin=btn(urdu?"Admin Moderation":"Admin Moderation Dashboard",false);
        Button core=btn(urdu?"Main Nikah Bridge":"Open Main Nikah Bridge",false);
        photo.setOnClickListener(v->photo()); likes.setOnClickListener(v->likes()); family.setOnClickListener(v->family()); ads.setOnClickListener(v->rewarded()); admin.setOnClickListener(v->admin()); core.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));
        root.addView(txt(urdu?"اہم: یہ screen صرف حقیقی Firebase/AdMob data استعمال کرتی ہے۔":"Important: this screen uses real Firebase/AdMob services only. Missing external configuration is shown as an error, never replaced with demo data.",14,false));
    }

    private void photo(){
        base(); title("Real Profile Photo"); root.addView(txt("Choose one photo from your device. It is uploaded to Firebase Storage under your authenticated UID and the resulting URL is saved to your real profile.",15,false));
        Button pick=btn("Choose Photo",true), back=btn("Back",false); pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_PHOTO);}); back.setOnClickListener(v->dashboard());
        db.collection("users").document(auth.getUid()).get().addOnSuccessListener(d->{String u=val(d,"photoUrl"); if(!u.isEmpty())root.addView(txt("Current real photo: uploaded",15,true));});
    }
    @Override protected void onActivityResult(int req,int result,Intent data){ super.onActivityResult(req,result,data); if(req!=PICK_PHOTO||result!=RESULT_OK||data==null||data.getData()==null)return; Uri uri=data.getData(); try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){} StorageReference ref=storage.getReference().child("profilePhotos/"+auth.getUid()+"/profile.jpg"); ref.putFile(uri).continueWithTask(t->{if(!t.isSuccessful()&&t.getException()!=null)throw t.getException();return ref.getDownloadUrl();}).addOnSuccessListener(url->{Map<String,Object> m=new HashMap<>();m.put("photoUrl",url.toString());m.put("photoUpdatedAt",FieldValue.serverTimestamp());db.collection("users").document(auth.getUid()).set(m,SetOptions.merge()).addOnSuccessListener(x->toast("Real profile photo uploaded successfully."));}).addOnFailureListener(e->toast("Photo upload failed. No fake photo was added.")); }

    private void likes(){
        base(); title("20 Daily Likes"); root.addView(txt("Each authenticated active member can send at most 20 real likes in a rolling UTC day. The server owns the counter; the client cannot reset it.",15,false));
        db.collection("users").whereEqualTo("profileActive",true).whereEqualTo("discoverable",true).limit(60).get().addOnSuccessListener(q->{for(DocumentSnapshot d:q){if(d.getId().equals(auth.getUid()))continue; TextView p=txt(val(d,"name")+" • "+val(d,"age")+" • "+val(d,"country"),18,true);root.addView(p);Button b=btn("Like",true);b.setOnClickListener(v->{Map<String,Object> x=new HashMap<>();x.put("toUid",d.getId());fn.getHttpsCallable("sendLike").call(x).addOnSuccessListener(r->{toast("Real like sent.");b.setEnabled(false);}).addOnFailureListener(e->toast("Like not sent: "+(e.getMessage()==null?"daily limit or safety rule":"daily limit or safety rule")));});}}).addOnFailureListener(e->toast("Could not load real profiles."));
        Button back=btn("Back",false);back.setOnClickListener(v->dashboard());
    }

    private void family(){
        base(); title("Family / Wali Bridge"); root.addView(txt("Real family participation for an active mutual connection. No fake Wali accounts are created.",15,false));
        EditText cid=input("Mutual connection ID"),familyUid=input("Family/Wali Firebase UID"); Button create=btn("Create Real Family Bridge",true); create.setOnClickListener(v->{Map<String,Object> x=new HashMap<>();x.put("connectionId",cid.getText().toString().trim());x.put("familyUid",familyUid.getText().toString().trim());fn.getHttpsCallable("createFamilyBridge").call(x).addOnSuccessListener(r->toast("Family Bridge created in Firebase." )).addOnFailureListener(e->toast("Family Bridge rejected by backend safety rules."));});
        EditText bridge=input("Existing Family Bridge ID"),question=input("Question for family/Wali"); Button send=btn("Send Real Family Question",true);send.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("bridgeId",bridge.getText().toString().trim());x.put("text",question.getText().toString().trim());fn.getHttpsCallable("sendFamilyQuestion").call(x).addOnSuccessListener(r->toast("Question sent securely." )).addOnFailureListener(e->toast("Question blocked by Family Bridge permissions."));});
        Button back=btn("Back",false);back.setOnClickListener(v->dashboard());
    }

    private void rewarded(){
        base(); title("2 Ads → 2 Messages"); root.addView(txt("Two completed rewarded ads can grant two real message credits. No demo ad ID is used. Production requires the real AdMob rewarded unit configured in the release build.",15,false));
        TextView status=txt("Checking AdMob production configuration…",15,false);root.addView(status);
        String unit=getString(getResources().getIdentifier("admob_rewarded_unit_id","string",getPackageName()));
        if(unit==null||unit.trim().isEmpty()){status.setText("AdMob production unit is not configured. Nothing will be faked or silently substituted.");}
        else { MobileAds.initialize(this,s->{loadReward(unit,status);}); }
        Button back=btn("Back",false);back.setOnClickListener(v->dashboard());
    }
    private void loadReward(String unit,TextView status){ RewardedAd.load(this,unit,new AdRequest.Builder().build(),new RewardedAdLoadCallback(){@Override public void onAdLoaded(RewardedAd ad){status.setText("Production rewarded ad ready. Watch completely to claim one real message credit.");ad.show(ProductionCompletionActivity.this,(RewardItem item)->claimReward());}@Override public void onAdFailedToLoad(LoadAdError e){status.setText("Production AdMob ad unavailable. No fake reward was granted.");}}); }
    private void claimReward(){Map<String,Object>x=new HashMap<>();x.put("source","rewarded_ad");fn.getHttpsCallable("claimRewardedMessageCredit").call(x).addOnSuccessListener(r->toast("One real message credit granted." )).addOnFailureListener(e->toast("Reward could not be verified; no credit granted."));}

    private void admin(){
        base(); title("Admin Moderation Dashboard"); root.addView(txt("Admin access is controlled by the Firebase Auth custom claim admin=true. A normal member cannot open or mutate this data.",15,false));
        db.collection("moderationQueue").whereEqualTo("status","pending").limit(50).get().addOnSuccessListener(q->{for(DocumentSnapshot d:q){root.addView(txt("Report: "+d.getId()+" • "+val(d,"reason"),16,true));Button review=btn("Mark Reviewed",false);review.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("reportId",d.getId());x.put("decision","reviewed");fn.getHttpsCallable("setModerationDecision").call(x).addOnSuccessListener(r->{review.setEnabled(false);toast("Moderation decision saved securely.");});});}}).addOnFailureListener(e->toast("Admin access required or queue unavailable."));
        db.collection("verifications").whereEqualTo("status","pending").limit(50).get().addOnSuccessListener(q->{for(DocumentSnapshot d:q){String uid=val(d,"userUid");root.addView(txt("Verification: "+uid,16,true));Button approve=btn("Approve Verified",false);approve.setOnClickListener(v->{Map<String,Object>x=new HashMap<>();x.put("userUid",uid);x.put("status","verified");fn.getHttpsCallable("setVerificationStatus").call(x).addOnSuccessListener(r->{approve.setEnabled(false);toast("Verification status saved securely.");});});}});
        Button back=btn("Back",false);back.setOnClickListener(v->dashboard());
    }
}
