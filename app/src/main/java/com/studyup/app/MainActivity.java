package com.studyup.app;

import android.os.Bundle;
import android.webkit.PermissionRequest;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Bridge;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize OneSignal SDK
        OneSignalManager.getInstance().initialize(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Grant microphone access to the WebView for Web Speech API
        if (bridge != null && bridge.getWebView() != null) {
            bridge.getWebView().setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public void onPermissionRequest(PermissionRequest request) {
                    request.grant(request.getResources());
                }
            });
        }
    }
}

