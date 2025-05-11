package com.app.happytails.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class PatreonOAuthHelper {

    private static final String TAG = "PatreonOAuthHelper";
    private static final String CLIENT_ID = APIKeys.PATREON_CLIENT_ID; // Replace with your Patreon client ID
    private static final String CLIENT_SECRET = APIKeys.PATREON_CLIENT_SECRET; // Replace with your Patreon client secret
    private static final String REDIRECT_URI = "https://rational-photon-380817.web.app/redirect_patreon"; // Your app's redirect URI
    private static final String PREF_NAME = "patreon_tokens";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";

    public static void startPatreonOAuth(Context context) {
        String authUrl = "https://www.patreon.com/oauth2/authorize?" +
                "response_type=code" +
                "&client_id=" + CLIENT_ID +
                "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                "&scope=identity";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        context.startActivity(intent);
    }

    public interface TokenCallback {
        void onSuccess(String accessToken);
        void onFailure(String errorMessage);
    }

    public static void exchangeCodeForToken(Context context, String code, TokenCallback callback) {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("code", code)
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("redirect_uri", REDIRECT_URI)
                .build();

        Request request = new Request.Builder()
                .url("https://www.patreon.com/api/oauth2/token")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = new JSONObject(json);
                        String accessToken = jsonObject.getString("access_token");
                        String refreshToken = jsonObject.getString("refresh_token");
                        long expiresIn = jsonObject.getLong("expires_in");
                        saveTokens(context, accessToken, refreshToken, expiresIn);
                        callback.onSuccess(accessToken);
                    } catch (Exception e) {
                        callback.onFailure("Failed to parse token response: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Token exchange failed: " + response.message());
                }
            }
        });
    }

    private static void saveTokens(Context context, String accessToken, String refreshToken, long expiresIn) {
        try {
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
            editor.putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + expiresIn * 1000);
            editor.apply();
            Log.d(TAG, "Tokens saved successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error saving tokens", e);
            throw new RuntimeException("Failed to save tokens", e);
        }
    }

    public interface PatronStatusCallback {
        void onSuccess(boolean isPatron, double pledgeAmount);
        void onFailure(String errorMessage);
    }

    public static void checkPatronStatus(Context context, String userId, PatronStatusCallback callback) {
        String accessToken = getAccessToken(context);
        if (accessToken == null) {
            callback.onFailure("No access token available");
            return;
        }

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://www.patreon.com/api/oauth2/v2/identity?include=memberships.campaign&fields%5Bmember%5D=currently_entitled_amount_cents,patron_status")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("API error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        String json = response.body().string();
                        JSONObject jsonObject = new JSONObject(json);
                        JSONArray included = jsonObject.optJSONArray("included");
                        boolean isPatron = false;
                        double pledgeAmount = 0.0;

                        if (included != null) {
                            for (int i = 0; i < included.length(); i++) {
                                JSONObject item = included.getJSONObject(i);
                                if ("member".equals(item.getString("type"))) {
                                    JSONObject attributes = item.getJSONObject("attributes");
                                    String patronStatus = attributes.getString("patron_status");
                                    if ("active_patron".equals(patronStatus)) {
                                        isPatron = true;
                                        int cents = attributes.getInt("currently_entitled_amount_cents");
                                        pledgeAmount = cents / 100.0; // Convert cents to dollars
                                        break;
                                    }
                                }
                            }
                        }

                        // Update Firestore with patron status
                        updateFirestorePatronStatus(userId, isPatron);
                        callback.onSuccess(isPatron, pledgeAmount);
                    } catch (Exception e) {
                        callback.onFailure("Failed to parse patron status: " + e.getMessage());
                    }
                } else {
                    callback.onFailure("Status check failed: " + response.message());
                }
            }
        });
    }

    private static void updateFirestorePatronStatus(String userId, boolean isPatron) {
        if (userId == null) {
            Log.e(TAG, "No user ID available for Firestore update");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .update("isPatron", isPatron, "lastPatronCheck", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore updated: isPatron=" + isPatron))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating Firestore", e));
    }

    private static String getAccessToken(Context context) {
        try {
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    PREF_NAME,
                    MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            return prefs.getString(KEY_ACCESS_TOKEN, null);
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving access token", e);
            return null;
        }
    }
}