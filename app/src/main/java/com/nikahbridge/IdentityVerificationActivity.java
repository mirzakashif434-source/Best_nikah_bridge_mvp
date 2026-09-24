package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.File;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class IdentityVerificationActivity extends Activity{
 LinearLayout root;Spinner type;TextView status,fileLabel,selfieLabel;Uri selected,selfieUri;
 public void onCreate(Bundle b){super.onCreate(b);AzureAuthManager.bindActivity(this);build();load();}
 int dp(int v){return Premium2030Ui.dp(this,v);}
 Button btn(String s,boolean primary){Button b=primary?Premium2030Ui.primary(this,s):Premium2030Ui.secondary(this,s);Premium2030Ui.addButton(root,b);return b;}
 void build(){
   ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(Premium2030Ui.CREAM);
   root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(20),dp(18),dp(28));sc.addView(root);setContentView(sc);
   root.addView(Premium2030Ui.title(this,"Identity Verification"));
   root.addView(Premium2030Ui.subtitle(this,"A safer community for sincere people. Verification is available only to members age 18+ — male or female."));
   root.addView(Premium2030Ui.heroLine(this,"Government ID • Current Selfie • Authorized Review"));
   LinearLayout card=Premium2030Ui.card(this);
   card.addView(Premium2030Ui.section(this,"Private document verification"));
   TextView info=Premium2030Ui.subtitle(this,"Your ID and current selfie upload directly to private Azure Storage for authorized review. The app never marks a selfie approved before review.");
   info.setGravity(android.view.Gravity.START);info.setPadding(0,0,0,dp(8));card.addView(info);
   type=new Spinner(this);type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"identity","manual"}));card.addView(type,new LinearLayout.LayoutParams(-1,dp(54)));
   root.addView(card);
   Button pick=btn("Choose Private ID Document",false);
   fileLabel=Premium2030Ui.subtitle(this,"No document selected");fileLabel.setGravity(android.view.Gravity.START);root.addView(fileLabel);
   pick.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/jpeg","image/png","application/pdf"});i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,41);});
   Button submit=btn("Submit ID Verification",true);submit.setOnClickListener(v->submitDocument());
   Button camera=btn("Take Current Selfie with Camera",false);camera.setOnClickListener(v->takeSelfie());
   selfieLabel=Premium2030Ui.subtitle(this,"No current selfie captured");selfieLabel.setGravity(android.view.Gravity.START);root.addView(selfieLabel);
   Button submitSelfie=btn("Submit Selfie for Verification",true);submitSelfie.setOnClickListener(v->submitSelfie());
   Button refresh=btn("Refresh Verification Status",false);refresh.setOnClickListener(v->load());
   status=Premium2030Ui.subtitle(this,"Status: loading…");status.setGravity(android.view.Gravity.START);root.addView(status);
   Button back=btn("Back",false);back.setOnClickListener(v->finish());
 }
 void takeSelfie(){
   try{
     File dir=new File(getCacheDir(),"verification-selfie");
     if(!dir.exists()&&!dir.mkdirs()){status.setText("Status: secure selfie storage is unavailable");return;}
     File file=File.createTempFile("selfie-", ".jpg", dir);
     selfieUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
     Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
     if(i.resolveActivity(getPackageManager())==null){status.setText("Status: camera is unavailable");return;}
     i.putExtra(MediaStore.EXTRA_OUTPUT,selfieUri);
     i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
     startActivityForResult(i,42);
   }catch(Exception e){status.setText("Status: camera could not start securely");}
 }
 protected void onActivityResult(int r,int code,Intent data){
   super.onActivityResult(r,code,data);
   if(code!=RESULT_OK)return;
   if(r==41){
     if(data==null)return;
     selected=data.getData();
     fileLabel.setText(selected==null?"No document selected":"Document selected securely");
   }else if(r==42&&selfieUri!=null){
     selfieLabel.setText("Full-resolution current camera selfie captured");
   }
 }
 void load(){AzureApiClient.get("/verification",new AzureApiClient.Callback(){public void ok(int c,String s){runOnUiThread(()->{try{org.json.JSONArray a=new org.json.JSONObject(s).optJSONArray("verifications");int approved=0,pending=0;if(a!=null)for(int i=0;i<a.length();i++){String st=a.optJSONObject(i).optString("status");if("approved".equals(st))approved++;if("pending".equals(st))pending++;}status.setText("Status: Azure verification loaded • approved "+approved+" • pending "+pending);}catch(Exception e){status.setText("Status: Azure verification loaded");}});}public void err(String e){runOnUiThread(()->status.setText("Status: Azure verification status unavailable"));}});}
 void submitDocument(){
   if(!AzureAuthManager.hasAccount(this)){status.setText("Status: sign in with Azure first");return;}
   if(selected==null){status.setText("Status: choose a document first");return;}
   try{
     String mime=getContentResolver().getType(selected);
     if(mime==null || !(mime.equals("application/pdf")||mime.equals("image/png")||mime.equals("image/jpeg"))){
       status.setText("Status: unsupported document type. Use PDF, JPG, or PNG.");return;
     }
     InputStream in=getContentResolver().openInputStream(selected);
     if(in==null){status.setText("Status: document could not be read");return;}
     ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;
     while((n=in.read(b))>0){total+=n;if(total>8*1024*1024){in.close();status.setText("Status: document is larger than 8 MB");return;}out.write(b,0,n);}
     in.close();
     if(out.size()==0){status.setText("Status: document is empty");return;}
     String name="verification-"+System.currentTimeMillis();String ext=mime.equals("application/pdf")?".pdf":mime.equals("image/png")?".png":".jpg";
     status.setText("Status: uploading ID securely to Azure…");
     AzureApiClient.multipart("/verification/identity","document",name+ext,mime,out.toByteArray(),new String[]{"documentType"},new String[]{String.valueOf(type.getSelectedItem())},new AzureApiClient.Callback(){public void ok(int c,String s){runOnUiThread(()->status.setText("Status: ID submitted to Azure — pending authorized review"));}public void err(String e){runOnUiThread(()->status.setText(e!=null&&e.contains("AGE_18_PLUS_REQUIRED")?"Status: ❌ Verification is only for age 18+.":"Status: Azure ID submission failed"));}});
   }catch(Exception e){status.setText("Status: document could not be prepared");}
 }
 void submitSelfie(){
   if(!AzureAuthManager.hasAccount(this)){status.setText("Status: sign in with Azure first");return;}
   if(selfieUri==null){status.setText("Status: take a current selfie first");return;}
   try{
     InputStream in=getContentResolver().openInputStream(selfieUri);
     if(in==null){status.setText("Status: selfie could not be read");return;}
     ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n,total=0;
     while((n=in.read(b))>0){total+=n;if(total>4*1024*1024){in.close();status.setText("Status: selfie is larger than 4 MB. Retake in normal camera quality.");return;}out.write(b,0,n);}
     in.close();
     byte[] bytes=out.toByteArray();
     if(bytes.length==0){status.setText("Status: selfie size is invalid");return;}
     status.setText("Status: uploading full-resolution current selfie securely to Azure…");
     AzureApiClient.multipart("/verification/selfie","selfie","selfie-"+System.currentTimeMillis()+".jpg","image/jpeg",bytes,new String[]{},new String[]{},new AzureApiClient.Callback(){
       public void ok(int c,String s){runOnUiThread(()->status.setText("Status: selfie submitted — pending authorized review"));}
       public void err(String e){runOnUiThread(()->status.setText(e!=null&&e.contains("AGE_18_PLUS_REQUIRED")?"Status: ❌ Verification is only for age 18+.":"Status: Azure selfie submission failed"));}
     });
   }catch(Exception e){status.setText("Status: selfie could not be prepared securely");}
 }
}
