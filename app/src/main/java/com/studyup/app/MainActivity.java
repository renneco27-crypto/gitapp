package com.studyup.app;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.Toast;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

public class MainActivity extends BridgeActivity {

    private OfflineResourceInterceptor offlineInterceptor;
 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(GoogleAuthPlugin.class);
        registerPlugin(OneSignalPlugin.class);
        registerPlugin(NativeDeckStoragePlugin.class);
        registerPlugin(NativeTTSPlugin.class);
        registerPlugin(NativeSTTPlugin.class);
        registerPlugin(SharedFilePlugin.class);
        super.onCreate(savedInstanceState);

        // Process any share intent that launched this activity (delayed so Capacitor bridge is ready)
        final Intent launchIntent = getIntent();
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> handleShareIntent(launchIntent), 300);

        offlineInterceptor = new OfflineResourceInterceptor(this);

        // Attach offline resource interceptor and DownloadListener to Capacitor's WebView
        if (getBridge() != null && getBridge().getWebView() != null) {
            WebView webView = getBridge().getWebView();
            webView.setWebViewClient(new BridgeWebViewClient(getBridge()) {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    if (offlineInterceptor != null) {
                        WebResourceResponse offlineResponse = offlineInterceptor.intercept(request);
                        if (offlineResponse != null) {
                            return offlineResponse;
                        }
                    }
                    return super.shouldInterceptRequest(view, request);
                }
            });

            // Route web downloads directly to public Downloads folder with system notification
            webView.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                    try {
                        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                        if (mimetype != null && !mimetype.isEmpty()) {
                            request.setMimeType(mimetype);
                        }
                        String cookies = CookieManager.getInstance().getCookie(url);
                        if (cookies != null) {
                            request.addRequestHeader("cookie", cookies);
                        }
                        request.addRequestHeader("User-Agent", userAgent);
                        request.setDescription("Downloading to Downloads folder...");
                        String filename = URLUtil.guessFileName(url, contentDisposition, mimetype);
                        request.setTitle(filename);
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);

                        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                        if (dm != null) {
                            dm.enqueue(request);
                            Toast.makeText(getApplicationContext(), "Downloading " + filename + " to Downloads folder...", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("StudyUp", "Download failed: " + e.getMessage(), e);
                    }
                }
            });
        }

        // Intercept requests originating from Service Workers (Serwist/Workbox)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                android.webkit.ServiceWorkerController.getInstance().setServiceWorkerClient(new android.webkit.ServiceWorkerClient() {
                    @Override
                    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                        if (offlineInterceptor != null) {
                            WebResourceResponse offlineResponse = offlineInterceptor.intercept(request);
                            if (offlineResponse != null) {
                                return offlineResponse;
                            }
                        }
                        return super.shouldInterceptRequest(request);
                    }
                });
            } catch (Exception e) {
                android.util.Log.w("StudyUp", "ServiceWorkerController interceptor setup skipped: " + e.getMessage());
            }
        }

        // Initialize OneSignal SDK safely
        try {
            OneSignalManager.getInstance().initialize(this);
        } catch (Exception e) {
            android.util.Log.e("StudyUp", "Error initializing OneSignalManager", e);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleShareIntent(intent);
    }

    private void handleShareIntent(Intent intent) {
        if (intent == null) return;
        com.getcapacitor.PluginHandle handle = getBridge() != null ? getBridge().getPlugin("SharedFile") : null;
        if (handle == null) {
            android.util.Log.w("StudyUp", "SharedFile plugin not registered yet");
            return;
        }
        SharedFilePlugin plugin = (SharedFilePlugin) handle.getInstance();
        if (plugin == null) {
            android.util.Log.w("StudyUp", "SharedFile plugin instance null");
            return;
        }
        plugin.handleIntent(intent);
    }
}