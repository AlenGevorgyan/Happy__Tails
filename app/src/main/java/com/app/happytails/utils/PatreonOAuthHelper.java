package com.app.happytails.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PatreonOAuthHelper {

    private static final String TAG = "PatreonOAuthHelper";
    public static final String CLIENT_ID = APIKeys.PATREON_CLIENT_ID;
    private static final String CLIENT_SECRET = APIKeys.PATREON_CLIENT_SECRET;
    public static final String REDIRECT_URI = "https://rational-photon-380817.web.app/redirect_patreon";
    private static final String PREF_NAME = "patreon_tokens";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";

    public static void startPatreonOAuth(Context context, String dogId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Authenticated user not found. Cannot start Patreon OAuth flow.");
            Toast.makeText(context, "Please log in to donate", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create state parameter with userId and dogId
            JSONObject stateData = new JSONObject();
            stateData.put("userId", user.getUid());
            stateData.put("dogId", dogId);
            String state = stateData.toString();
            
            // Log the state data
            Log.d(TAG, "Created state data: " + state);
            Log.d(TAG, "userId: " + user.getUid());
            Log.d(TAG, "dogId: " + dogId);

            String authUrl = "https://www.patreon.com/oauth2/authorize?" +
                    "response_type=code" +
                    "&client_id=" + CLIENT_ID +
                    "&redirect_uri=" + Uri.encode(REDIRECT_URI) +
                    "&scope=identity%20identity.memberships" +
                    "&state=" + Uri.encode(state);

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
            context.startActivity(intent);
            Log.d(TAG, "Starting Patreon OAuth with URL: " + authUrl);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating state parameter", e);
            Toast.makeText(context, "Error starting donation process", Toast.LENGTH_SHORT).show();
        }
    }

    public interface TokenCallback {
        void onSuccess(String accessToken);
        void onFailure(String errorMessage);
    }

    public interface TokenExchangeCallback {
        void onSuccess(String accessToken, String refreshToken);
        void onFailure(String errorMessage);
    }

    public static void exchangeCodeForTokens(Context context, String code, TokenExchangeCallback callback) {
        if (code == null || code.isEmpty()) {
            callback.onFailure("Authorization code is missing");
            return;
        }

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
                Log.e(TAG, "Network error during token exchange", e);
                callback.onFailure("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : null;
                if (response.isSuccessful() && responseBody != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String accessToken = jsonObject.getString("access_token");
                        String refreshToken = jsonObject.getString("refresh_token");
                        long expiresIn = jsonObject.getLong("expires_in");
                        
                        // Save tokens both locally and to Firestore
                        saveTokens(context, accessToken, refreshToken, expiresIn);
                        saveTokensToFirestore(FirebaseAuth.getInstance().getCurrentUser().getUid(), 
                            accessToken, refreshToken, expiresIn);
                            
                        callback.onSuccess(accessToken, refreshToken);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing token response", e);
                        callback.onFailure("Failed to parse token response: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Token exchange failed: " + responseBody);
                    callback.onFailure("Token exchange failed: " + (responseBody != null ? responseBody : response.message()));
                }
            }
        });
    }

    private static void saveTokensToFirestore(String userId, String accessToken, String refreshToken, long expiresIn) {
        if (userId == null) {
            Log.e(TAG, "Cannot save tokens to Firestore: userId is null");
            return;
        }

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("patreonAccessToken", accessToken);
        tokenData.put("patreonRefreshToken", refreshToken);
        tokenData.put("tokenExpiresAt", System.currentTimeMillis() + (expiresIn * 1000));

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update(tokenData)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Tokens saved to Firestore successfully"))
            .addOnFailureListener(e -> Log.e(TAG, "Error saving tokens to Firestore", e));
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
            Log.d(TAG, "Tokens saved to SharedPreferences successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error saving tokens to SharedPreferences", e);
            throw new RuntimeException("Failed to save tokens", e);
        }
    }

    public interface PatronStatusCallback {
        void onSuccess(boolean isPatron, double pledgeAmount);
        void onFailure(String errorMessage);
    }

    public static void checkPatronStatus(Context context, String userId, PatronStatusCallback callback) {
        if (userId == null) {
            callback.onFailure("User ID is required");
            return;
        }

        String accessToken = getAccessToken(context);
        if (accessToken == null) {
            // Try to get token from Firestore
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String firestoreToken = documentSnapshot.getString("patreonAccessToken");
                    if (firestoreToken != null) {
                        checkPatronStatusWithToken(firestoreToken, userId, callback);
                    } else {
                        callback.onFailure("No access token available");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to get token from Firestore: " + e.getMessage()));
            return;
        }

        checkPatronStatusWithToken(accessToken, userId, callback);
    }

    private static void checkPatronStatusWithToken(String accessToken, String userId, PatronStatusCallback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://www.patreon.com/api/oauth2/v2/identity?include=memberships.campaign&fields%5Bmember%5D=currently_entitled_amount_cents,patron_status")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API error during patron status check", e);
                callback.onFailure("API error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : null;
                if (response.isSuccessful() && responseBody != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
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
                                        pledgeAmount = cents / 100.0;
                                        break;
                                    }
                                }
                            }
                        }

                        // Update Firestore with patron status
                        updateFirestorePatronStatus(userId, isPatron, pledgeAmount);
                        callback.onSuccess(isPatron, pledgeAmount);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing patron status response", e);
                        callback.onFailure("Failed to parse patron status: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Patron status check failed: " + responseBody);
                    callback.onFailure("Status check failed: " + (responseBody != null ? responseBody : response.message()));
                }
            }
        });
    }

    private static void updateFirestorePatronStatus(String userId, boolean isPatron, double pledgeAmount) {
        if (userId == null) {
            Log.e(TAG, "No user ID available for Firestore update");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("isPatron", isPatron);
        updates.put("lastPatronCheck", System.currentTimeMillis());
        updates.put("pledgeAmount", pledgeAmount);

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .update(updates)
            .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore updated: isPatron=" + isPatron + ", pledgeAmount=" + pledgeAmount))
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
            String token = prefs.getString(KEY_ACCESS_TOKEN, null);
            long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0);
            
            // Check if token is expired
            if (token != null && System.currentTimeMillis() >= expiresAt) {
                // Token is expired, try to refresh it
                String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null);
                if (refreshToken != null) {
                    // TODO: Implement token refresh logic
                    Log.d(TAG, "Token expired, refresh needed");
                    return null;
                }
            }
            
            return token;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving access token", e);
            return null;
        }
    }
}