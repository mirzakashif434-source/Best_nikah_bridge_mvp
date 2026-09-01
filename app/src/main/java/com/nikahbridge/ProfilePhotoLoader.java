package com.nikahbridge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads real Firebase Storage profile-photo URLs without blocking the UI thread. */
public final class ProfilePhotoLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private ProfilePhotoLoader() {}

    public static void load(String url, ImageView target) {
        if (url == null || url.trim().isEmpty()) return;
        EXECUTOR.execute(() -> {
            Bitmap bitmap = null;
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(15000);
                c.setInstanceFollowRedirects(true);
                c.connect();
                try (InputStream in = c.getInputStream()) { bitmap = BitmapFactory.decodeStream(in); }
                c.disconnect();
            } catch (Exception ignored) {}
            final Bitmap result = bitmap;
            if (result != null) MAIN.post(() -> target.setImageBitmap(result));
        });
    }
}
