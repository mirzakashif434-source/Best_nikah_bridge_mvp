package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.HashMap;

/**
 * Real account-deletion screen for Best Nikah Bridge.
 *
 * The existing deleteMyAccount backend remains untouched. This additive screen
 * adds an explicit, authenticated, user-facing deletion path with recent
 * sign-in verification before the permanent backend deletion call.
 */
public class AccountDeletionActivity extends Activity {
    private LinearLayout root;
    private FirebaseAuth auth;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(95,108,103), red=Color.rgb(165,50,50), light=Color.rgb(247,250,249);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        auth=FirebaseAuth.getInstance();
        build();
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(bold?dark:gray);t.setPadding(dp(5),dp(8),dp(5),dp(12));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button button(String label,boolean primary){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(primary?Color.WHITE:green);android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(primary?green:Color.WHITE);g.setCornerRadius(dp(18));if(!primary)g.setStroke(dp(2),green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;}

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(light);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(24),dp(22),dp(32));scroll.addView(root);setContentView(scroll);
        TextView title=text("Delete Best Nikah Bridge Account",28,true);title.setGravity(Gravity.CENTER);root.addView(title);
        root.addView(text("This is a permanent deletion. Your Best Nikah Bridge account and personal profile data will be removed through the authenticated Firebase backend. This action cannot be undone.",16,false));
        root.addView(text("Before deleting:\n• Your profile will no longer be available for matching.\n• Interests, connections and related account records handled by the deletion service will be removed.\n• Safety/legal records may be retained only where legally required or necessary for fraud/security prevention.\n• Sign out is different from deletion: use Back if you only want to leave the account signed in.",15,false));

        FirebaseUser user=auth.getCurrentUser();
        if(user==null){root.addView(text("You are not signed in. Sign in first to delete your account.",16,true));Button back=button("Back",false);back.setOnClickListener(v->finish());return;}

        EditText password=new EditText(this);password.setHint("Confirm with your account password");password.setTextSize(16);password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);root.addView(password,new LinearLayout.LayoutParams(-1,dp(62)));
        CheckBox confirm=new CheckBox(this);confirm.setText("I understand that account deletion is permanent and cannot be undone.");root.addView(confirm);
        Button delete=button("Permanently Delete My Account",true);delete.setTextColor(Color.WHITE);
        android.graphics.drawable.GradientDrawable danger=new android.graphics.drawable.GradientDrawable();danger.setColor(red);danger.setCornerRadius(dp(18));delete.setBackground(danger);
        Button signOut=button("Sign out only",false);Button back=button("Back",false);

        delete.setOnClickListener(v->{
            FirebaseUser current=auth.getCurrentUser();
            if(current==null){toast("Please sign in again.");return;}
            if(!confirm.isChecked()){toast("Please confirm permanent deletion first.");return;}
            String p=password.getText().toString();
            if(p.length()<8){password.setError("Enter your current password");return;}
            if(current.getEmail()==null||current.getEmail().trim().isEmpty()){toast("This account has no email/password credential available for secure re-authentication.");return;}
            delete.setEnabled(false);signOut.setEnabled(false);back.setEnabled(false);
            password.setEnabled(false);confirm.setEnabled(false);
            current.reauthenticate(EmailAuthProvider.getCredential(current.getEmail(),p))
                    .addOnSuccessListener(x->callDeletion(delete,signOut,back))
                    .addOnFailureListener(e->{password.setEnabled(true);confirm.setEnabled(true);delete.setEnabled(true);signOut.setEnabled(true);back.setEnabled(true);toast("Password confirmation failed. Account was not deleted.");});
        });
        signOut.setOnClickListener(v->{auth.signOut();toast("Signed out. Your account was not deleted.");finish();});
        back.setOnClickListener(v->finish());
    }

    private void callDeletion(Button delete,Button signOut,Button back){
        delete.setText("Deleting securely…");
        FirebaseFunctions.getInstance().getHttpsCallable("deleteMyAccount").call(new HashMap<>())
                .addOnSuccessListener(result->{auth.signOut();Toast.makeText(this,"Account deleted permanently.",Toast.LENGTH_LONG).show();finish();})
                .addOnFailureListener(e->{delete.setEnabled(true);signOut.setEnabled(true);back.setEnabled(true);delete.setText("Permanently Delete My Account");toast("Deletion failed. No deletion confirmation was recorded. Please retry when online.");});
    }

    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
