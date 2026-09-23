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

/** Loads real profile photos. Azure match photos are fetched only through authenticated API paths. */
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

    public static void loadAzure(String apiPath, ImageView target, Runnable unavailable) {
        if (apiPath == null || apiPath.trim().isEmpty()) {
            if (unavailable != null) MAIN.post(unavailable);
            return;
        }
        AzureApiClient.getBytes(apiPath,new AzureApiClient.BinaryCallback(){
            public void ok(int code,String type,byte[] body){
                final Bitmap b=body==null?null:BitmapFactory.decodeByteArray(body,0,body.length);
                if(b!=null)MAIN.post(()->target.setImageBitmap(b));
                else if(unavailable!=null)MAIN.post(unavailable);
            }
            public void err(String message){if(unavailable!=null)MAIN.post(unavailable);}
        });
    }
}
