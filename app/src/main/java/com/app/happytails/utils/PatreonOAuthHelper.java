package com.app.happytails.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.IOException;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PatreonOAuthHelper {

    private static final String TAG = "PatreonOAuthHelper";
    private static final String CLIENT_ID = "Uez7JLX7nDE20Plv7bJhnfMg6CAATELJ0ogExskHrPynJ8TbEZbahrqiJVH207to"; // Replace with your Patreon client ID
    private static final String CLIENT_SECRET = "TxDYtT-cfPoKoo8OLqsQYobpLMeqyJcUfznDcagVrVyRIt1TmnG2KBtTpBlg_RGP";
    private static final String REDIRECT_URI = "https://happytails.page.link/UkMX";
    
    // The correct token endpoint for Patreon OAuth
    private static final String TOKEN_ENDPOINT = "https://www.patreon.com/api/oauth2/token";
    
    // The identity endpoint to get user data after authentication
    private static final String IDENTITY_ENDPOINT = "https://www.patreon.com/api/oauth2/v2/identity";
    
    // Campaign ID for donations (replace with your campaign ID)
    private static final String CAMPAIGN_ID = "12345678";
    
    // Store the state parameter to verify OAuth responses
    private static String stateParam;

    /**
     * Get the authorization URL with state parameter for security
     */
    public static String getAuthorizationUrl() {
        // Generate a random state parameter to prevent CSRF attacks
        stateParam = UUID.randomUUID().toString();
        
        return "https://www.patreon.com/oauth2/authorize" +
               "?response_type=code" +
               "&client_id=" + CLIENT_ID +
               "&redirect_uri=" + REDIRECT_URI +
               "&state=" + stateParam +
               "&scope=identity%20identity.memberships%20campaigns.members";
    }
    
    /**
     * Launch the authorization flow in the browser
     */
    public static void startBrowserAuthentication(Context context) {
        String authUrl = getAuthorizationUrl();
        
        try {
            // Try to use Chrome Custom Tabs if available
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build();
            customTabsIntent.launchUrl(context, Uri.parse(authUrl));
        } catch (Exception e) {
            // Fallback to regular browser intent
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
            context.startActivity(browserIntent);
        }
    }
    
    /**
     * Verify the response from OAuth flow
     */
    public static boolean verifyOAuthResponse(Uri responseUri) {
        String state = responseUri.getQueryParameter("state");
        return state != null && state.equals(stateParam);
    }

    /**
     * Exchange authorization code for token
     */
    public static void exchangeCodeForToken(String authorizationCode, final TokenCallback callback) {
        Log.d(TAG, "Exchanging authorization code for token");
        // Create OkHttp client
        OkHttpClient client = new OkHttpClient();

        // Prepare the request body
        FormBody formBody = new FormBody.Builder()
                .add("code", authorizationCode)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("redirect_uri", REDIRECT_URI)
                .add("grant_type", "authorization_code")
                .build();

        // Create the request for token exchange
        Request request = new Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(formBody)
                .build();

        // Make the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = "";
                try {
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }
                    
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Token response received successfully");
                        // Parse the JSON response
                        String accessToken = parseAccessToken(responseBody);
                        if (accessToken != null) {
                            // Get user information with the token
                            getUserInfo(accessToken, callback);
                        } else {
                            Log.e(TAG, "Failed to parse access token from response");
                            callback.onFailure("Failed to parse access token from response");
                        }
                    } else {
                        // Handle unsuccessful response
                        Log.e(TAG, "Error: " + response.code() + " - " + response.message() + "\nBody: " + responseBody);
                        callback.onFailure("Error: " + response.code() + " - " + response.message());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception during token exchange: " + e.getMessage());
                    callback.onFailure("Exception: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error exchanging code for token: " + e.getMessage());
                callback.onFailure("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Get user information with the access token
     */
    private static void getUserInfo(String accessToken, final TokenCallback callback) {
        Log.d(TAG, "Getting user information with access token");
        OkHttpClient client = new OkHttpClient();
        
        Request request = new Request.Builder()
                .url(IDENTITY_ENDPOINT + "?include=memberships,campaign")
                .header("Authorization", "Bearer " + accessToken)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "User identity retrieved successfully");
                    // Pass the token back to the callback
                    callback.onSuccess(accessToken);
                } else {
                    Log.e(TAG, "Error getting user info: " + response.code());
                    callback.onFailure("Error getting user info: " + response.code());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Error getting user info: " + e.getMessage());
                callback.onFailure("Error getting user info: " + e.getMessage());
            }
        });
    }
    
    /**
     * Open creator's campaign page for a specific dog
     * Shows all tiers/campaigns rather than asking for amount
     */
    public static void openCreatorCampaigns(Context context, String accessToken, String dogId) {
        Log.d(TAG, "Opening campaign page for dog: " + dogId);
        
        // First get the creator ID from the dog document
        FirebaseUtil.getDogReference(dogId).get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    String creatorId = document.getString("creatorId");
                    String creatorPatreonUrl = document.getString("creatorPatreonUrl");
                    
                    if (creatorPatreonUrl != null && !creatorPatreonUrl.isEmpty()) {
                        // If we have a direct URL to the creator's page, use it
                        Log.d(TAG, "Opening creator's Patreon page: " + creatorPatreonUrl);
                        
                        // Add return parameter for tracking
                        String finalUrl = creatorPatreonUrl;
                        if (!finalUrl.contains("?")) {
                            finalUrl += "?";
                        } else {
                            finalUrl += "&";
                        }
                        finalUrl += "return_app=happytails&dog_id=" + dogId;
                        
                        // Open in browser
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl));
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(browserIntent);
                    } 
                    else if (creatorId != null && !creatorId.isEmpty()) {
                        // If we have just the creator ID, construct URL
                        String url = "https://www.patreon.com/user?u=" + creatorId + "&return_app=happytails&dog_id=" + dogId;
                        Log.d(TAG, "Opening creator's page by ID: " + url);
                        
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(browserIntent);
                    }
                    else {
                        // If no direct creator info, try to get it from Patreon using the token
                        getCreatorInfoFromPatreon(context, accessToken, dogId);
                    }
                } else {
                    Toast.makeText(context, "Could not find dog information", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting dog document", e);
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    /**
     * Get creator information from Patreon when not available in our database
     */
    private static void getCreatorInfoFromPatreon(Context context, String accessToken, String dogId) {
        // If we don't have creatorId, try to get campaign info from Patreon
        getCampaignData(accessToken, new CampaignCallback() {
            @Override
            public void onSuccess(String campaignId, String creatorId) {
                if (!creatorId.isEmpty()) {
                    // Save the creator ID for future use
                    FirebaseUtil.getDogReference(dogId)
                        .update("creatorId", creatorId)
                        .addOnCompleteListener(task -> {
                            Log.d(TAG, "Updated creator ID in database");
                        });
                    
                    // Open the creator's page
                    String url = "https://www.patreon.com/user?u=" + creatorId + "&return_app=happytails&dog_id=" + dogId;
                    Log.d(TAG, "Opening creator's page from Patreon data: " + url);
                    
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(browserIntent);
                } else {
                    // If we have just the campaign ID, use that
                    String url = "https://www.patreon.com/campaigns/" + campaignId + "?return_app=happytails&dog_id=" + dogId;
                    Log.d(TAG, "Opening campaign page: " + url);
                    
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(browserIntent);
                }
            }
            
            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to get campaign data: " + errorMessage);
                Toast.makeText(context, "Couldn't find campaign: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Check if a payment was successful by parsing the return URL
     * This detects both one-time donations and pledges to campaigns
     */
    public static boolean checkDonationSuccess(Uri responseUri) {
        if (responseUri == null) {
            return false;
        }

        Log.d(TAG, "Checking donation success: URI=" + responseUri.toString());

        // Check for Patreon thank you pages
        boolean isThankYouPage = responseUri.toString().contains("patreon.com/thank_you");
        boolean isSuccessPage = responseUri.toString().contains("patreon.com/bePatron-success");

        // Check for status parameters in return URL
        String status = responseUri.getQueryParameter("status");
        boolean hasSuccessStatus = status != null &&
                (status.equals("success") || status.equals("completed") || status.equals("processed"));

        // Check for our custom app return parameter
        boolean isAppReturn = responseUri.toString().contains("return_app=happytails");

        // Check if this is a membership confirmation
        boolean isMembershipConfirm = responseUri.toString().contains("membership_confirm=true");

        // Log all conditions for debugging
        Log.d(TAG, "isThankYouPage=" + isThankYouPage +
                   ", isSuccessPage=" + isSuccessPage +
                   ", hasSuccessStatus=" + hasSuccessStatus +
                   ", isAppReturn=" + isAppReturn +
                   ", isMembershipConfirm=" + isMembershipConfirm);

        // Return true if any success condition is met
        return (isThankYouPage || isSuccessPage || hasSuccessStatus || isMembershipConfirm) && isAppReturn;
    }

    /**
     * Get pledge amount from a successful payment
     */
    public static double getPledgeAmount(Uri responseUri) {
        // First try to get amount directly from URL
        try {
            String amountParam = responseUri.getQueryParameter("amount");
            if (amountParam != null && !amountParam.isEmpty()) {
                return Double.parseDouble(amountParam);
            }

            // Try tier/pledge parameters
            String tierParam = responseUri.getQueryParameter("tier");
            if (tierParam != null && !tierParam.isEmpty()) {
                return Double.parseDouble(tierParam);
            }

            String pledgeParam = responseUri.getQueryParameter("pledge");
            if (pledgeParam != null && !pledgeParam.isEmpty()) {
                return Double.parseDouble(pledgeParam);
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing pledge amount", e);
        }

        // If no amount found in URL, check for tier indicators in the URL path
        String path = responseUri.getPath();
        if (path != null) {
            // Common Patreon tier amounts
            if (path.contains("tier=1") || path.contains("$1")) return 1.0;
            if (path.contains("tier=3") || path.contains("$3")) return 3.0;
            if (path.contains("tier=5") || path.contains("$5")) return 5.0;
            if (path.contains("tier=10") || path.contains("$10")) return 10.0;
            if (path.contains("tier=20") || path.contains("$20")) return 20.0;
            if (path.contains("tier=50") || path.contains("$50")) return 50.0;
            if (path.contains("tier=100") || path.contains("$100")) return 100.0;
        }

        // Default to a standard amount if we can't determine it
        return 5.0;
    }

    /**
     * Get dogId from success URL
     */
    public static String getDogIdFromSuccessUrl(Uri responseUri) {
        return responseUri.getQueryParameter("dog_id");
    }

    /**
     * Get campaign data from Patreon API using the access token
     */
    private static void getCampaignData(String accessToken, CampaignCallback callback) {
        if (accessToken == null || accessToken.isEmpty()) {
            callback.onFailure("No access token available");
            return;
        }
        
        OkHttpClient client = new OkHttpClient();
        
        Request request = new Request.Builder()
                .url("https://www.patreon.com/api/oauth2/v2/identity?include=campaign")
                .header("Authorization", "Bearer " + accessToken)
                .build();
                
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        
                        // Parse the campaign ID and creator ID from the response
                        if (json.has("included")) {
                            JSONArray included = json.getJSONArray("included");
                            for (int i = 0; i < included.length(); i++) {
                                JSONObject item = included.getJSONObject(i);
                                if (item.getString("type").equals("campaign")) {
                                    String campaignId = item.getString("id");
                                    
                                    // Get creator ID from relationships if available
                                    String creatorId = "";
                                    if (item.has("relationships") && 
                                        item.getJSONObject("relationships").has("creator") && 
                                        item.getJSONObject("relationships").getJSONObject("creator").has("data")) {
                                            creatorId = item.getJSONObject("relationships")
                                                          .getJSONObject("creator")
                                                          .getJSONObject("data")
                                                          .getString("id");
                                    }
                                    
                                    Log.d(TAG, "Found campaign ID: " + campaignId + ", creator ID: " + creatorId);
                                    callback.onSuccess(campaignId, creatorId);
                                    return;
                                }
                            }
                        }
                        callback.onFailure("No campaign found in response");
                    } else {
                        callback.onFailure("API error: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing campaign data", e);
                    callback.onFailure("Error parsing campaign data: " + e.getMessage());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Network error getting campaign data", e);
                callback.onFailure("Network error: " + e.getMessage());
            }
        });
    }

    /**
     * Callback for campaign data retrieval
     */

    private static String parseAccessToken(String responseBody) {
        // Use JSON parsing to extract the access token
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse.getString("access_token");
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Callback interface for handling the token response
    public interface TokenCallback {
        void onSuccess(String accessToken);
        void onFailure(String errorMessage);
    }
    
    // Callback interface for campaign data retrieval
    public interface CampaignCallback {
        void onSuccess(String campaignId, String creatorId);
        void onFailure(String errorMessage);
    }
    
    // Callback interface for donation updates
    public interface DonationCallback {
        void onSuccess(double newAmount, int fundingPercentage);
        void onFailure(String errorMessage);
    }

    /**
     * Update funding amount in Firebase for a dog after successful donation
     */
    public static void updateFundingAmount(String dogId, double amount, DonationCallback callback) {
        Log.d(TAG, "Updating funding amount for dog: " + dogId + ", amount: " + amount);
        
        FirebaseUtil.getDogReference(dogId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Get current funding amount
                    Double currentAmount = documentSnapshot.getDouble("fundingAmount");
                    Long donationCount = documentSnapshot.getLong("donationCount");
                    
                    if (currentAmount == null) currentAmount = 0.0;
                    if (donationCount == null) donationCount = 0L;
                    
                    // Calculate new values
                    double newAmount = currentAmount + amount;
                    long newCount = donationCount + 1;
                    
                    // Calculate funding percentage based on target amount
                    Double targetAmount = documentSnapshot.getDouble("targetAmount");
                    int fundingPercentage = 0;
                    
                    if (targetAmount != null && targetAmount > 0) {
                        fundingPercentage = (int)((newAmount / targetAmount) * 100);
                        if (fundingPercentage > 100) fundingPercentage = 100;
                    }
                    
                    Log.d(TAG, "Current amount: " + currentAmount + ", new amount: " + newAmount + 
                               ", target: " + targetAmount + ", percentage: " + fundingPercentage);
                    
                    // Update the document with all the new values
                    int finalFundingPercentage = fundingPercentage;
                    FirebaseUtil.getDogReference(dogId)
                        .update(
                            "fundingAmount", newAmount,
                            "donationCount", newCount,
                            "fundingPercentage", fundingPercentage,
                            "lastDonationAmount", amount,
                            "lastDonationTimestamp", System.currentTimeMillis()
                        )
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Funding updated successfully");
                            callback.onSuccess(newAmount, finalFundingPercentage);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error updating funding", e);
                            callback.onFailure(e.getMessage());
                        });
                } else {
                    callback.onFailure("Dog document not found");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting dog document", e);
                callback.onFailure(e.getMessage());
            });
    }
}
