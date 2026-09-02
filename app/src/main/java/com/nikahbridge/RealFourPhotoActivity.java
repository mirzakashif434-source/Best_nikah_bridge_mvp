package com.nikahbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Additive camera-first four-photo flow.
 * Photo 1 MUST be captured with the device camera. Photos 2-4 may be selected
 * from the device gallery. Every image is uploaded to Firebase Storage under
 * the authenticated UID; no bundled, stock, placeholder or demo photos exist.
 * The existing ProfilePhotoActivity remains untouched.
 */
public class RealFourPhotoActivity extends Activity {
    private static final int CAMERA_FIRST = 8401;
    private static final int GALLERY_REMAINING = 8402;
    private static final int MAX_BYTES = 5 * 1024 * 1024;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private LinearLayout root;
    private TextView status;
    private final ArrayList<Uri> galleryUris = new ArrayList<>();
    private Bitmap cameraBitmap;
    private ImageView[] previews;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        build();
    }

    private void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 22, 20, 30);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(247, 250, 249));
        setContentView(root);

        TextView title = new TextView(this);
        title.setText("Genuine 4-Photo Profile");
        title.setTextSize(27);
        title.setTextColor(Color.rgb(30,45,41));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 70));

        TextView intro = new TextView(this);
        intro.setText("Photo 1 must be taken now with your camera. Photos 2–4 can come from your gallery. This helps reduce copied or misleading profiles.");
        intro.setTextSize(16);
        intro.setTextColor(Color.rgb(95,108,103));
        root.addView(intro, new LinearLayout.LayoutParams(-1, 100));

        status = new TextView(this);
        status.setText("Start with the real camera photo.");
        status.setTextSize(15);
        status.setTextColor(Color.rgb(30,45,41));
        root.addView(status, new LinearLayout.LayoutParams(-1, 60));

        previews = new ImageView[4];
        for (int i = 0; i < 4; i++) {
            previews[i] = new ImageView(this);
            previews[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
            previews[i].setBackgroundColor(Color.WHITE);
            TextView label = new TextView(this);
            label.setText("Photo " + (i + 1) + (i == 0 ? " — CAMERA REQUIRED" : " — GALLERY"));
            label.setTextSize(14);
            label.setTextColor(Color.rgb(30,45,41));
            root.addView(label, new LinearLayout.LayoutParams(-1, 42));
            root.addView(previews[i], new LinearLayout.LayoutParams(-1, 210));
        }

        Button camera = button("1. Take Photo 1 with Camera", true);
        camera.setOnClickListener(v -> takeCameraPhoto());
        Button gallery = button("2. Choose Photos 2–4 from Gallery", true);
        gallery.setOnClickListener(v -> chooseRemainingPhotos());
        Button upload = button("3. Upload All 4 Securely", true);
        upload.setOnClickListener(v -> uploadAll());
        Button back = button("Back", false);
        back.setOnClickListener(v -> finish());
    }

    private Button button(String text, boolean filled) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(filled ? Color.WHITE : Color.rgb(18,103,82));
        b.setBackgroundColor(filled ? Color.rgb(18,103,82) : Color.WHITE);
        root.addView(b, new LinearLayout.LayoutParams(-1, 62));
        return b;
    }

    private void takeCameraPhoto() {
        if (auth.getCurrentUser() == null) {
            status.setText("Please sign in again.");
            return;
        }
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) == null) {
            status.setText("No camera app is available on this device.");
            return;
        }
        startActivityForResult(i, CAMERA_FIRST);
    }

    private void chooseRemainingPhotos() {
        if (cameraBitmap == null) {
            status.setText("Photo 1 must be captured with the camera first.");
            return;
        }
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, GALLERY_REMAINING);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == CAMERA_FIRST && data.getExtras() != null) {
            Object value = data.getExtras().get("data");
            if (value instanceof Bitmap) {
                cameraBitmap = (Bitmap) value;
                previews[0].setImageBitmap(cameraBitmap);
                galleryUris.clear();
                status.setText("Camera Photo 1 captured. Now choose up to 3 gallery photos.");
            }
            return;
        }
        if (requestCode == GALLERY_REMAINING) {
            galleryUris.clear();
            if (data.getClipData() != null) {
                int count = Math.min(3, data.getClipData().getItemCount());
                for (int i = 0; i < count; i++) galleryUris.add(data.getClipData().getItemAt(i).getUri());
            } else if (data.getData() != null) {
                galleryUris.add(data.getData());
            }
            for (int i = 1; i < 4; i++) {
                previews[i].setImageDrawable(null);
            }
            for (int i = 0; i < galleryUris.size(); i++) previews[i + 1].setImageURI(galleryUris.get(i));
            status.setText(galleryUris.size() + " genuine gallery photo(s) selected. Select up to 3, then upload.");
        }
    }

    private void uploadAll() {
        if (auth.getCurrentUser() == null) {
            status.setText("Please sign in again.");
            return;
        }
        if (cameraBitmap == null) {
            status.setText("Photo 1 is required and must come from the camera.");
            return;
        }
        if (galleryUris.size() != 3) {
            status.setText("Please select exactly 3 gallery photos for Photos 2–4.");
            return;
        }
        status.setText("Uploading 4 real photos securely…");
        uploadCameraThenGallery(0);
    }

    private void uploadCameraThenGallery(int index) {
        if (index == 0) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            cameraBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            uploadBytes(out.toByteArray(), 0, () -> uploadCameraThenGallery(1));
            return;
        }
        if (index <= 3) {
            uploadUri(galleryUris.get(index - 1), index, () -> uploadCameraThenGallery(index + 1));
            return;
        }
        saveProfileUrls();
    }

    private void uploadBytes(byte[] bytes, int slot, Runnable next) {
        if (bytes.length > MAX_BYTES) {
            status.setText("Camera photo is larger than 5 MB. Please retake it.");
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        StorageReference ref = storage.getReference().child("profilePhotos").child(uid).child("photo_" + (slot + 1) + ".jpg");
        StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpeg").build();
        ref.putBytes(bytes, metadata).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) throw task.getException();
            return ref.getDownloadUrl();
        }).addOnSuccessListener(url -> { urls[slot] = url.toString(); next.run(); })
          .addOnFailureListener(e -> status.setText("Photo " + (slot + 1) + " upload failed. No fake photo was added."));
    }

    private final String[] urls = new String[4];

    private void uploadUri(Uri uri, int slot, Runnable next) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) { status.setText("Could not open gallery photo " + (slot + 1) + "."); return; }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0, read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) { in.close(); status.setText("Gallery photo " + (slot + 1) + " is larger than 5 MB."); return; }
                out.write(buffer, 0, read);
            }
            in.close();
            StorageReference ref = storage.getReference().child("profilePhotos").child(auth.getCurrentUser().getUid()).child("photo_" + (slot + 1) + ".jpg");
            StorageMetadata metadata = new StorageMetadata.Builder().setContentType("image/jpeg").build();
            ref.putBytes(out.toByteArray(), metadata).continueWithTask(task -> {
                if (!task.isSuccessful() && task.getException() != null) throw task.getException();
                return ref.getDownloadUrl();
            }).addOnSuccessListener(url -> { urls[slot] = url.toString(); next.run(); })
              .addOnFailureListener(e -> status.setText("Photo " + (slot + 1) + " upload failed. No fake photo was added."));
        } catch (Exception e) {
            status.setText("Gallery photo " + (slot + 1) + " could not be read.");
        }
    }

    private void saveProfileUrls() {
        String uid = auth.getCurrentUser().getUid();
        ArrayList<String> list = new ArrayList<>();
        for (String url : urls) list.add(url);
        Map<String,Object> update = new HashMap<>();
        update.put("photoUrls", list);
        update.put("photoUrl", urls[0]);
        update.put("photoPresent", true);
        update.put("photoCount", 4);
        update.put("cameraFirstPhoto", true);
        update.put("photoUpdatedAt", FieldValue.serverTimestamp());
        db.collection("users").document(uid).set(update, SetOptions.merge())
          .addOnSuccessListener(v -> { status.setText("All 4 genuine photos are saved securely."); setResult(RESULT_OK); })
          .addOnFailureListener(e -> status.setText("Photos uploaded but profile save failed. Please retry."));
    }
}
