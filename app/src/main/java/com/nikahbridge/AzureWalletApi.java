package com.nikahbridge;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AzureWalletApi {
    private static final String BASE_URL = "https://bestnikahbredge-prod-fn-dkf3ake6d8gsg7cw.eastus-01.azurewebsites.net/api";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    interface Callback { void onSuccess(String body); void onError(String message); }

    static void get(String path, Callback callback) { request("GET", path, null, callback); }
    static void post(String path, String json, Callback callback) { request("POST", path, json, callback); }

    private static void request(String method, String path, String json, Callback callback) {
        AzureAuthManager.acquireToken(new AzureAuthManager.Callback() {
            @Override public void ok(String token) {
                EXECUTOR.execute(() -> {
                    HttpURLConnection c = null;
                    try {
                        c = (HttpURLConnection)new URL(BASE_URL + path).openConnection();
                        c.setRequestMethod(method);
                        c.setConnectTimeout(15000);
                        c.setReadTimeout(20000);
                        c.setRequestProperty("Authorization", "Bearer " + token);
                        c.setRequestProperty("Accept", "application/json");
                        if (json != null) {
                            c.setDoOutput(true);
                            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                            try (OutputStream out = c.getOutputStream()) { out.write(json.getBytes(StandardCharsets.UTF_8)); }
                        }
                        int code = c.getResponseCode();
                        InputStream stream = code >= 400 ? c.getErrorStream() : c.getInputStream();
                        String body = read(stream);
                        if (code >= 200 && code < 300) callback.onSuccess(body);
                        else callback.onError("HTTP " + code + (body.isEmpty() ? "" : ": " + body));
                    } catch (Exception e) { callback.onError(e.getMessage() == null ? "NETWORK_ERROR" : e.getMessage()); }
                    finally { if (c != null) c.disconnect(); }
                });
            }
            @Override public void err(String message) { callback.onError(message); }
        });
    }

    private static String read(InputStream in) throws Exception {
        if (in == null) return "";
        StringBuilder b = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line; while ((line = r.readLine()) != null) b.append(line);
        }
        return b.toString();
    }
}