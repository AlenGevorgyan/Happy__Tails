package com.app.happytails.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.app.happytails.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String TAG = "FCMService";
    public static final String CHANNEL_ID_DEFAULT = "default_channel";
    public static final String CHANNEL_ID_CHAT = "chat_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleDataMessage(remoteMessage.getData());
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            handleNotification(
                remoteMessage.getNotification().getTitle(),
                remoteMessage.getNotification().getBody(),
                remoteMessage.getData()
            );
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        
        // Save the FCM token to Firestore
        saveTokenToFirestore(token);
    }

    private void handleDataMessage(Map<String, String> data) {
        String title = data.get("title");
        String message = data.get("message");
        String channelId = data.get("channel_id");
        String senderId = data.get("senderId");
        
        if (title == null) title = "Happy Tails";
        if (message == null) message = "You have a new notification!";
        if (channelId == null) channelId = CHANNEL_ID_DEFAULT;
        
        // Check if it's a chat message
        if (data.containsKey("senderId") || data.containsKey("chatRoomId")) {
            channelId = CHANNEL_ID_CHAT;
        }
        
        sendNotification(title, message, channelId, data);
    }

    private void handleNotification(String title, String body, Map<String, String> data) {
        if (title == null) title = "Happy Tails";
        if (body == null) body = "You have a new notification!";
        
        String channelId = CHANNEL_ID_DEFAULT;
        
        // Check if it's a chat message
        if (data != null && (data.containsKey("chatRoomId") || data.containsKey("senderId"))) {
            channelId = CHANNEL_ID_CHAT;
        }
        
        sendNotification(title, body, channelId, data);
    }

    private void sendNotification(String title, String messageBody, String channelId, Map<String, String> data) {
        // Create intent for MainActivity (no special navigation)
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // Add any extra data to the intent
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }
        
        // Create random request code to ensure PendingIntent is unique
        int requestCode = (int) System.currentTimeMillis();
        
        // Create PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 
            requestCode, 
            intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        // Create the notification channels
        createNotificationChannels();
        
        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        
        // Generate a unique notification ID
        int notificationId = requestCode;

        try {
            notificationManager.notify(notificationId, notificationBuilder.build());
            Log.d(TAG, "Notification sent with ID: " + notificationId);
        } catch (SecurityException e) {
            // This can happen on Android 13+ if notification permission is not granted
            Log.e(TAG, "No notification permission", e);
        }
    }

    /**
     * Create notification channels for Android O and above
     */
    public static void createNotificationChannels() {
        // Only needed for API level 26+ (Android O and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                (NotificationManager) ApplicationUtil.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Default channel
            NotificationChannel defaultChannel = new NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            defaultChannel.setDescription("Channel for general notifications");
            defaultChannel.enableLights(true);
            defaultChannel.enableVibration(true);
            
            // Chat channel
            NotificationChannel chatChannel = new NotificationChannel(
                CHANNEL_ID_CHAT,
                "Chat Notifications",
                NotificationManager.IMPORTANCE_HIGH
            );
            chatChannel.setDescription("Channel for chat messages");
            chatChannel.enableLights(true);
            chatChannel.enableVibration(true);
            
            // Register the channels
            notificationManager.createNotificationChannel(defaultChannel);
            notificationManager.createNotificationChannel(chatChannel);
        }
    }

    private void saveTokenToFirestore(String token) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
        }
    }
} 