package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONArray;
import org.json.JSONObject;

public class FamilyCircleActivity extends Activity {
    private static final String PREFS="family_nikah_circle_pending", KEY="invite_token";
    private LinearLayout root,membersBox,invitesBox,suggestionsBox;
    private TextView status;
    private Spinner role;
    private EditText suggestedUser,note;
    private String lastInviteLink,lastInviteShareUrl,myRole="";

    public static void savePendingInvite(android.content.Context c,String token){
        if(token!=null&&!token.trim().isEmpty())c.getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY,token.trim()).apply();
    }
    public static String pendingInvite(android.content.Context c){
        return c.getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY,"");
    }
    public static void clearPendingInvite(android.content.Context c){
        c.getSharedPreferences(PREFS,MODE_PRIVATE).edit().remove(KEY).apply();
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        AzureAuthManager.bindActivity(this);
        captureDeepLink(getIntent());
        if(!AzureAuthManager.hasAccount(this)){
            buildAuthRecovery();
            return;
        }
        build();
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);captureDeepLink(i);if(AzureAuthManager.hasAccount(this))tryJoinPending();}
    @Override protected void onResume(){super.onResume();if(AzureAuthManager.hasAccount(this)){tryJoinPending();load();}}

    private void buildAuthRecovery(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);sc.setBackgroundColor(Premium2030Ui.CREAM);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(24),dp(18),dp(34));root.setBackgroundColor(Premium2030Ui.CREAM);
        sc.addView(root);setContentView(sc);

        root.addView(Premium2030Ui.title(this,"Best Nikah Family Circle"));
        root.addView(Premium2030Ui.subtitle(this,"Sign in with Azure to manage your real private Family Circle, invites, members and suggestions."));
        Button signIn=btn("Sign in with Azure",true);
        Button back=btn("Back",false);
        signIn.setOnClickListener(v->startActivity(new Intent(this,AzureExternalAuthActivity.class)));
        back.setOnClickListener(v->finish());

        ViewCompat.setOnApplyWindowInsetsListener(sc,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(18),dp(24),dp(18),dp(34)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(sc);
    }

    private void captureDeepLink(Intent i){
        Uri d=i==null?null:i.getData();
        if(d!=null&&"bestnikahbredge".equalsIgnoreCase(d.getScheme())&&"circle".equalsIgnoreCase(d.getHost())){
            String token=d.getQueryParameter("token"); if(token!=null&&!token.trim().isEmpty())savePendingInvite(this,token);
        }
    }

    private int dp(int v){return Premium2030Ui.dp(this,v);}
    private Button btn(String s,boolean primary){Button b=primary?Premium2030Ui.primary(this,s):Premium2030Ui.secondary(this,s);Premium2030Ui.addButton(root,b);return b;}
    private EditText input(String h){EditText e=new EditText(this);e.setHint(h);e.setTextSize(16);e.setPadding(dp(14),0,dp(14),0);e.setBackground(Premium2030Ui.outlined(this,android.graphics.Color.WHITE,Premium2030Ui.GOLD_SOFT,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.setMargins(0,dp(4),0,dp(6));root.addView(e,lp);return e;}

    private void build(){
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);sc.setBackgroundColor(Premium2030Ui.CREAM);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(34));sc.addView(root);setContentView(sc);
        ViewCompat.setOnApplyWindowInsetsListener(sc,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(18),dp(20),dp(18),dp(34)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(sc);

        root.addView(Premium2030Ui.title(this,"Best Nikah Family Circle"));
        root.addView(Premium2030Ui.subtitle(this,"Invite trusted family, involve your Wali, and let your circle suggest serious matches."));
        root.addView(Premium2030Ui.heroLine(this,"Private family network • Real Azure accounts • Your consent stays in control"));

        root.addView(Premium2030Ui.section(this,"Invite Family"));
        role=new Spinner(this);
        role.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"wali","parent","sibling","family","trusted"}));
        root.addView(role,new LinearLayout.LayoutParams(-1,dp(54)));
        Button create=btn("Create Secure Invite",true);create.setOnClickListener(v->createInvite());
        Button share=btn("Share Invite on WhatsApp / SMS",false);share.setOnClickListener(v->shareInvite());
        invitesBox=new LinearLayout(this);invitesBox.setOrientation(LinearLayout.VERTICAL);root.addView(invitesBox);

        root.addView(Premium2030Ui.section(this,"Circle Members"));
        membersBox=new LinearLayout(this);membersBox.setOrientation(LinearLayout.VERTICAL);root.addView(membersBox);

        root.addView(Premium2030Ui.section(this,"Suggest a Serious Match"));
        suggestedUser=input("Real member ID");
        note=input("Optional family note");
        Button suggest=btn("Suggest Match to Circle Owner",true);suggest.setOnClickListener(v->suggest());

        root.addView(Premium2030Ui.section(this,"Family Suggestions"));
        suggestionsBox=new LinearLayout(this);suggestionsBox.setOrientation(LinearLayout.VERTICAL);root.addView(suggestionsBox);

        status=Premium2030Ui.subtitle(this,"Status: ready");status.setGravity(android.view.Gravity.START);root.addView(status);
        Button refresh=btn("Refresh Family Circle",false);refresh.setOnClickListener(v->load());
        Button back=btn("Back",false);back.setOnClickListener(v->finish());
    }

    private void tryJoinPending(){
        String token=pendingInvite(this);
        if(token==null||token.trim().isEmpty())return;
        try{
            JSONObject b=new JSONObject().put("token",token);
            status.setText("Status: joining secure family circle…");
            AzureApiClient.post("/family-circle/join",b.toString(),new AzureApiClient.Callback(){
                public void ok(int c,String s){runOnUiThread(()->{clearPendingInvite(FamilyCircleActivity.this);status.setText("Status: Family Circle joined securely");load();});}
                public void err(String e){runOnUiThread(()->{
                    if(e.contains("ALREADY_IN_CIRCLE")||e.contains("OWNER_ALREADY_IN_CIRCLE"))clearPendingInvite(FamilyCircleActivity.this);
                    status.setText("Status: invite could not be joined");
                });}
            });
        }catch(Exception e){status.setText("Status: invite is invalid");}
    }

    private void load(){
        if(!AzureAuthManager.hasAccount(this))return;
        AzureApiClient.get("/family-circle",new AzureApiClient.Callback(){
            public void ok(int c,String s){runOnUiThread(()->{
                try{
                    JSONObject o=new JSONObject(s);JSONObject circle=o.optJSONObject("circle");
                    JSONArray members=o.optJSONArray("members");
                    int memberLimit=o.optInt("memberLimit",2);
                    membersBox.removeAllViews();
                    membersBox.addView(Premium2030Ui.subtitle(FamilyCircleActivity.this,"Circle member limit: "+memberLimit));
                    if(circle!=null){
                        myRole=circle.optString("role","");
                        membersBox.addView(Premium2030Ui.subtitle(FamilyCircleActivity.this,"Circle: "+circle.optString("name","My Nikah Circle")+" • Role: "+myRole));
                    }
                    if(members==null||members.length()==0)membersBox.addView(Premium2030Ui.subtitle(FamilyCircleActivity.this,"No family members joined yet."));
                    else for(int i=0;i<members.length();i++){
                        JSONObject m=members.optJSONObject(i);if(m==null)continue;
                        String memberId=m.optString("id",""),roleName=m.optString("role","family");
                        LinearLayout card=Premium2030Ui.card(FamilyCircleActivity.this);
                        TextView row=Premium2030Ui.subtitle(FamilyCircleActivity.this,"• "+m.optString("display_name","Family member")+" — "+roleName);
                        LanguageManager.protectUserContent(row);card.addView(row);
                        if("owner".equals(myRole)&&!"owner".equals(roleName)&&!memberId.isEmpty()){
                            Button remove=Premium2030Ui.secondary(FamilyCircleActivity.this,"Remove from Circle");
                            card.addView(remove,new LinearLayout.LayoutParams(-1,dp(48)));
                            remove.setOnClickListener(v->removeMember(memberId));
                        }
                        membersBox.addView(card);
                    }
                    status.setText("Status: Family Circle loaded from Azure");
                    loadInvites();
                    loadSuggestions();
                }catch(Exception e){status.setText("Status: Family Circle response could not be read");}
            });}
            public void err(String e){runOnUiThread(()->status.setText("Status: Family Circle unavailable")); }
        });
    }

    private void createInvite(){
        if(!AzureAuthManager.hasAccount(this)){
            LanguageManager.dialog(this)
                .setTitle("Sign in required")
                .setMessage("Sign in with Azure to create a real Family Circle invite.")
                .setPositiveButton("Sign in",(d,w)->startActivity(new Intent(this,AzureExternalAuthActivity.class)))
                .setNegativeButton("Not now",null)
                .show();
            return;
        }
        try{
            JSONObject b=new JSONObject().put("role",String.valueOf(role.getSelectedItem())).put("maxUses",5).put("expiresDays",7);
            status.setText("Status: creating secure invite…");
            AzureApiClient.post("/family-circle/invites",b.toString(),new AzureApiClient.Callback(){
                public void ok(int c,String s){runOnUiThread(()->{try{JSONObject j=new JSONObject(s);lastInviteLink=j.optString("deepLink","");lastInviteShareUrl=j.optString("shareUrl",lastInviteLink);status.setText(lastInviteShareUrl.isEmpty()?"Status: invite created":"Status: invite ready to share");}catch(Exception e){status.setText("Status: invite created");}});}
                public void err(String e){runOnUiThread(()->status.setText("Status: invite creation failed")); }
            });
        }catch(Exception e){status.setText("Status: invite request error");}
    }

    private void loadInvites(){
        AzureApiClient.get("/family-circle/invites",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(body).optJSONArray("invites");invitesBox.removeAllViews();
                    if(a==null||a.length()==0)return;
                    invitesBox.addView(Premium2030Ui.section(FamilyCircleActivity.this,"Active Invites"));
                    for(int i=0;i<a.length();i++){
                        JSONObject x=a.optJSONObject(i);if(x==null)continue;
                        String id=x.optString("id",""),roleName=x.optString("role","family");
                        boolean revoked=!x.isNull("revoked_at");
                        TextView row=Premium2030Ui.subtitle(FamilyCircleActivity.this,roleName+" • uses "+x.optInt("use_count",0)+"/"+x.optInt("max_uses",0)+(revoked?" • revoked":""));
                        invitesBox.addView(row);
                        if("owner".equals(myRole)&&!revoked&&!id.isEmpty()){
                            Button revoke=Premium2030Ui.secondary(FamilyCircleActivity.this,"Revoke Invite");
                            invitesBox.addView(revoke,new LinearLayout.LayoutParams(-1,dp(46)));
                            revoke.setOnClickListener(v->revokeInvite(id));
                        }
                    }
                }catch(Exception ignored){}
            });}
            public void err(String e){}
        });
    }

    private void revokeInvite(String id){
        AzureApiClient.delete("/family-circle/invites/"+Uri.encode(id),"{}",new AzureApiClient.Callback(){
            public void ok(int c,String s){runOnUiThread(()->{status.setText("Status: invite revoked");loadInvites();});}
            public void err(String e){runOnUiThread(()->status.setText("Status: invite revoke failed"));}
        });
    }

    private void removeMember(String id){
        AzureApiClient.delete("/family-circle/members/"+Uri.encode(id),"{}",new AzureApiClient.Callback(){
            public void ok(int c,String s){runOnUiThread(()->{status.setText("Status: family member removed");load();});}
            public void err(String e){runOnUiThread(()->status.setText("Status: member removal failed"));}
        });
    }

    private void shareInvite(){
        String share=(lastInviteShareUrl==null||lastInviteShareUrl.trim().isEmpty())?lastInviteLink:lastInviteShareUrl;
        if(share==null||share.trim().isEmpty()){status.setText("Status: create an invite first");return;}
        String text="Join my private Best Nikah Family Circle. Open this secure invite:\n"+share;
        Intent send=new Intent(Intent.ACTION_SEND);send.setType("text/plain");send.putExtra(Intent.EXTRA_TEXT,text);
        startActivity(Intent.createChooser(send,"Share Family Circle Invite"));
    }

    private void suggest(){
        String id=suggestedUser.getText().toString().trim();
        if(id.isEmpty()){LanguageManager.setError(suggestedUser,"Real member ID required");return;}
        try{
            JSONObject b=new JSONObject().put("suggestedUserId",id).put("note",note.getText().toString().trim());
            status.setText("Status: sending family suggestion…");
            AzureApiClient.post("/family-circle/suggestions",b.toString(),new AzureApiClient.Callback(){
                public void ok(int c,String s){runOnUiThread(()->{suggestedUser.setText("");note.setText("");status.setText("Status: match suggestion stored securely");loadSuggestions();});}
                public void err(String e){runOnUiThread(()->status.setText("Status: match suggestion failed")); }
            });
        }catch(Exception e){status.setText("Status: suggestion request error");}
    }

    private void loadSuggestions(){
        AzureApiClient.get("/family-circle/suggestions",new AzureApiClient.Callback(){
            public void ok(int c,String s){runOnUiThread(()->{
                try{
                    JSONArray a=new JSONObject(s).optJSONArray("suggestions");suggestionsBox.removeAllViews();
                    if(a==null||a.length()==0){suggestionsBox.addView(Premium2030Ui.subtitle(FamilyCircleActivity.this,"No family match suggestions yet."));return;}
                    for(int i=0;i<a.length();i++){
                        JSONObject x=a.optJSONObject(i);if(x==null)continue;
                        String name=x.optString("suggested_name","Member"),by=x.optString("suggested_by_name","Family member"),st=x.optString("status","pending"),id=x.optString("id","");
                        LinearLayout card=Premium2030Ui.card(FamilyCircleActivity.this);
                        TextView t=Premium2030Ui.subtitle(FamilyCircleActivity.this,name+" • suggested by "+by+" • "+st);
                        LanguageManager.protectUserContent(t);card.addView(t);
                        String n=x.optString("note","");if(!n.isEmpty()){TextView nv=Premium2030Ui.subtitle(FamilyCircleActivity.this,n);LanguageManager.protectUserContent(nv);card.addView(nv);}
                        if("owner".equals(myRole)&&("pending".equals(st)||"viewed".equals(st))){
                            Button accept=Premium2030Ui.primary(FamilyCircleActivity.this,"Accept & Send Interest");
                            Button decline=Premium2030Ui.secondary(FamilyCircleActivity.this,"Decline");
                            card.addView(accept,new LinearLayout.LayoutParams(-1,dp(52)));card.addView(decline,new LinearLayout.LayoutParams(-1,dp(52)));
                            accept.setOnClickListener(v->respondSuggestion(id,"accepted"));
                            decline.setOnClickListener(v->respondSuggestion(id,"declined"));
                        }
                        suggestionsBox.addView(card);
                    }
                }catch(Exception e){status.setText("Status: suggestions could not be read");}
            });}
            public void err(String e){runOnUiThread(()->status.setText("Status: suggestions unavailable")); }
        });
    }

    private void respondSuggestion(String id,String decision){
        try{
            JSONObject b=new JSONObject().put("status",decision);
            AzureApiClient.patch("/family-circle/suggestions/"+Uri.encode(id),b.toString(),new AzureApiClient.Callback(){
                public void ok(int c,String s){runOnUiThread(()->{status.setText("Status: family suggestion updated");loadSuggestions();});}
                public void err(String e){runOnUiThread(()->status.setText("Status: suggestion update failed")); }
            });
        }catch(Exception e){status.setText("Status: suggestion update error");}
    }
}
