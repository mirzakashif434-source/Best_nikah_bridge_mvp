package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;



/**
 * Real user profile-photo flow.
 * Photos are selected from the device and uploaded through the authenticated
 * Azure API to Azure Blob Storage with PostgreSQL metadata.
 * No bundled/sample/fake profile photos are used.
 *
 * The original single-photo flow remains available. The new four-photo flow
 * is additive and camera-first; the first image must be captured by camera and
 * the remaining three may be selected from the gallery.
 */
public class ProfilePhotoActivity extends Activity {
    private static final int PICK_IMAGE = 7101;
    private static final long MAX_BYTES = 4L * 1024L * 1024L;

    private ImageView preview;
    private TextView status;
    private Uri selected;

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    private Button styledButton(String label, boolean primary, LinearLayout parent) {
        Button b = primary ? Premium2030Ui.primary(this,label) : Premium2030Ui.secondary(this,label);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(6),0,dp(6));
        b.setMinHeight(dp(62));
        parent.addView(b,lp);
        return b;
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(32));
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(247,250,249));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackgroundColor(Color.rgb(247,250,249));
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2));setContentView(scroll);
        ViewCompat.setOnApplyWindowInsetsListener(scroll,(v,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left,bars.top,bars.right,0);
            root.setPadding(dp(24),dp(24),dp(24),dp(32)+bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(scroll);

        TextView title = new TextView(this);
        title.setText("Real Profile Photo");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(30,45,41));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(70)));

        status = new TextView(this);
        status.setText("Choose your own genuine photo. No demo or stock profile photos are used.");
        status.setTextSize(16);
        status.setTextColor(Color.rgb(95,108,103));
        root.addView(status, new LinearLayout.LayoutParams(-1, dp(90)));

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setVisibility(android.view.View.GONE);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(320)));

        Button choose = styledButton("Choose Real Photo", true, root);

        Button upload = styledButton("Upload to Azure Securely", true, root);
        upload.setEnabled(false);

        Button four = styledButton("Camera-First 4 Photos", false, root);
        four.setOnClickListener(v -> startActivity(new Intent(this, RealFourPhotoActivity.class)));

        Button genderMatches = styledButton("View Gender-Filtered Nikah Matches", false, root);
        genderMatches.setOnClickListener(v -> startActivity(new Intent(this, GenderFilteredMatchesActivity.class)));

        Button back = styledButton("Back", false, root);

        choose.setOnClickListener(v -> requireTermsBeforePhotoAction(this::pickImage));
        upload.setOnClickListener(v -> requireTermsBeforePhotoAction(() ->
                PremiumFeatureGate.require(this,"paid20Features","20 SAR Basic or higher",this::uploadImageAzure)));
        back.setOnClickListener(v -> finish());
    }

    private void requireTermsBeforePhotoAction(Runnable action) {
        if (!AzureAuthManager.hasAccount(this)) { status.setText("Please sign in with Azure again."); return; }
        AzureApiClient.get("/terms/status", new AzureApiClient.Callback() {
            @Override public void ok(int code, String body) {
                try {
                    org.json.JSONObject o = new org.json.JSONObject(body);
                    boolean accepted = o.optBoolean("termsAccepted", false);
                    int age = o.optInt("age", 0);
                    String current = o.optString("currentTermsVersion", "");
                    String version = o.optString("termsVersion", "");
                    runOnUiThread(() -> {
                        if (accepted && age >= 18 && current.equals(version)) action.run();
                        else {
                            status.setText("Please accept the current Terms & Community Guidelines first.");
                            startActivityForResult(new Intent(ProfilePhotoActivity.this, TermsAndCommunityGuidelinesActivity.class), 7201);
                        }
                    });
                } catch (Exception e) { runOnUiThread(() -> status.setText("Could not verify Azure content permissions.")); }
            }
            @Override public void err(String message) { runOnUiThread(() -> status.setText("Azure terms check failed: " + message)); }
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 7201 && resultCode == RESULT_OK) status.setText("Terms accepted. You can now choose or upload your real profile photo.");
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        selected = data.getData();
        long size = getSize(selected);
        if (size > MAX_BYTES) {
            selected = null;
            status.setText("Photo is larger than 4 MB. Choose a smaller image.");
            return;
        }
        try { getContentResolver().takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        preview.setImageURI(selected);
        preview.setVisibility(android.view.View.VISIBLE);
        Button uploadButton = findButtonByText("Upload to Azure Securely");
        if (uploadButton != null) uploadButton.setEnabled(true);
        status.setText("Photo selected. Tap Upload to Azure Securely.");
    }

    private Button findButtonByText(String label) {
        if (!(preview.getParent() instanceof LinearLayout)) return null;
        LinearLayout parent=(LinearLayout)preview.getParent();
        for(int i=0;i<parent.getChildCount();i++){
            android.view.View child=parent.getChildAt(i);
            if(child instanceof Button && label.contentEquals(((Button)child).getText())) return (Button)child;
        }
        return null;
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, PICK_IMAGE);
    }

    private long getSize(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null);
            if (c != null && c.moveToFirst()) return c.getLong(0);
        } catch (Exception ignored) {} finally { if (c != null) c.close(); }
        return 0L;
    }

    /** Primary production path: authenticated Azure API -> AI moderation -> Blob + PostgreSQL. */
    private void uploadImageAzure() {
        if (selected == null) { status.setText("Choose a real photo first."); return; }
        if (!AzureAuthManager.hasAccount(this)) { status.setText("Please sign in with Azure again."); return; }
        try {
            java.io.InputStream in = getContentResolver().openInputStream(selected);
            if (in == null) { status.setText("Could not read the selected photo."); return; }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int total = 0, read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) { in.close(); status.setText("Photo is larger than 4 MB."); return; }
                out.write(buffer, 0, read);
            }
            in.close();
            String mime = getContentResolver().getType(selected);
            if (mime == null || !(mime.equals("image/jpeg") || mime.equals("image/png") || mime.equals("image/webp"))) {
                status.setText("Unsupported photo type. Please choose JPG, PNG, or WebP.");
                return;
            }
            final String finalMime = mime;
            status.setText("Uploading real photo to Azure securely…");
            AzureApiClient.multipart("/photos","photo","profile."+finalMime.substring(finalMime.indexOf('/')+1),finalMime,out.toByteArray(),
                    new String[]{"visibility"},new String[]{"private"},new AzureApiClient.Callback(){
                @Override public void ok(int code,String body){runOnUiThread(()->{status.setText("Real profile photo saved in Azure.");setResult(RESULT_OK);});}
                @Override public void err(String message){runOnUiThread(()->status.setText("Azure photo upload failed: "+message));}
            });
        } catch(Exception e) { status.setText("Could not read the selected photo."); }
    }

}
