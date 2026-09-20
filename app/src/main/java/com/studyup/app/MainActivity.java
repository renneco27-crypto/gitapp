package com.studyup.app;

import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
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
        super.onCreate(savedInstanceState);

        offlineInterceptor = new OfflineResourceInterceptor(this);

        // Attach offline resource interceptor to Capacitor's BridgeWebViewClient
        if (getBridge() != null && getBridge().getWebView() != null) {
            getBridge().getWebView().setWebViewClient(new BridgeWebViewClient(getBridge()) {
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
}

