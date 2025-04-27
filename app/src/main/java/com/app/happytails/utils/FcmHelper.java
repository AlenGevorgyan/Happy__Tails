package com.app.happytails.utils;

import android.util.Log;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class FcmHelper {

    private static final String FCM_URL = "https://fcm.googleapis.com/v1/projects/rational-photon-380817/messages:send"; // Ensure this is correct

    // Enhanced error handling with retries
    private static final int MAX_RETRIES = 3;

    // Method to send push notification
    public static void sendPushNotification(final String accessToken, final String registrationToken, final String title, final String body) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int attempt = 0;
                boolean success = false;
                while (attempt < MAX_RETRIES && !success) {
                    attempt++;
                    try {
                        // Prepare the payload for the push notification
                        JSONObject message = new JSONObject();
                        message.put("message", new JSONObject()
                                .put("token", registrationToken)
                                .put("notification", new JSONObject()
                                        .put("title", title)
                                        .put("body", body)
                                ));

                        // Set up HTTP connection
                        URL url = new URL(FCM_URL);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoOutput(true);

                        // Send the payload
                        try (OutputStream os = connection.getOutputStream()) {
                            byte[] input = message.toString().getBytes("utf-8");
                            os.write(input, 0, input.length);
                        }

                        // Get response code and log
                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            Log.d("FcmHelper", "Push notification sent successfully");
                            success = true;
                        } else {
                            Log.e("FcmHelper", "Failed to send notification. Response code: " + responseCode);
                        }
                    } catch (Exception e) {
                        Log.e("FcmHelper", "Error in sending notification: " + e.getMessage());
                    }

                    if (!success) {
                        Log.d("FcmHelper", "Retrying... Attempt " + attempt);
                    }
                }

                if (!success) {
                    Log.e("FcmHelper", "Failed to send notification after " + MAX_RETRIES + " attempts");
                }
            }
        }).start();
    }
}
