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

        // Initialize OneSignal SDK safely
        try {
            OneSignalManager.getInstance().initialize(this);
        } catch (Exception e) {
            android.util.Log.e("StudyUp", "Error initializing OneSignalManager", e);
        }
    }
}

