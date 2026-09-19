package com.studyup.app;

import android.content.Context;
import android.util.Log;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.user.subscriptions.IPushSubscriptionObserver;
import com.onesignal.user.subscriptions.PushSubscriptionChangedState;
import com.onesignal.Continue;

/**
 * Centralized OneSignal Manager
 * Wraps all OneSignal SDK interactions for the app safely
 */
public class OneSignalManager {
    private static OneSignalManager instance;
    private static final String TAG = "OneSignalManager";
    private static final String ONESIGNAL_APP_ID = "2823b83c-3776-4c25-9f9b-62174fe6c13b";
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
        try {
            appContext = context.getApplicationContext();
            
            // Set log level for debugging
            OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
            
            // Initialize OneSignal
            OneSignal.initWithContext(appContext, ONESIGNAL_APP_ID);
            
            // Set up push subscription observer for logging without crashing UI
            setupPushSubscriptionObserver();

            // Request permission safely via OneSignal v5 API
            OneSignal.getNotifications().requestPermission(true, Continue.none());
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize OneSignal", e);
        }
    }

    /**
     * Login user with external ID (typically email)
     */
    public void login(String externalId) {
        if (externalId != null && !externalId.isEmpty()) {
            try {
                OneSignal.login(externalId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to login user in OneSignal", e);
            }
        }
    }

    /**
     * Logout current user
     */
    public void logout() {
        try {
            OneSignal.logout();
        } catch (Exception e) {
            Log.e(TAG, "Failed to logout OneSignal user", e);
        }
    }

    /**
     * Set user email for notifications
     */
    public void setEmail(String email) {
        if (email != null && !email.isEmpty()) {
            try {
                OneSignal.getUser().addEmail(email);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set email in OneSignal", e);
            }
        }
    }

    /**
     * Set user tag for targeting
     */
    public void setTag(String key, String value) {
        if (key != null && !key.isEmpty() && value != null) {
            try {
                OneSignal.getUser().addTag(key, value);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set tag in OneSignal", e);
            }
        }
    }

    /**
     * Set up push subscription observer
     */
    private static void setupPushSubscriptionObserver() {
        try {
            IPushSubscriptionObserver observer = new IPushSubscriptionObserver() {
                @Override
                public void onPushSubscriptionChange(PushSubscriptionChangedState state) {
                    if (state != null && state.getCurrent() != null) {
                        Log.i(TAG, "OneSignal Push Subscription ID: " + state.getCurrent().getId());
                    }
                }
            };
            pushSubscriptionObserver = observer;
            OneSignal.getUser().getPushSubscription().addObserver(observer);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set up push subscription observer", e);
        }
    }

    /**
     * Request push notification permission
     */
    public void requestPushPermission() {
        try {
            OneSignal.getNotifications().requestPermission(true, Continue.none());
        } catch (Exception e) {
            Log.e(TAG, "Failed to request push permission", e);
        }
    }

    /**
     * Set log level for debugging
     */
    public void setLogLevel(LogLevel level) {
        OneSignal.getDebug().setLogLevel(level);
    }
}
