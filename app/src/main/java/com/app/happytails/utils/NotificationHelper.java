package com.app.happytails.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.app.happytails.R;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Locale;
import java.util.Random;

public class NotificationHelper {
    private static final String CHANNEL_ID = "chat_messages";
    private static final String INFO_CHANNEL_ID = "info_messages";
    private static final int NOTIFICATION_ID = 1;

    // Initialize notification channels (for chat and info)
    public static void initNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chatChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chat Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chatChannel.setDescription("Notifications for chat messages");

            NotificationChannel infoChannel = new NotificationChannel(
                    INFO_CHANNEL_ID,
                    "Info Messages",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            infoChannel.setDescription("General info notifications");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(chatChannel);
            manager.createNotificationChannel(infoChannel);
        }
    }

    // Show a chat message notification
    public static void showChatNotification(Context context, String title, String message, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(notificationId, builder.build());
    }

    // Show a general info notification
    public static void showInfoNotification(Context context, String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, INFO_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(new Random().nextInt(10000), builder.build());
    }

    // Handle Firebase message (for chat)
    public static void handleFirebaseMessage(Context context, com.google.firebase.messaging.RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() != null) {
            showChatNotification(
                    context,
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody(),
                    NOTIFICATION_ID
            );
        }
    }
}