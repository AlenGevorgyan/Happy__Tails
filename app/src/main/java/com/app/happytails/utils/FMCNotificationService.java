package com.app.happytails.utils;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FMCNotificationService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMessaging";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if the message contains a notification payload
//        if (remoteMessage.getNotification() != null) {
//            String title = remoteMessage.getNotification().getTitle();
//            String body = remoteMessage.getNotification().getBody();
//            Log.d(TAG, "Notification received: " + title + " - " + body);
//            // Display the notification using NotificationHelper
//            FcmHelper.sendPushNotification(getApplicationContext(), title, body);
//        }
//
//        // Handle data payload (if any)
//        if (remoteMessage.getData().size() > 0) {
//            Log.d(TAG, "Data Payload: " + remoteMessage.getData());
//        }
    }
}
