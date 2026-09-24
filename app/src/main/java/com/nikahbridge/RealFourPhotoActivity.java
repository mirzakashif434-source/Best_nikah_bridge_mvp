package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.core.content.FileProvider;
import java.io.File;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private TextView poseStatus;
    private ImageView[] previews=new ImageView[4];
    private PoseGuideView[] poseGuides=new PoseGuideView[4];
    private final ArrayList<View> poseCards=new ArrayList<>();
    private Button completeGenderButton;
    private Button cameraButton;
    private Button galleryButton;
    private Button submitButton;
    private String profileGender="";
    private final ArrayList<Uri> galleryUris=new ArrayList<>();
    private Uri cameraUri;
    private String verificationSetId="";
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}

    private class PoseGuideView extends View {
        private final int pose;
        private String gender="";
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        PoseGuideView(int pose){super(RealFourPhotoActivity.this);this.pose=pose;setBackgroundColor(Color.rgb(248,250,249));}
        void setGender(String g){gender=g==null?"":g.toLowerCase(java.util.Locale.US);invalidate();}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight(),cx=w/2f,cy=h*0.42f;
            p.setStrokeWidth(dp(3));p.setStyle(Paint.Style.STROKE);p.setColor(Color.rgb(18,103,82));
            if(!"male".equals(gender)&&!"female".equals(gender)){
                p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(16));p.setColor(Color.rgb(95,108,103));
                c.drawText("Complete profile gender to load pose guide",cx,h/2f,p);return;
            }
            float shift=pose==1?-w*0.08f:pose==2?w*0.08f:0f;
            float faceX=cx+shift;
            p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(224,190,160));c.drawOval(new RectF(faceX-dp(42),cy-dp(52),faceX+dp(42),cy+dp(48)),p);
            p.setColor(Color.rgb(50,45,42));
            if("female".equals(gender)){
                c.drawArc(new RectF(faceX-dp(54),cy-dp(68),faceX+dp(54),cy+dp(58)),185,170,true,p);
                p.setColor(Color.rgb(247,250,249));c.drawOval(new RectF(faceX-dp(36),cy-dp(44),faceX+dp(36),cy+dp(44)),p);
                p.setColor(Color.rgb(224,190,160));c.drawOval(new RectF(faceX-dp(34),cy-dp(42),faceX+dp(34),cy+dp(42)),p);
            } else {
                c.drawArc(new RectF(faceX-dp(44),cy-dp(62),faceX+dp(44),cy+dp(25)),190,160,true,p);
            }
            p.setColor(Color.DKGRAY);c.drawCircle(faceX-dp(13),cy-dp(8),dp(3),p);c.drawCircle(faceX+dp(13),cy-dp(8),dp(3),p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(2));p.setColor(Color.rgb(120,70,65));
            RectF mouth=new RectF(faceX-dp(16),cy+dp(4),faceX+dp(16),cy+dp(24));
            c.drawArc(mouth, pose==3?10:25, pose==3?160:130,false,p);
            p.setStyle(Paint.Style.FILL);p.setColor(Color.rgb(35,75,65));
            c.drawRoundRect(new RectF(cx-dp(80),h*0.70f,cx+dp(80),h*0.98f),dp(24),dp(24),p);
            p.setTextAlign(Paint.Align.CENTER);p.setTextSize(dp(14));p.setColor(Color.rgb(18,103,82));
            String label=pose==0?"FRONT":pose==1?"SLIGHT LEFT":pose==2?"SLIGHT RIGHT":"NATURAL SMILE";
            c.drawText(("female".equals(gender)?"FEMALE • ":"MALE • ")+label,cx,dp(22),p);
        }
    }

    private void setVerificationEnabled(boolean enabled){
        if(cameraButton!=null)cameraButton.setEnabled(enabled);
        if(galleryButton!=null)galleryButton.setEnabled(enabled);
        if(submitButton!=null)submitButton.setEnabled(enabled);
        if(completeGenderButton!=null)completeGenderButton.setVisibility(enabled?View.GONE:View.VISIBLE);
        for(View card:poseCards)card.setVisibility(enabled?View.VISIBLE:View.GONE);
    }

    private void loadPoseGender(){
        if(!AzureAuthManager.hasAccount(this)){
            profileGender="";
            poseStatus.setText("Sign in with Azure, then complete your profile gender to load the correct pose guide.");
            setVerificationEnabled(false);
            return;
        }
        AzureApiClient.get("/profile",new AzureApiClient.Callback(){
            public void ok(int code,String body){runOnUiThread(()->{
                try{
                    org.json.JSONObject p=new org.json.JSONObject(body).optJSONObject("profile");
                    profileGender=p==null?"":p.optString("gender","").trim().toLowerCase(java.util.Locale.US);
                    if(!"male".equals(profileGender)&&!"female".equals(profileGender)){
                        poseStatus.setText("Complete your profile gender first. Then this screen will load the correct male or female pose guide.");
                        profileGender="";
                        setVerificationEnabled(false);
                    }else{
                        poseStatus.setText(("female".equals(profileGender)?"Female":"Male")+" pose guide loaded from your real Azure profile.");
                        setVerificationEnabled(true);
                    }
                    for(PoseGuideView v:poseGuides)if(v!=null)v.setGender(profileGender);
                }catch(Exception e){
                    profileGender="";
                    poseStatus.setText("Complete your profile gender first. Then this screen will load the correct pose guide.");
                    setVerificationEnabled(false);
                }
            });}
            public void err(String m){runOnUiThread(()->{
                profileGender="";
                poseStatus.setText("Could not load profile gender from Azure. Open your profile, save gender, then return here.");
                setVerificationEnabled(false);
            });}
        });
    }

    @Override protected void onCreate(Bundle state){super.onCreate(state);AzureAuthManager.bindActivity(this);build();loadStatus();}
    @Override protected void onResume(){super.onResume();if(poseStatus!=null)loadPoseGender();}

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
    private void guide(String title,String body,int slot){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(12),dp(14),dp(14));card.setBackgroundColor(Color.WHITE);
        poseGuides[slot]=new PoseGuideView(slot);
        card.addView(poseGuides[slot],new LinearLayout.LayoutParams(-1,dp(210)));
        card.addView(text(title,17,true));
        card.addView(text(body,14,false));
        previews[slot]=new ImageView(this);previews[slot].setScaleType(ImageView.ScaleType.CENTER_CROP);previews[slot].setBackgroundColor(Color.rgb(238,243,241));
        card.addView(previews[slot],new LinearLayout.LayoutParams(-1,dp(185)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,dp(10));root.addView(card,lp);
        poseCards.add(card);
    }

    private void build(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(20),dp(18),dp(30));root.setBackgroundColor(Color.rgb(247,250,249));scroll.addView(root);setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(18),dp(20),dp(18),dp(30)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(scroll);

        TextView title=text("4-Photo Profile Verification",27,true);title.setGravity(Gravity.CENTER);root.addView(title);
        root.addView(text("18+ ONLY — male or female. Your main photo must be taken now with the camera. Photos 2–4 may come from your gallery, but all four must clearly show the same person. Your profile is not marked verified until the complete set passes verification.",15,false));

        status=text("Status: start with Photo 1.",15,true);root.addView(status);
        poseStatus=text("Loading your pose guide from your real profile…",14,false);root.addView(poseStatus);

        completeGenderButton=button("Complete Profile Gender",false);
        completeGenderButton.setOnClickListener(v->startActivity(new Intent(this,AzureHomeActivity.class)));
        completeGenderButton.setVisibility(View.GONE);

        guide("Photo 1 — MAIN PHOTO • CAMERA REQUIRED","Look straight at the camera. Good light, one face only, no sunglasses, no heavy filter. This becomes your verified main profile photo.",0);
        guide("Photo 2 — GALLERY","Choose a clear recent photo of the same person. Turn slightly to your left while keeping both eyes visible.",1);
        guide("Photo 3 — GALLERY","Choose another clear recent photo of the same person. Turn slightly to your right; avoid masks and strong filters.",2);
        guide("Photo 4 — GALLERY","Choose one more clear recent photo of the same person with a natural smile. No group photo; one visible face only.",3);

        cameraButton=button("1. Take Main Photo with Camera",true);cameraButton.setOnClickListener(v->startSetThenCamera());
        galleryButton=button("2. Choose Exactly 3 Gallery Photos",true);galleryButton.setOnClickListener(v->chooseRemainingPhotos());
        submitButton=button("3. Upload 4 Photos & Verify Profile",true);submitButton.setOnClickListener(v->uploadAll());
        setVerificationEnabled(false);
        loadPoseGender();
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
        try{
            File dir=new File(getCacheDir(),"verification-camera");
            if(!dir.exists()&&!dir.mkdirs()){status.setText("Status: camera storage is unavailable.");return;}
            File file=File.createTempFile("main-photo-", ".jpg", dir);
            cameraUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(i.resolveActivity(getPackageManager())==null){status.setText("Status: camera is unavailable.");return;}
            i.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i,CAMERA_FIRST);
        }catch(Exception e){status.setText("Status: camera could not start.");}
    }

    private void chooseRemainingPhotos(){
        if(cameraUri==null||verificationSetId.isEmpty()){status.setText("Status: take Photo 1 with the camera first.");return;}
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i,GALLERY_REMAINING);
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK||data==null)return;
        if(requestCode==CAMERA_FIRST&&cameraUri!=null){
            previews[0].setImageURI(cameraUri);galleryUris.clear();
            for(int i=1;i<4;i++)previews[i].setImageDrawable(null);
            status.setText("Status: full-resolution main camera photo captured. Now choose exactly 3 gallery photos of the same person.");
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
        if(cameraUri==null){status.setText("Status: Photo 1 camera image is required.");return;}
        if(galleryUris.size()!=3){status.setText("Status: exactly 3 gallery photos are required.");return;}
        status.setText("Status: securely uploading 4 photos to Azure…");
        try{
            byte[] bytes=readImageBytes(cameraUri,true);
            if(bytes==null){status.setText("Status: main camera photo could not be prepared.");return;}
            uploadSlot(1,bytes,"camera",()->uploadGallery(0));
        }catch(Exception e){status.setText("Status: main camera photo could not be prepared.");}
    }

    private void uploadGallery(int index){
        if(index>=3){submitSet();return;}
        try{
            Uri uri=galleryUris.get(index);
            byte[] bytes=readImageBytes(uri,false);
            if(bytes==null){status.setText("Status: Photo "+(index+2)+" is larger than 4 MB or could not be read.");return;}
            uploadSlot(index+2,bytes,"gallery",()->uploadGallery(index+1));
        }catch(Exception e){status.setText("Status: gallery Photo "+(index+2)+" could not be prepared.");}
    }

    private byte[] readImageBytes(Uri uri,boolean allowCompress)throws Exception{
        InputStream in=getContentResolver().openInputStream(uri);
        if(in==null)return null;
        ByteArrayOutputStream raw=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n,total=0;
        while((n=in.read(buffer))!=-1){total+=n;if(total>12*1024*1024){in.close();return null;}raw.write(buffer,0,n);}
        in.close();
        byte[] bytes=raw.toByteArray();
        if(bytes.length<=MAX_BYTES)return bytes;
        if(!allowCompress)return null;
        Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        if(bitmap==null)return null;
        int w=bitmap.getWidth(),h=bitmap.getHeight(),max=Math.max(w,h);
        if(max>2048){float s=2048f/max;Bitmap scaled=Bitmap.createScaledBitmap(bitmap,Math.max(1,Math.round(w*s)),Math.max(1,Math.round(h*s)),true);if(scaled!=bitmap)bitmap.recycle();bitmap=scaled;}
        for(int q=92;q>=60;q-=8){ByteArrayOutputStream out=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,q,out);if(out.size()<=MAX_BYTES){bitmap.recycle();return out.toByteArray();}}
        bitmap.recycle();return null;
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
