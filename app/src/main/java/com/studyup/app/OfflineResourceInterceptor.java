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

        // When online, let the live server handle ALL page navigations, APIs, and auth
        // Only intercept static chunk assets (_next/static/...) to speed up loading from local cache
        if (isOnline()) {
            if (!path.startsWith("/_next/") && !path.endsWith(".png") && !path.endsWith(".jpg") && 
                !path.endsWith(".svg") && !path.endsWith(".woff2") && !path.endsWith(".ico")) {
                return null; // LIVE ONLINE PASSTHROUGH
            }
        }

        // When offline (or for cached static assets), resolve locally
        return getLocalResponse(uri, path);
    }

    private WebResourceResponse getLocalResponse(Uri uri, String path) {
        String cleanPath = path;
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }
        if (cleanPath.isEmpty()) {
            cleanPath = "index.html";
        }

        String mimeType = getMimeType(cleanPath);
        String encoding = "UTF-8";
        boolean isRsc = (uri != null && uri.getQueryParameter("_rsc") != null) || path.endsWith(".rsc");

        try {
            // 0. Check for downloaded webapp updates in getFilesDir() first
            File dataDir = context.getFilesDir();
            File updatedAsset = new File(dataDir, "webapp/" + cleanPath);
            if (updatedAsset.exists() && updatedAsset.isFile()) {
                try {
                    InputStream is = new java.io.FileInputStream(updatedAsset);
                    Map<String, String> responseHeaders = new HashMap<>();
                    responseHeaders.put("Access-Control-Allow-Origin", "*");
                    return new WebResourceResponse(mimeType, encoding, 200, "OK", responseHeaders, is);
                } catch (Exception e) {}
            }

            // 1. Direct match in packaged assets: "public/" + cleanPath
            String assetPath = "public/" + cleanPath;
            try {
                InputStream is = assetManager.open(assetPath);
                Map<String, String> responseHeaders = new HashMap<>();
                responseHeaders.put("Access-Control-Allow-Origin", "*");
                return new WebResourceResponse(mimeType, encoding, 200, "OK", responseHeaders, is);
            } catch (Exception notInAssets) {
                // Not a direct static file in assets
            }

            // 2. Study routes: /study/{deckId}/...
            if (path.startsWith("/study/") || path.equals("/study")) {
                if (path.equals("/study") || path.equals("/study/")) {
                    try {
                        InputStream is = assetManager.open("public/index.html");
                        Map<String, String> responseHeaders = new HashMap<>();
                        responseHeaders.put("Access-Control-Allow-Origin", "*");
                        return new WebResourceResponse("text/html", encoding, 200, "OK", responseHeaders, is);
                    } catch (Exception ignored) {}
                }

                String sub = path.substring("/study/".length());
                if (sub.endsWith("/")) sub = sub.substring(0, sub.length() - 1);

                int slashIdx = sub.indexOf('/');
                String modePath = "";
                if (slashIdx != -1) {
                    modePath = sub.substring(slashIdx + 1);
                }

                String baseOfflineAsset;
                if (modePath.isEmpty() || modePath.equals(".rsc")) {
                    baseOfflineAsset = "public/study/_offline";
                } else {
                    if (modePath.endsWith(".rsc")) {
                        modePath = modePath.substring(0, modePath.length() - 4);
                    }
                    baseOfflineAsset = "public/study/_offline/" + modePath;
                }

                String targetAsset = isRsc ? (baseOfflineAsset + ".rsc") : (baseOfflineAsset + ".html");
                String responseMime = isRsc ? "text/x-component" : "text/html";

                try {
                    File localUpdate = new File(dataDir, "webapp/" + baseOfflineAsset.replace("public/", "") + (isRsc ? ".rsc" : ".html"));
                    InputStream is;
                    if (localUpdate.exists() && localUpdate.isFile()) {
                        is = new java.io.FileInputStream(localUpdate);
                    } else {
                        is = assetManager.open(targetAsset);
                    }
                    Map<String, String> responseHeaders = new HashMap<>();
                    responseHeaders.put("Access-Control-Allow-Origin", "*");
                    responseHeaders.put("Cache-Control", "no-cache");
                    return new WebResourceResponse(responseMime, encoding, 200, "OK", responseHeaders, is);
                } catch (Exception ex) {
                    try {
                        File localUpdateHtml = new File(dataDir, "webapp/" + baseOfflineAsset.replace("public/", "") + ".html");
                        InputStream is;
                        if (localUpdateHtml.exists() && localUpdateHtml.isFile()) {
                            is = new java.io.FileInputStream(localUpdateHtml);
                        } else {
                            is = assetManager.open(baseOfflineAsset + ".html");
                        }
                        Map<String, String> responseHeaders = new HashMap<>();
                        responseHeaders.put("Access-Control-Allow-Origin", "*");
                        return new WebResourceResponse("text/html", encoding, 200, "OK", responseHeaders, is);
                    } catch (Exception ex2) {
                        try {
                            File fallbackHtml = new File(dataDir, "webapp/study/_offline.html");
                            InputStream is;
                            if (fallbackHtml.exists() && fallbackHtml.isFile()) {
                                is = new java.io.FileInputStream(fallbackHtml);
                            } else {
                                is = assetManager.open("public/study/_offline.html");
                            }
                            Map<String, String> responseHeaders = new HashMap<>();
                            responseHeaders.put("Access-Control-Allow-Origin", "*");
                            return new WebResourceResponse("text/html", encoding, 200, "OK", responseHeaders, is);
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 3. Pre-rendered HTML / RSC routes: e.g. /leaderboard -> public/leaderboard.html or .rsc
            if (!cleanPath.contains(".")) {
                if (isRsc) {
                    try {
                        File localRsc = new File(dataDir, "webapp/" + cleanPath + ".rsc");
                        InputStream is;
                        if (localRsc.exists() && localRsc.isFile()) {
                            is = new java.io.FileInputStream(localRsc);
                        } else {
                            is = assetManager.open("public/" + cleanPath + ".rsc");
                        }
                        Map<String, String> responseHeaders = new HashMap<>();
                        responseHeaders.put("Access-Control-Allow-Origin", "*");
                        return new WebResourceResponse("text/x-component", encoding, 200, "OK", responseHeaders, is);
                    } catch (Exception ignored) {}
                }
                try {
                    File localHtml = new File(dataDir, "webapp/" + cleanPath + ".html");
                    InputStream is;
                    if (localHtml.exists() && localHtml.isFile()) {
                        is = new java.io.FileInputStream(localHtml);
                    } else {
                        is = assetManager.open("public/" + cleanPath + ".html");
                    }
                    Map<String, String> responseHeaders = new HashMap<>();
                    responseHeaders.put("Access-Control-Allow-Origin", "*");
                    return new WebResourceResponse("text/html", encoding, 200, "OK", responseHeaders, is);
                } catch (Exception notHtml) {}
            }

            // 4. Fallback for root / index rsc
            if (cleanPath.equals("index.html") && isRsc) {
                try {
                    File localIndexRsc = new File(dataDir, "webapp/index.rsc");
                    InputStream is;
                    if (localIndexRsc.exists() && localIndexRsc.isFile()) {
                        is = new java.io.FileInputStream(localIndexRsc);
                    } else {
                        is = assetManager.open("public/index.rsc");
                    }
                    Map<String, String> responseHeaders = new HashMap<>();
                    responseHeaders.put("Access-Control-Allow-Origin", "*");
                    return new WebResourceResponse("text/x-component", encoding, 200, "OK", responseHeaders, is);
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving offline response for: " + path, e);
        }

        return null;
    }

    private String getMimeType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".rsc")) return "text/x-component";
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
