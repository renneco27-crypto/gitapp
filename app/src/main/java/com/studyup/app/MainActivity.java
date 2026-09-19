package com.studyup.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(GoogleAuthPlugin.class);
        registerPlugin(OneSignalPlugin.class);
        super.onCreate(savedInstanceState);
        
        // Initialize OneSignal SDK safely
        try {
            OneSignalManager.getInstance().initialize(this);
        } catch (Exception e) {
            android.util.Log.e("StudyUp", "Error initializing OneSignalManager", e);
        }
    }
}

