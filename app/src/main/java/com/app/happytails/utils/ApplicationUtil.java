package com.app.happytails.utils;

import android.app.Application;
import android.content.Context;

/**
 * Utility class to provide application context throughout the app
 */
public class ApplicationUtil extends Application {
    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
        
        // Initialize notification channels
        FirebaseMessagingService.createNotificationChannels();
    }

    public static Context getAppContext() {
        return appContext;
    }
} 