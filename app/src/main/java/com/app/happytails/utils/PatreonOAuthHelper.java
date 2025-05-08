package com.app.happytails.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PatreonOAuthHelper {

    private static final String TAG = "PatreonOAuthHelper";
    private static final String CLIENT_ID = APIKeys.PATREON_CLIENT_ID; // Replace with your Patreon client ID
    private static final String CLIENT_SECRET = APIKeys.PATREON_CLIENT_SECRET; // Replace with your Patreon client secret
    private static final String REDIRECT_URI = "https://rational-photon-380817.web.app/redirect_patreon";
    private static final String TOKEN_URL = "https://www.patreon.com/api/oauth2/token";
    private static final String CAMPAIGNS_URL = "https://www.patreon.com/api/oauth2/v2/campaigns";

    /**
     * Starts the Patreon OAuth flow.
     * @param context The application context.
     */
    public static void startPatreonOAuth(Context context) {
        String authUrl = "https://www.patreon.com/oauth2/authorize?" +
                "response_type=code&client_id=" + CLIENT_ID +
                "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                "&scope=identity%20campaigns"; // Add required scopes

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        context.startActivity(intent);
    }

    public static void startPatreonOAuthForOwners(Context context) {
        String authUrl = "https://www.patreon.com/oauth2/authorize?" +
                "response_type=code&client_id=" + CLIENT_ID +
                "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                "&scope=identity";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        context.startActivity(intent);
    }

    /**
     * Exchanges the authorization code for an access token.
     * @param context The application context.
     * @param authCode The authorization code received from Patreon.
     * @param callback The callback to handle success or failure.
     */
    public static void exchangeCodeForToken(Context context, String authCode, OAuthCallback callback) {
        OkHttpClient client = new OkHttpClient();

        FormBody formBody = new FormBody.Builder()
                .add("code", authCode)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("redirect_uri", REDIRECT_URI)
                .add("grant_type", "authorization_code")
                .build();

        Request request = new Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure("Failed to exchange authorization code: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        String accessToken = jsonResponse.getString("access_token");
                        callback.onSuccess(accessToken);
                    } catch (JSONException e) {
                        callback.onFailure("Failed to parse access token: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Error exchanging authorization code: " + response.message());
                }
            }
        });
    }

    /**
     * Fetches campaigns associated with the authenticated Patreon account.
     * @param accessToken The access token received after OAuth.
     * @param callback The callback to handle success or failure.
     */
    public static void fetchCampaigns(String accessToken, CampaignsCallback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(CAMPAIGNS_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure("Failed to fetch campaigns: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        JSONArray campaigns = jsonResponse.getJSONArray("data");
                        callback.onSuccess(campaigns);
                    } catch (JSONException e) {
                        callback.onFailure("Failed to parse campaigns: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Error fetching campaigns: " + response.message());
                }
            }
        });
    }

    // Fetch campaign data after authorization
    public static void fetchCampaignDataForOwners(String accessToken, CampaignsCallback callback) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://www.patreon.com/api/oauth2/v2/campaigns?include=members";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure("Failed to fetch campaign data: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        callback.onSuccess(jsonResponse.names());
                    } catch (JSONException e) {
                        callback.onFailure("Failed to parse campaign data: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Error fetching campaign data: " + response.message());
                }
            }
        });
    }

    /**
     * Updates the funding amount for a specific dog in Firestore.
     * @param dogId The ID of the dog to update.
     * @param amount The donation amount to add.
     * @param callback The callback to handle success or failure.
     */
    public static void updateFundingAmount(String dogId, double amount, DonationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("dogs").document(dogId).get().addOnSuccessListener(documentSnapshot -> {
            Double current = documentSnapshot.getDouble("currentFunding");
            Double target = documentSnapshot.getDouble("targetFunding");
            double newAmount = (current != null ? current : 0) + amount;
            int newPercentage = (int) ((newAmount / (target != null ? target : 1)) * 100);

            db.collection("dogs").document(dogId)
                    .update("currentFunding", newAmount, "fundingProgress", newPercentage)
                    .addOnSuccessListener(aVoid -> callback.onSuccess(newAmount, newPercentage))
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        }).addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Checks if the donation was successful based on the redirect URI.
     * @param uri The redirect URI.
     * @return True if the donation was successful, false otherwise.
     */
    public static boolean checkDonationSuccess(Uri uri) {
        return uri != null && uri.toString().contains("donation_success=true");
    }

    /**
     * Extracts the pledge amount from the redirect URI.
     * @param uri The redirect URI.
     * @return The pledged amount.
     */
    public static double getPledgeAmount(Uri uri) {
        try {
            return Double.parseDouble(uri.getQueryParameter("amount"));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Extracts the dog ID from the redirect URI.
     * @param uri The redirect URI.
     * @return The dog ID.
     */
    public static String getDogIdFromSuccessUrl(Uri uri) {
        return uri.getQueryParameter("dogId");
    }

    // Callback interfaces
    public interface OAuthCallback {
        void onSuccess(String accessToken);
        void onFailure(String errorMessage);
    }

    public interface CampaignsCallback {
        void onSuccess(JSONArray campaigns);
        void onFailure(String errorMessage);
    }

    public interface DonationCallback {
        void onSuccess(double newAmount, int fundingPercentage);
        void onFailure(String errorMessage);
    }
}