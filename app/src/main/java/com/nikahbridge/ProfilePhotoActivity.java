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
import android.graphics.Color;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

/**
 * Real user profile-photo flow.
 * Photos are selected from the device, uploaded to Firebase Storage, and the
 * resulting URL is stored on the authenticated user's Firestore profile.
 * No bundled/sample/fake profile photos are used.
 *
 * The original single-photo flow remains available. The new four-photo flow
 * is additive and camera-first; the first image must be captured by camera and
 * the remaining three may be selected from the gallery.
 */
public class ProfilePhotoActivity extends Activity {
    private static final int PICK_IMAGE = 7101;
    private static final long MAX_BYTES = 5L * 1024L * 1024L;

    private ImageView preview;
    private TextView status;
    private Uri selected;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 32);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(247,250,249));
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("Real Profile Photo");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(30,45,41));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 70));

        status = new TextView(this);
        status.setText("Choose your own genuine photo. No demo or stock profile photos are used.");
        status.setTextSize(16);
        status.setTextColor(Color.rgb(95,108,103));
        root.addView(status, new LinearLayout.LayoutParams(-1, 90));

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(preview, new LinearLayout.LayoutParams(-1, 420));

        Button choose = new Button(this);
        choose.setText("Choose Real Photo");
        root.addView(choose, new LinearLayout.LayoutParams(-1, 62));

        Button upload = new Button(this);
        upload.setText("Upload Securely");
        root.addView(upload, new LinearLayout.LayoutParams(-1, 62));

        Button four = new Button(this);
        four.setText("Camera-First 4 Photos");
        root.addView(four, new LinearLayout.LayoutParams(-1, 62));
        four.setOnClickListener(v -> startActivity(new Intent(this, RealFourPhotoActivity.class)));

        Button genderMatches = new Button(this);
        genderMatches.setText("View Gender-Filtered Nikah Matches");
        root.addView(genderMatches, new LinearLayout.LayoutParams(-1, 62));
        genderMatches.setOnClickListener(v -> startActivity(new Intent(this, GenderFilteredMatchesActivity.class)));

        Button back = new Button(this);
        back.setText("Back");
        root.addView(back, new LinearLayout.LayoutParams(-1, 62));

        choose.setOnClickListener(v -> pickImage());
        upload.setOnClickListener(v -> uploadImage());
        back.setOnClickListener(v -> finish());
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, PICK_IMAGE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        selected = data.getData();
        long size = getSize(selected);
        if (size > MAX_BYTES) {
            selected = null;
            status.setText("Photo is larger than 5 MB. Choose a smaller image.");
            return;
        }
        try { getContentResolver().takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION); } catch (Exception ignored) {}
        preview.setImageURI(selected);
        status.setText("Photo selected. Tap Upload Securely.");
    }

    private long getSize(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null);
            if (c != null && c.moveToFirst()) return c.getLong(0);
        } catch (Exception ignored) {} finally { if (c != null) c.close(); }
        return 0L;
    }

    private void uploadImage() {
        if (selected == null) { status.setText("Choose a real photo first."); return; }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { status.setText("Please sign in again."); return; }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("profilePhotos").child(uid).child("profile.jpg");
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpeg").build();

        status.setText("Uploading securely…");
        ref.putFile(selected, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(url -> {
                    Map<String,Object> update = new HashMap<>();
                    update.put("photoUrl", url.toString());
                    update.put("photoUpdatedAt", FieldValue.serverTimestamp());
                    update.put("photoPresent", true);
                    FirebaseFirestore.getInstance().collection("users").document(uid)
                            .set(update, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(v -> { status.setText("Real profile photo saved securely."); setResult(RESULT_OK); })
                            .addOnFailureListener(e -> status.setText("Photo uploaded, but profile update failed. Please retry."));
                })
                .addOnFailureListener(e -> status.setText("Photo upload failed. Check your connection and try again."));
    }
}
