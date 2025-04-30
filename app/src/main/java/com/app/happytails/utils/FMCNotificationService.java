package com.app.happytails.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FMCNotificationService extends FirebaseMessagingService {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.initNotificationChannel(this);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        NotificationHelper.handleFirebaseMessage(this, remoteMessage);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Сохранение токена...
    }
}
