package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/** Production bilingual Welcome. Real Firebase account flow preserved. */
public class WelcomeActivity extends Activity {
    private final int dark=Color.rgb(25,55,45);
    private FirebaseAuth auth; private FirebaseFirestore db;
    @Override protected void onCreate(Bundle state){super.onCreate(state);try{if(FirebaseApp.getApps(this).isEmpty())FirebaseApp.initializeApp(this);auth=FirebaseAuth.getInstance();}catch(RuntimeException ignored){auth=null;}if(auth!=null&&auth.getCurrentUser()!=null){startActivity(new Intent(this,MainActivity.class));finish();return;}showWelcome();}
    private void showWelcome(){requestWindowFeature(Window.FEATURE_NO_TITLE);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);scroll.setBackgroundColor(Color.rgb(250,247,239));FrameLayout canvas=new FrameLayout(this);WelcomeArtworkView artwork=new WelcomeArtworkView(this);canvas.addView(artwork,new FrameLayout.LayoutParams(-1,-2));TextView start=hit("GET STARTED / آغاز کریں"),login=hit("SIGN IN / لاگ اِن کریں"),lang=hit("English / اردو");canvas.addView(start);canvas.addView(login);canvas.addView(lang);artwork.post(()->position(artwork,start,login,lang));start.setOnClickListener(v->showAccountDialog(false));login.setOnClickListener(v->showAccountDialog(true));lang.setOnClickListener(v->showLanguageDialog());scroll.addView(canvas,new ScrollView.LayoutParams(-1,-2));setContentView(scroll);}
    private TextView hit(String d){TextView v=new TextView(this);v.setText("");v.setContentDescription(d);v.setBackgroundColor(Color.TRANSPARENT);v.setClickable(true);v.setFocusable(true);return v;}
    private void position(WelcomeArtworkView a,TextView s,TextView l,TextView g){int w=a.getWidth(),h=a.getHeight();if(w<=0)return;int bw=(int)(w*.74f),bh=Math.max(dp(52),(int)(h*.06f)),left=(w-bw)/2;place(s,left,(int)(h*.800f),bw,bh);place(l,left,(int)(h*.878f),bw,bh);place(g,(int)(w*.735f),(int)(h*.010f),(int)(w*.245f),Math.max(dp(44),(int)(h*.055f)));}
    private void place(View v,int x,int y,int w,int h){FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(w,h);p.leftMargin=x;p.topMargin=y;v.setLayoutParams(p);}
    private void showLanguageDialog(){new AlertDialog.Builder(this).setTitle("Language / زبان").setSingleChoiceItems(new String[]{"English","اردو"},0,(d,w)->{d.dismiss();toast(w==0?"English selected.":"اردو منتخب کیا گیا۔");}).setNegativeButton("Cancel",null).show();}
    private void showAccountDialog(boolean signIn){if(auth==null){try{if(FirebaseApp.getApps(this).isEmpty())FirebaseApp.initializeApp(this);auth=FirebaseAuth.getInstance();}catch(RuntimeException ignored){toast("Secure account service is temporarily unavailable. Please try again.");return;}}LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),0);EditText email=new EditText(this),pass=new EditText(this);email.setHint("Email address");email.setTextColor(dark);email.setHintTextColor(Color.rgb(100,100,100));email.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);email.setSingleLine(true);pass.setHint("Password (minimum 8 characters)");pass.setTextColor(dark);pass.setHintTextColor(Color.rgb(100,100,100));pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);pass.setSingleLine(true);box.addView(email,new LinearLayout.LayoutParams(-1,dp(62)));box.addView(pass,new LinearLayout.LayoutParams(-1,dp(62)));AlertDialog dialog=new AlertDialog.Builder(this).setTitle(signIn?"Sign in to Best Nikah Bridge":"Create your real account").setMessage(signIn?"Use your real Firebase account.":"Create a real account, then verify your email and complete your genuine Nikah profile.").setView(box).setNegativeButton("Cancel",null).setPositiveButton(signIn?"SIGN IN":"GET STARTED",null).create();dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String e=email.getText().toString().trim(),p=pass.getText().toString();if(!e.contains("@")){email.setError("Valid email required");return;}if(p.length()<8){pass.setError("Minimum 8 characters");return;}if(signIn){auth.signInWithEmailAndPassword(e,p).addOnSuccessListener(r->enterMain(dialog)).addOnFailureListener(r->toast("Sign in failed. Check your email and password."));}else{auth.createUserWithEmailAndPassword(e,p).addOnSuccessListener(r->{FirebaseUser u=r.getUser();if(u==null)return;u.sendEmailVerification();Map<String,Object> profile=new HashMap<>();profile.put("uid",u.getUid());profile.put("profileActive",false);profile.put("discoverable",false);profile.put("verificationStatus","unverified");profile.put("termsAccepted",false);profile.put("intentConfirmed",false);profile.put("createdAt",FieldValue.serverTimestamp());if(db==null)db=FirebaseFirestore.getInstance();db.collection("users").document(u.getUid()).set(profile).addOnSuccessListener(ok->enterMain(dialog)).addOnFailureListener(er->toast("Account created, but secure profile setup needs another try."));}).addOnFailureListener(er->toast("Account creation failed. Email may already be registered."));}}));dialog.show();}
    private void enterMain(AlertDialog d){d.dismiss();startActivity(new Intent(this,MainActivity.class));finish();}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
