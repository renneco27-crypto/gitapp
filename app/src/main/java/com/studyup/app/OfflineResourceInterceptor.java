package com.studyup.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class OfflineResourceInterceptor {

    private static final String TAG = "OfflineInterceptor";
    private final Context context;
    private final AssetManager assetManager;

    public OfflineResourceInterceptor(Context context) {
        this.context = context.getApplicationContext();
        this.assetManager = this.context.getAssets();
    }

    public boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            android.net.Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Exception e) {
            return false;
        }
    }

    public WebResourceResponse intercept(WebResourceRequest request) {
        if (request == null || request.getUrl() == null) return null;
        Uri uri = request.getUrl();
        String host = uri.getHost();

        // Only intercept requests for our domain or localhost
        if (host == null || (!host.contains("studyup.cloud-ip.cc") && !host.contains("localhost"))) {
            return null;
        }

        String path = uri.getPath();
        if (path == null) path = "/";

        // If online and it's an API request, let the live network handle it directly
        if (isOnline() && (path.startsWith("/api/") || path.startsWith("/auth/"))) {
            return null;
        }

        // If offline (or for static asset requests), try serving from packaged assets or disk
        return getLocalResponse(path);
    }

    private WebResourceResponse getLocalResponse(String path) {
        String cleanPath = path;
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        if (cleanPath.isEmpty()) {
            cleanPath = "index.html";
        }

        String mimeType = getMimeType(cleanPath);
        String encoding = "UTF-8";

        try {
            // 1. Check in packaged assets: "public/" + cleanPath
            String assetPath = "public/" + cleanPath;
            try {
                InputStream is = assetManager.open(assetPath);
                Map<String, String> responseHeaders = new HashMap<>();
                responseHeaders.put("Access-Control-Allow-Origin", "*");
                return new WebResourceResponse(mimeType, encoding, 200, "OK", responseHeaders, is);
            } catch (Exception notInAssets) {
                // Not a direct static file in assets
            }

            // 2. If it is a route navigation (e.g. /study/..., /feed, /leaderboard), serve the offline SPA shell
            if (!cleanPath.contains(".") || cleanPath.endsWith(".html")) {
                try {
                    InputStream is = assetManager.open("public/index.html");
                    Map<String, String> responseHeaders = new HashMap<>();
                    responseHeaders.put("Access-Control-Allow-Origin", "*");
                    return new WebResourceResponse("text/html", encoding, 200, "OK", responseHeaders, is);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving offline response for: " + path, e);
        }

        return null;
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html") || !path.contains(".")) return "text/html";
        if (path.endsWith(".js") || path.endsWith(".mjs")) return "application/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".woff2")) return "font/woff2";
        if (path.endsWith(".woff")) return "font/woff";
        if (path.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }
}
