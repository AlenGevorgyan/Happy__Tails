package com.app.happytails.backend;

import com.google.auth.oauth2.GoogleCredentials;
import okhttp3.*;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.util.Collections;

public class FcmV1Sender {
    public static String sendNotification(String serviceAccountPath, String deviceFcmToken, String title, String bodyText) throws Exception {
        // 1. Load service account credentials
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new FileInputStream(serviceAccountPath))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/firebase.messaging"));
        googleCredentials.refreshIfExpired();
        String accessToken = googleCredentials.getAccessToken().getTokenValue();

        // 2. Prepare FCM v1 request
        String projectId = new JSONObject(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(serviceAccountPath))))
                .getString("project_id");
        String fcmUrl = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

        JSONObject message = new JSONObject();
        message.put("message", new JSONObject()
                .put("token", deviceFcmToken)
                .put("notification", new JSONObject()
                        .put("title", title)
                        .put("body", bodyText)
                )
        );

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(message.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(fcmUrl)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new RuntimeException("FCM send failed: " + response.code() + " - " + responseBody);
            }
            return responseBody;
        }
    }
} 