package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class RealFourPhotoActivity extends Activity {
    private static final int CAMERA_FIRST=8401,GALLERY_REMAINING=8402;
    private static final int MAX_BYTES=4*1024*1024;

    private LinearLayout root;
    private TextView status;
    private ImageView[] previews=new ImageView[4];
    private final ArrayList<Uri> galleryUris=new ArrayList<>();
    private Bitmap cameraBitmap;
    private String verificationSetId="";
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    @Override protected void onCreate(Bundle state){super.onCreate(state);AzureAuthManager.bindActivity(this);build();loadStatus();}

    private TextView text(String value,int size,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(Color.rgb(30,45,41));
        t.setGravity(Gravity.START);t.setPadding(dp(6),dp(7),dp(6),dp(7));
        if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        return t;
    }
    private Button button(String value,boolean primary){
        Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(16);
        b.setTextColor(primary?Color.WHITE:Color.rgb(18,103,82));b.setBackgroundColor(primary?Color.rgb(18,103,82):Color.WHITE);
        root.addView(b,new LinearLayout.LayoutParams(-1,dp(62)));return b;
    }
    private void guide(String icon,String title,String body,int slot){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(14));card.setBackgroundColor(Color.WHITE);
        TextView visual=text(icon,42,true);visual.setGravity(Gravity.CENTER);card.addView(visual,new LinearLayout.LayoutParams(-1,dp(68)));
        card.addView(text(title,17,true));
        card.addView(text(body,14,false));
        previews[slot]=new ImageView(this);previews[slot].setScaleType(ImageView.ScaleType.CENTER_CROP);previews[slot].setBackgroundColor(Color.rgb(238,243,241));
        card.addView(previews[slot],new LinearLayout.LayoutParams(-1,dp(185)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(10));root.addView(card,lp);
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(30));root.setBackgroundColor(Color.rgb(247,250,249));scroll.addView(root);setContentView(scroll);

        TextView title=text("4-Photo Profile Verification",27,true);title.setGravity(Gravity.CENTER);root.addView(title);
        root.addView(text("18+ ONLY — male or female. Your main photo must be taken now with the camera. Photos 2–4 may come from your gallery, but all four must clearly show the same person. Your profile is not marked verified until the complete set passes verification.",15,false));

        status=text("Status: start with Photo 1.",15,true);root.addView(status);

        guide("🙂","Photo 1 — MAIN PHOTO • CAMERA REQUIRED","Look straight at the camera. Good light, one face only, no sunglasses, no heavy filter. This becomes your verified main profile photo.",0);
        guide("🙂↙️","Photo 2 — GALLERY","Choose a clear recent photo of the same person. A slight left angle is fine; face must still be easy to recognize.",1);
        guide("↘️🙂","Photo 3 — GALLERY","Choose another clear recent photo of the same person. A slight right angle is fine; avoid masks and strong filters.",2);
        guide("😊","Photo 4 — GALLERY","Choose one more clear recent photo of the same person. Natural smile is fine. No group photo; one visible face only.",3);

        Button camera=button("1. Take Main Photo with Camera",true);camera.setOnClickListener(v->startSetThenCamera());
        Button gallery=button("2. Choose Exactly 3 Gallery Photos",true);gallery.setOnClickListener(v->chooseRemainingPhotos());
        Button submit=button("3. Upload 4 Photos & Verify Profile",true);submit.setOnClickListener(v->uploadAll());
        Button refresh=button("Refresh Verification Status",false);refresh.setOnClickListener(v->loadStatus());
        Button back=button("Back",false);back.setOnClickListener(v->finish());
    }

    private void startSetThenCamera(){
        if(!AzureAuthManager.hasAccount(this)){status.setText("Status: please sign in with Azure again.");return;}
        AzureApiClient.post("/photo-verification/start","{}",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    verificationSetId=new org.json.JSONObject(body).optJSONObject("set").optString("id","");
                    if(verificationSetId.isEmpty()){status.setText("Status: verification set could not start.");return;}
                    takeCameraPhoto();
                }catch(Exception e){status.setText("Status: verification set could not start.");}
            });}
            public void err(String m){runOnUiThread(()->status.setText(m!=null&&m.contains("AGE_18_PLUS_REQUIRED")?"Status: ❌ Verification is only for age 18+.":"Status: could not start 4-photo verification."));}
        });
    }

    private void takeCameraPhoto(){
        Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(i.resolveActivity(getPackageManager())==null){status.setText("Status: camera is unavailable.");return;}
        startActivityForResult(i,CAMERA_FIRST);
    }

    private void chooseRemainingPhotos(){
        if(cameraBitmap==null||verificationSetId.isEmpty()){status.setText("Status: take Photo 1 with the camera first.");return;}
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,GALLERY_REMAINING);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null)return;
        if(requestCode==CAMERA_FIRST&&data.getExtras()!=null&&data.getExtras().get("data") instanceof Bitmap){
            cameraBitmap=(Bitmap)data.getExtras().get("data");previews[0].setImageBitmap(cameraBitmap);galleryUris.clear();
            for(int i=1;i<4;i++)previews[i].setImageDrawable(null);
            status.setText("Status: main camera photo captured. Now choose exactly 3 gallery photos of the same person.");
            return;
        }
        if(requestCode==GALLERY_REMAINING){
            galleryUris.clear();
            if(data.getClipData()!=null){
                int count=Math.min(3,data.getClipData().getItemCount());
                for(int i=0;i<count;i++)galleryUris.add(data.getClipData().getItemAt(i).getUri());
            }else if(data.getData()!=null)galleryUris.add(data.getData());
            for(int i=1;i<4;i++)previews[i].setImageDrawable(null);
            for(int i=0;i<galleryUris.size();i++)previews[i+1].setImageURI(galleryUris.get(i));
            status.setText("Status: "+galleryUris.size()+"/3 gallery photos selected. All must be the same person as Photo 1.");
        }
    }

    private void uploadAll(){
        if(verificationSetId.isEmpty()){status.setText("Status: start again with Photo 1.");return;}
        if(cameraBitmap==null){status.setText("Status: Photo 1 camera image is required.");return;}
        if(galleryUris.size()!=3){status.setText("Status: exactly 3 gallery photos are required.");return;}
        status.setText("Status: securely uploading 4 photos to Azure…");
        ByteArrayOutputStream out=new ByteArrayOutputStream();cameraBitmap.compress(Bitmap.CompressFormat.JPEG,92,out);
        uploadSlot(1,out.toByteArray(),"camera",()->uploadGallery(0));
    }

    private void uploadGallery(int index){
        if(index>=3){submitSet();return;}
        try{
            Uri uri=galleryUris.get(index);InputStream in=getContentResolver().openInputStream(uri);
            if(in==null){status.setText("Status: could not read gallery Photo "+(index+2)+".");return;}
            ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int total=0,n;
            while((n=in.read(buffer))!=-1){total+=n;if(total>MAX_BYTES){in.close();status.setText("Status: Photo "+(index+2)+" is larger than 4 MB.");return;}out.write(buffer,0,n);}
            in.close();uploadSlot(index+2,out.toByteArray(),"gallery",()->uploadGallery(index+1));
        }catch(Exception e){status.setText("Status: gallery Photo "+(index+2)+" could not be prepared.");}
    }

    private void uploadSlot(int slot,byte[] bytes,String source,Runnable next){
        if(bytes.length==0||bytes.length>MAX_BYTES){status.setText("Status: Photo "+slot+" size is invalid.");return;}
        AzureApiClient.multipart("/photo-verification/"+verificationSetId+"/photos/"+slot,"photo","photo_"+slot+".jpg","image/jpeg",bytes,
            new String[]{"source"},new String[]{source},new AzureApiClient.Callback(){
                public void ok(int code,String body){runOnUiThread(()->{status.setText("Status: Photo "+slot+"/4 uploaded securely.");next.run();});}
                public void err(String m){runOnUiThread(()->status.setText("Status: Photo "+slot+" upload failed. "+m));}
            });
    }

    private void submitSet(){
        status.setText("Status: checking all 4 photos…");
        AzureApiClient.post("/photo-verification/"+verificationSetId+"/submit","{}",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    org.json.JSONObject o=new org.json.JSONObject(body);String s=o.optString("status","pending");
                    if("approved".equals(s))status.setText("Status: ✅ Profile photo verification approved. Main photo is verified.");
                    else status.setText("Status: 4 photos submitted. Verification is pending authorized same-person review.");
                    setResult(RESULT_OK);
                }catch(Exception e){status.setText("Status: 4 photos submitted for verification.");}
            });}
            public void err(String m){runOnUiThread(()->{
                if(m!=null&&m.contains("PHOTOS_NOT_SAME_PERSON"))status.setText("Status: ❌ photos did not verify as the same person. Please upload 4 matching photos.");
                else if(m!=null&&m.contains("AGE_18_PLUS_REQUIRED"))status.setText("Status: ❌ Verification is only for age 18+.");
                else status.setText("Status: verification submission failed. "+m);
            });}
        });
    }

    private void loadStatus(){
        if(!AzureAuthManager.hasAccount(this))return;
        AzureApiClient.get("/photo-verification/status",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    org.json.JSONObject o=new org.json.JSONObject(body);boolean verified=o.optBoolean("photoVerified",false);
                    org.json.JSONObject set=o.optJSONObject("set");String s=set==null?"not started":set.optString("status","not started");
                    status.setText(verified?"Status: ✅ Profile photo verified":"Status: "+s+" — 4 matching photos are required for verification.");
                }catch(Exception ignored){}
            });}
            public void err(String m){}
        });
    }
}
