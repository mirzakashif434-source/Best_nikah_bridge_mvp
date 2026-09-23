package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class IdentityVerificationActivity extends Activity{
 LinearLayout root;Spinner type;TextView status,fileLabel;Uri selected;
 public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);build();load();}
 int dp(int v){return Premium2030Ui.dp(this,v);}
 Button btn(String s,boolean primary){Button b=primary?Premium2030Ui.primary(this,s):Premium2030Ui.secondary(this,s);Premium2030Ui.addButton(root,b);return b;}
 void build(){
   ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Premium2030Ui.CREAM);
   root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(28));sc.addView(root);setContentView(sc);
   root.addView(Premium2030Ui.title(this,"Identity Verification"));
   root.addView(Premium2030Ui.subtitle(this,"A safer community for sincere people."));
   root.addView(Premium2030Ui.heroLine(this,"Trusted • Genuine • Serious"));
   LinearLayout card=Premium2030Ui.card(this);
   card.addView(Premium2030Ui.section(this,"Private document verification"));
   TextView info=Premium2030Ui.subtitle(this,"Your document uploads directly to private Azure Storage for authorized review.");
   info.setGravity(android.view.Gravity.START);info.setPadding(0,0,0,dp(8));card.addView(info);
   type=new Spinner(this);type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"identity","manual"}));card.addView(type,new LinearLayout.LayoutParams(-1,dp(54)));
   root.addView(card);
   Button pick=btn("Choose Private ID Document",false);
   fileLabel=Premium2030Ui.subtitle(this,"No document selected");fileLabel.setGravity(android.view.Gravity.START);root.addView(fileLabel);
   pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/jpeg","image/png","application/pdf"});i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,41);});
   Button submit=btn("Submit Real Verification",true);submit.setOnClickListener(v->submit());
   Button refresh=btn("Refresh Status",false);refresh.setOnClickListener(v->load());
   status=Premium2030Ui.subtitle(this,"Status: loading…");status.setGravity(android.view.Gravity.START);root.addView(status);
   Button back=btn("Back",false);back.setOnClickListener(v->finish());
 }
 protected void onActivityResult(int r,int code,Intent data){super.onActivityResult(r,code,data);if(r==41&&code==RESULT_OK&&data!=null){selected=data.getData();fileLabel.setText(selected==null?"No document selected":"Document selected securely");}}
 void load(){AzureApiClient.get("/verification",new AzureApiClient.Callback(){public void ok(int c,String s){runOnUiThread(()->status.setText("Status: Azure verification status loaded"));}public void err(String e){runOnUiThread(()->status.setText("Status: Azure verification status unavailable"));}});}
 void submit(){if(selected==null){status.setText("Status: choose a document first");return;}try{InputStream in=getContentResolver().openInputStream(selected);ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))>0){total+=n;if(total>8*1024*1024)throw new Exception("Document too large");out.write(b,0,n);}in.close();String mime=getContentResolver().getType(selected);if(mime==null)mime="application/octet-stream";String name="verification-"+System.currentTimeMillis();String ext=mime.equals("application/pdf")?".pdf":mime.equals("image/png")?".png":".jpg";status.setText("Status: uploading securely to Azure…");AzureApiClient.multipart("/verification/identity","document",name+ext,mime,out.toByteArray(),new String[]{"documentType"},new String[]{String.valueOf(type.getSelectedItem())},new AzureApiClient.Callback(){public void ok(int c,String s){runOnUiThread(()->status.setText("Status: verification submitted to Azure — pending authorized review"));}public void err(String e){runOnUiThread(()->status.setText("Status: Azure verification submission failed"));}});}catch(Exception e){status.setText("Status: document could not be prepared");}}
}
