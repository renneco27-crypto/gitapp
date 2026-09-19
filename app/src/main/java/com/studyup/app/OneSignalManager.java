package com.studyup.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.user.subscriptions.IPushSubscriptionObserver;
import com.onesignal.user.subscriptions.PushSubscriptionChangedState;
import com.onesignal.Continue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized OneSignal Manager
 * Wraps all OneSignal SDK interactions for the app
 */
public class OneSignalManager {
    private static OneSignalManager instance;
    private static final String ONESIGNAL_APP_ID = "2823b83c-3776-4c25-9f9b-62174fe6c13b";
    private static final AtomicBoolean dialogShown = new AtomicBoolean(false);
    private static IPushSubscriptionObserver pushSubscriptionObserver;
    private static Context appContext;

    private OneSignalManager() {
        // Private constructor for singleton
    }

    public static synchronized OneSignalManager getInstance() {
        if (instance == null) {
            instance = new OneSignalManager();
        }
        return instance;
    }

    /**
     * Initialize OneSignal SDK
     * Should be called once at app startup
     */
    public void initialize(Context context) {
        appContext = context.getApplicationContext();
        
        // Set log level for debugging
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        
        // Initialize OneSignal
        OneSignal.initWithContext(appContext, ONESIGNAL_APP_ID);
        
        // Set up push subscription observer for verification
        setupPushSubscriptionObserver();
    }

    /**
     * Login user with external ID (typically email)
     */
    public void login(String externalId) {
        if (externalId != null && !externalId.isEmpty()) {
            OneSignal.login(externalId);
        }
    }

    /**
     * Logout current user
     */
    public void logout() {
        OneSignal.logout();
    }

    /**
     * Set user email for notifications
     */
    public void setEmail(String email) {
        if (email != null && !email.isEmpty()) {
            OneSignal.getUser().addEmail(email);
        }
    }

    /**
     * Set user tag for targeting
     */
    public void setTag(String key, String value) {
        if (key != null && !key.isEmpty() && value != null) {
            OneSignal.getUser().addTag(key, value);
        }
    }

    /**
     * Check if subscription ID is a real, server-assigned value
     */
    private static boolean isRegistered(String subscriptionId) {
        return subscriptionId != null && !subscriptionId.isEmpty() && !subscriptionId.startsWith("local-");
    }

    /**
     * Show integration complete dialog if device is registered
     */
    private static void maybeShowIntegrationCompleteDialog(String subscriptionId) {
        if (isRegistered(subscriptionId) && dialogShown.compareAndSet(false, true)) {
            new Handler(Looper.getMainLooper()).post(() -> {
                showIntegrationCompleteDialog();
            });
        }
    }

    /**
     * Set up push subscription observer
     */
    private static void setupPushSubscriptionObserver() {
        IPushSubscriptionObserver observer = new IPushSubscriptionObserver() {
            @Override
            public void onPushSubscriptionChange(PushSubscriptionChangedState state) {
                maybeShowIntegrationCompleteDialog(state.getCurrent().getId());
            }
        };
        pushSubscriptionObserver = observer;
        OneSignal.getUser().getPushSubscription().addObserver(observer);

        // Check current subscription ID immediately in case it's already assigned
        maybeShowIntegrationCompleteDialog(OneSignal.getUser().getPushSubscription().getId());
    }

    /**
     * Show integration complete dialog
     */
    private static void showIntegrationCompleteDialog() {
        if (appContext == null) return;
        
        new AlertDialog.Builder(appContext)
            .setTitle("Your OneSignal SDK integration is complete!")
            .setMessage(
                "You can now send Push Notifications & In-App Messages through OneSignal. " +
                "Tap below to enable push notifications."
            )
            .setPositiveButton("Got it", (dialog, which) -> {
                requestPushPermission();
            })
            .setCancelable(false)
            .show();
    }

    /**
     * Request push notification permission
     */
    private static void requestPushPermission() {
        OneSignal.getNotifications().requestPermission(true, Continue.none());
    }

    /**
     * Set log level for debugging
     */
    public void setLogLevel(LogLevel level) {
        OneSignal.getDebug().setLogLevel(level);
    }
}
