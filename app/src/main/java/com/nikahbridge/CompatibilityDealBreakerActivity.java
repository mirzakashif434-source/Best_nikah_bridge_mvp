package com.nikahbridge;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/** Real deal-breaker comparison using stated Firebase preferences only. */
public class CompatibilityDealBreakerActivity extends Activity {
    private FirebaseAuth auth; private FirebaseFirestore db; private LinearLayout root; private EditText uid; private TextView result;
    private final int green=Color.rgb(18,103,82), dark=Color.rgb(30,45,41), gray=Color.rgb(85,100,95), light=Color.rgb(247,250,249);
    @Override public void onCreate(Bundle b){super.onCreate(b);auth=FirebaseAuth.getInstance();db=FirebaseFirestore.getInstance();render();}
    private void base(){ScrollView s=new ScrollView(this);s.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,22,20,30);root.setBackgroundColor(light);s.addView(root);setContentView(s);}
    private TextView txt(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(bold?dark:gray);t.setPadding(6,8,6,10);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private Button btn(String x,boolean fill){Button b=new Button(this);b.setText(x);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(fill?Color.WHITE:green);GradientDrawable g=new GradientDrawable();g.setColor(fill?green:Color.WHITE);g.setCornerRadius(18);if(!fill)g.setStroke(2,green);b.setBackground(g);root.addView(b,new LinearLayout.LayoutParams(-1,62));return b;}
    private void render(){base();root.addView(txt("Compatibility Deal-Breaker Engine",27,true));root.addView(txt("Compare a real member's stated deal-breakers with your own stated preferences. The engine never invents hidden traits or labels a person as good/bad.",15,false));uid=new EditText(this);uid.setHint("Real matched member Firebase UID");uid.setTextSize(16);root.addView(uid,new LinearLayout.LayoutParams(-1,62));Button check=btn("Check Real Deal-Breakers",true);check.setOnClickListener(v->check());result=txt("",16,false);root.addView(result);Button back=btn("Back",false);back.setOnClickListener(v->finish());}
    private String text(DocumentSnapshot d,String k){Object v=d.get(k);return v==null?"":String.valueOf(v);}
    private Set<String> tokens(String s){Set<String> out=new HashSet<>();for(String p:s.toLowerCase(Locale.ROOT).split("[,;|\\n]+")){p=p.trim();if(p.length()>2)out.add(p);}return out;}
    private void check(){if(auth.getCurrentUser()==null){result.setText("Sign in required.");return;}String other=uid.getText().toString().trim();if(other.isEmpty()||other.equals(auth.getUid())){result.setText("Enter a different real member UID.");return;}db.collection("users").document(auth.getUid()).get().addOnSuccessListener(me->db.collection("users").document(other).get().addOnSuccessListener(them->{if(!them.exists()||!Boolean.TRUE.equals(them.getBoolean("profileActive"))||!Boolean.TRUE.equals(them.getBoolean("discoverable"))){result.setText("That real profile is unavailable.");return;}Set<String> mine=tokens(text(me,"dealbreakers"));Set<String> theirs=tokens(text(them,"dealbreakers"));Set<String> conflicts=new TreeSet<>();for(String a:mine)for(String b:theirs){if(a.equals(b)||a.contains(b)||b.contains(a))conflicts.add(a.length()<=b.length()?a:b);}StringBuilder s=new StringBuilder();s.append(conflicts.isEmpty()?"NO STATED KEYWORD CONFLICT DETECTED":"STATED KEYWORD CONFLICTS FOUND").append("\n\n");if(conflicts.isEmpty())s.append("Your saved deal-breakers and the other member's saved deal-breakers do not share an obvious keyword.");else{s.append("Review these stated terms together:\n");for(String x:conflicts)s.append("• ").append(x).append("\n");}s.append("\n\nThis is a keyword-level check only. It does not understand context, consent, character, religion, or safety beyond recorded platform data. Discuss important deal-breakers directly before proceeding.");result.setText(s.toString());}).addOnFailureListener(e->result.setText("Could not read the real member profile."))).addOnFailureListener(e->result.setText("Could not load your real profile."));}
}
