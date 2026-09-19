package com.studyup.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.onesignal.OneSignal;

/**
 * Capacitor Plugin for OneSignal integration
 * Bridges JavaScript calls to native OneSignalManager
 */
@CapacitorPlugin(name = "OneSignalBridge")
public class OneSignalPlugin extends Plugin {

    @PluginMethod
    public void login(PluginCall call) {
        String externalId = call.getString("externalId");
        if (externalId == null || externalId.isEmpty()) {
            call.reject("externalId is required");
            return;
        }
        
        OneSignalManager.getInstance().login(externalId);
        call.resolve();
    }

    @PluginMethod
    public void logout(PluginCall call) {
        OneSignalManager.getInstance().logout();
        call.resolve();
    }

    @PluginMethod
    public void setEmail(PluginCall call) {
        String email = call.getString("email");
        if (email == null || email.isEmpty()) {
            call.reject("email is required");
            return;
        }
        
        OneSignalManager.getInstance().setEmail(email);
        call.resolve();
    }

    @PluginMethod
    public void setTag(PluginCall call) {
        String key = call.getString("key");
        String value = call.getString("value");
        
        if (key == null || key.isEmpty()) {
            call.reject("key is required");
            return;
        }
        
        OneSignalManager.getInstance().setTag(key, value);
        call.resolve();
    }

    @PluginMethod
    public void getSubscriptionId(PluginCall call) {
        String subscriptionId = OneSignal.getUser().getPushSubscription().getId();
        JSObject result = new JSObject();
        result.put("subscriptionId", subscriptionId);
        call.resolve(result);
    }
}
