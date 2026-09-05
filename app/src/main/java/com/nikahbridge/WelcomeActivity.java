package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

/**
 * Locked Best Nikah Bridge Welcome experience.
 * All entry actions lead to the real Firebase account/profile flow.
 * No demo users, fake matches, or local-only authentication.
 *
 * The final artwork is used only as the visual Welcome layer. Existing
 * Firebase account/sign-in logic remains unchanged.
 */
public class WelcomeActivity {
    private final int green = Color.rgb(18, 103, 82);
    private final int dark = Color.rgb(25, 55, 45);
    private final int gold = Color.rgb(190, 145, 45);
    private final int cream = Color.rgb(250, 247, 239);
    private LinearLayout root;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this);
            auth = FirebaseAuth.getInstance();
        } catch (RuntimeException ignored) { auth = null; }
        if (auth != null && auth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class)); finish(); return;
        }
        showWelcome();
    }

    private void showWelcome() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(cream);
        scroll.setVerticalScrollBarEnabled(false);

        FrameLayout canvas = new FrameLayout(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(cream);

        ImageView artwork = new ImageView(this);
        artwork.setImageResource(com.nikahbridge.R.drawable.welcome_final);
        artwork.setScaleType(ImageView.ScaleType.FIT_XY);
        artwork.setAdjustViewBounds(true);
        canvas.addView(artwork, new FrameLayout.LayoutParams(-1, -2));

        TextView getStarted = hitTarget("Get Started / آغاز کریں");
        TextView signIn = hitTarget("Sign in / لاگ ان کریں");
        TextView language = hitTarget("Language: English / اردو");

        canvas.addView(getStarted);
        canvas.addView(signIn);
        canvas.addView(language);

        artwork.post(() -> positionHitTargets(artwork, getStarted, signIn, language));

        getStarted.setOnClickListener(v -> showAccountDialog(false));
        signIn.setOnClickListener(v -> showAccountDialog(true));
        language.setOnClickListener(v -> showLanguageDialog());

        root.addView(canvas, new LinearLayout.LayoutParams(-1, -2));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);
    }

    private TextView hitTarget(String description) {
        TextView v = new TextView(this);
        v.setText("");
        v.setContentDescription(description);
        v.setBackgroundColor(Color.TRANSPARENT);
        v.setClickable(true);
        v.setFocusable(true);
        return v;
    }

    private void positionHitTargets(ImageView artwork, TextView getStarted, TextView signIn, TextView language) {
        int w = artwork.getWidth();
        if (w <= 0) return;
        int h = (int)(w * 1.5f);
        int buttonW = (int)(w * 0.74f);
        int buttonH = Math.max(dp(54), (int)(h * 0.060f));
        int left = (w - buttonW) / 2;

        place(getStarted, left, (int)(h * 0.792f), buttonW, buttonH);
        place(signIn, left, (int)(h * 0.864f), buttonW, buttonH);
        place(language, (int)(w * 0.755f), (int)(h * 0.018f), (int)(w * 0.205f), Math.max(dp(52), (int)(h * 0.055f)));
    }

    private void place(View v, int left, int top, int width, int height) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(width, height);
        p.leftMargin = left;
        p.topMargin = top;
        v.setLayoutParams(p);
    }

    private void showLanguageDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Language / زبان")
                .setSingleChoiceItems(new String[]{"English", "اردو"}, 0, (dialog, which) -> {
                    dialog.dismiss();
                    toast(which == 0 ? "English selected." : "اردو منتخب کیا گیا۔");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Button button(String label,boolean filled){ Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(17); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD); b.setGravity(Gravity.CENTER); b.setTextColor(filled?Color.WHITE:green); GradientDrawable g=new GradientDrawable(); g.setCornerRadius(dp(32)); g.setColor(filled?green:Color.TRANSPARENT); if(!filled)g.setStroke(dp(2),green); b.setBackground(g); return b; }
    private TextView text(String value,int size,boolean bold,int color){ TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t; }
    private LinearLayout.LayoutParams params(int w,int h,int l,int t,int r,int b){ LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(dp(l),dp(t),dp(r),dp(b)); return p; }
    private int dp(int value){ return (int)(value*getResources().getDisplayMetrics().density+0.5f); }

    private void showAccountDialog(boolean signIn){
        if(auth==null){try{if(FirebaseApp.getApps(this).isEmpty())FirebaseApp.initializeApp(this);auth=FirebaseAuth.getInstance();}catch(RuntimeException ignored){toast("Secure account service is temporarily unavailable. Please try again.");return;}}
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8),dp(4),dp(8),0);
        EditText email=new EditText(this); email.setHint("Email address"); email.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); email.setTextColor(dark); email.setHintTextColor(Color.rgb(100,100,100)); email.setSingleLine(true);
        EditText password=new EditText(this); password.setHint("Password (minimum 8 characters)"); password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); password.setTextColor(dark); password.setHintTextColor(Color.rgb(100,100,100)); password.setSingleLine(true);
        box.addView(email,new LinearLayout.LayoutParams(-1,dp(62))); box.addView(password,new LinearLayout.LayoutParams(-1,dp(62)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(signIn?"Sign in to Best Nikah Bridge":"Create your real account").setMessage(signIn?"Use your real Firebase account.":"Create a real account, then verify your email and complete your genuine Nikah profile.").setView(box).setNegativeButton("Cancel",null).setPositiveButton(signIn?"SIGN IN":"GET STARTED",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String e=email.getText().toString().trim();String p=password.getText().toString();if(!e.contains("@")){email.setError("Valid email required");return;}if(p.length()<8){password.setError("Minimum 8 characters");return;}if(signIn){auth.signInWithEmailAndPassword(e,p).addOnSuccessListener(result->enterMain(dialog)).addOnFailureListener(err->toast("Sign in failed. Check your email and password."));}else{auth.createUserWithEmailAndPassword(e,p).addOnSuccessListener(result->{FirebaseUser u=result.getUser();if(u==null)return;u.sendEmailVerification();Map<String,Object> profile=new HashMap<>();profile.put("uid",u.getUid());profile.put("profileActive",false);profile.put("discoverable",false);profile.put("verificationStatus","unverified");profile.put("termsAccepted",false);profile.put("intentConfirmed",false);profile.put("createdAt",FieldValue.serverTimestamp());if(db==null)db=FirebaseFirestore.getInstance();db.collection("users").document(u.getUid()).set(profile).addOnSuccessListener(ok->enterMain(dialog)).addOnFailureListener(err->toast("Account created, but secure profile setup needs another try."));}).addOnFailureListener(err->toast("Account creation failed. Email may already be registered."));}}));
        dialog.show();
    }
    private void enterMain(AlertDialog dialog){dialog.dismiss();startActivity(new Intent(this,MainActivity.class));finish();}
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}
