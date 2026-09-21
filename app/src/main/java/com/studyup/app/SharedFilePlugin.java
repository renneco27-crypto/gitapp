package com.studyup.app;

import android.content.Intent;
import android.net.Uri;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "SharedFile")
public class SharedFilePlugin extends Plugin {
    private JSArray pendingFiles = null;

    public void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        JSArray files = new JSArray();

        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (uri != null) files.put(uri.toString());
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            java.util.ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (uris != null) for (Uri u : uris) files.put(u.toString());
        } else return;

        if (files.length() == 0) return;

        JSObject ret = new JSObject();
        ret.put("files", files);
        notifyListeners("sharedFiles", ret, true);
        pendingFiles = files;
    }

    @PluginMethod
    public void getPendingFiles(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("files", pendingFiles != null ? pendingFiles : new JSArray());
        call.resolve(ret);
        // Don't null it out immediately — let retries get it too.
        // Clear after a short delay so a later cold-launch doesn't pick up stale files.
        if (pendingFiles != null) {
            final JSArray snapshot = pendingFiles;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (pendingFiles == snapshot) pendingFiles = null;
            }, 10_000);
        }
    }
}