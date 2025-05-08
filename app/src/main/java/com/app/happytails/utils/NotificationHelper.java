package com.app.happytails.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages:send";
    private static final String SERVER_KEY = "YOUR_SERVER_KEY"; // Replace with your FCM server key
    private static final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Check if notification permission is granted (for Android 13+)
     */
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Permission automatically granted on older Android versions
    }

    /**
     * Request notification permission (for Android 13+)
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    /**
     * Shows the app settings screen so the user can enable notifications manually
     */
    public static void openNotificationSettings(Context context) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Creates a permission request launcher for activities
     */
    public static ActivityResultLauncher<String> createPermissionLauncher(AppCompatActivity activity) {
        return activity.registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted");
                    // Initialize the notification channels if needed
                    FirebaseMessagingService.createNotificationChannels();
                } else {
                    Log.d(TAG, "Notification permission denied");
                    // Notify the user that notifications are disabled
                    boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                            activity, Manifest.permission.POST_NOTIFICATIONS);
                    
                    if (!showRationale) {
                        // User selected "Don't ask again", guide them to settings
                        openNotificationSettings(activity);
                    }
                }
            }
        );
    }

    /**
     * Request notification permission for Android 13+ with launcher
     */
    public static void requestNotificationPermissionWithLauncher(
            ActivityResultLauncher<String> permissionLauncher) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    /**
     * Initialize Firebase Cloud Messaging
     */
    public static void initialize() {
        // Create notification channels for Android O+
        FirebaseMessagingService.createNotificationChannels();

        // Get the FCM token
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                    return;
                }

                // Get new FCM registration token
                String token = task.getResult();
                Log.d(TAG, "FCM Token: " + token);
                
                // Save token to Firestore
                saveTokenToFirestore(token);
            });
    }

    private static void saveTokenToFirestore(String token) {
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

    /**
     * Send a chat notification to a specific user
     */
    public static void sendChatNotification(String receiverUserId, String senderName, String message) {
        Log.d(TAG, "Sending chat notification to user: " + receiverUserId);
        
        FirebaseFirestore.getInstance().collection("users").document(receiverUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String receiverToken = documentSnapshot.getString("fcmToken");
                    if (receiverToken != null && !receiverToken.isEmpty()) {
                        // Create chat-specific data for deep linking
                        Map<String, String> chatData = createChatData(senderName, message);
                        
                        // Add chatroom ID for navigation if needed
                        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        String chatroomId = getChatroomId(currentUserId, receiverUserId);
                        chatData.put("chatRoomId", chatroomId);
                        
                        Log.d(TAG, "Sending notification to: " + receiverUserId + 
                              " with sender: " + chatData.get("senderName") + 
                              " for chatroom: " + chatroomId);
                        
                        // Send the notification with all required data
                        sendCloudMessage(
                            receiverToken,
                            "New message from " + senderName,
                            message,
                            FirebaseMessagingService.CHANNEL_ID_CHAT,
                            chatData
                        );
                    } else {
                        Log.d(TAG, "Receiver FCM token not found for user: " + receiverUserId);
                    }
                } else {
                    Log.d(TAG, "Receiver user document not found for ID: " + receiverUserId);
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Error fetching receiver user data", e));
    }

    /**
     * Get a chatroom ID from two user IDs (consistent with FirebaseUtil implementation)
     */
    private static String getChatroomId(String userId1, String userId2) {
        if (userId1.hashCode() < userId2.hashCode()) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    private static Map<String, String> createChatData(String senderName, String message) {
        Map<String, String> data = new HashMap<>();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        data.put("senderId", currentUserId);
        data.put("senderName", senderName);
        data.put("message", message);
        data.put("messageType", "chat");
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return data;
    }

    /**
     * Send a cloud message to a specific device token
     */
    public static void sendCloudMessage(String token, String title, String message, 
                                       String channelId, Map<String, String> additionalData) {
        executor.execute(() -> {
            try {
                // Create notification payload
                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", message);

                JSONObject data = new JSONObject();
                data.put("title", title);
                data.put("message", message);
                data.put("channel_id", channelId);
                
                // Add additional data
                if (additionalData != null) {
                    for (Map.Entry<String, String> entry : additionalData.entrySet()) {
                        data.put(entry.getKey(), entry.getValue());
                    }
                }

                JSONObject fcmMessage = new JSONObject();
                fcmMessage.put("notification", notification);
                fcmMessage.put("data", data);
                fcmMessage.put("token", token);

                JSONObject root = new JSONObject();
                root.put("message", fcmMessage);

                // Send FCM message
                sendHttpRequest(root.toString());
            } catch (Exception e) {
                Log.e(TAG, "Failed to create FCM message", e);
            }
        });
    }

    /**
     * Send notification to a topic
     */
    public static void sendNotificationToTopic(String topic, String title, String message) {
        executor.execute(() -> {
            try {
                JSONObject notification = new JSONObject();
                notification.put("title", title);
                notification.put("body", message);

                JSONObject data = new JSONObject();
                data.put("title", title);
                data.put("message", message);
                data.put("channel_id", FirebaseMessagingService.CHANNEL_ID_DEFAULT);

                JSONObject fcmMessage = new JSONObject();
                fcmMessage.put("notification", notification);
                fcmMessage.put("data", data);
                fcmMessage.put("topic", topic);

                JSONObject root = new JSONObject();
                root.put("message", fcmMessage);

                sendHttpRequest(root.toString());
            } catch (Exception e) {
                Log.e(TAG, "Failed to create FCM message", e);
            }
        });
    }

    /**
     * Send an HTTP request to FCM API
     */
    private static void sendHttpRequest(String jsonPayload) {
        try {
            URL url = new URL(FCM_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + SERVER_KEY);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Notification sent successfully");
            } else {
                Log.e(TAG, "Failed to send notification. Response code: " + responseCode);
            }

            conn.disconnect();
        } catch (IOException e) {
            Log.e(TAG, "Error sending FCM message", e);
        }
    }
} 