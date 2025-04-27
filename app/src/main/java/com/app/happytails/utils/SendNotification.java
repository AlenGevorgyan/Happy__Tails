package com.app.happytails.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.app.happytails.utils.model.UserModel;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SendNotification {

    private static final String TAG = "SendNotification";
    private static final String PROJECT_ID = "rational-photon-380817";
    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "YOUR_SERVER_KEY";

    // Method to send push notifications via backend
    public void sendPushNotification(String fcmToken, String notificationTitle, String notificationBody) {
        try {
            OkHttpClient client = new OkHttpClient();
            org.json.JSONObject json = new org.json.JSONObject();
            json.put("fcmToken", fcmToken);
            json.put("title", notificationTitle);
            json.put("body", notificationBody);

            RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json; charset=utf-8"));
            // TODO: Replace with your backend server's actual IP or domain
            String backendUrl = "http://10.0.2.2:8080/send-notification"; // 10.0.2.2 for Android emulator, or use your server IP

            Request request = new Request.Builder()
                    .url(backendUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Backend notification send failed", e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Backend notification error: " + response.code());
                    } else {
                        Log.d(TAG, "Notification sent via backend successfully");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification via backend: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
