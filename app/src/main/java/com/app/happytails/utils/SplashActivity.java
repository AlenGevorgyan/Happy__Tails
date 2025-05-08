package com.app.happytails.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.app.happytails.R;
import com.google.firebase.messaging.FirebaseMessaging;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 3000; // Delay for splash screen in milliseconds
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Register notification permission launcher
        notificationPermissionLauncher = NotificationHelper.createPermissionLauncher(this);
        
        // Initialize notification channels
        FirebaseMessagingService.createNotificationChannels();
        
        // Initialize FCM and refresh token in background
        initializeMessaging();
        
        // Check notification permission
        if (!NotificationHelper.hasNotificationPermission(this)) {
            // We need to request notification permission early
            NotificationHelper.requestNotificationPermissionWithLauncher(notificationPermissionLauncher);
        }

        // Handle the intent (for deep linking, OAuth redirect, or notifications)
        handleIntent(getIntent());
    }

    /**
     * Initialize Firebase Cloud Messaging
     */
    private void initializeMessaging() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Log.d(TAG, "FCM Token obtained successfully");
                    // We don't need to save it here as it's handled by NotificationHelper
                } else {
                    Log.e(TAG, "FCM Token fetch failed", task.getException());
                }
            });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent
        handleIntent(intent); // Process the new intent
    }

    /**
     * Handles the incoming intent, whether it's a deep link, OAuth redirect, notification, or normal app launch.
     *
     * @param intent The incoming intent to process.
     */
    private void handleIntent(Intent intent) {
        // First, check for notification data
        if (intent.hasExtra("senderId") || intent.hasExtra("userId") || intent.hasExtra("chatRoomId")) {
            Log.d(TAG, "Notification intent received");
            navigateToMainActivityWithNotificationData(intent);
            return;
        }

        // Check for deep link data
        Uri data = intent.getData();
        if (data != null) {
            Log.d(TAG, "Intent Data: " + data.toString());

            if (isOAuthRedirect(data)) {
                // Handle OAuth redirect
                String code = data.getQueryParameter("code");
                if (code != null) {
                    Log.d(TAG, "OAuth Code Received: " + code);
                    navigateToMainActivityWithOAuth(code);
                } else {
                    Log.e(TAG, "OAuth redirect is missing the 'code' parameter.");
                    navigateToMainActivity();
                }
                return;
            } else {
                Log.d(TAG, "Unknown deep link: " + data.toString());
            }
        }

        // Default behavior: Show sign-in screen after a delay
        showSplashAndNavigateToSignIn();
    }

    /**
     * Navigates to MainActivity with notification data.
     * This ensures notification clicks can navigate appropriately.
     * 
     * @param intent The intent containing notification data
     */
    private void navigateToMainActivityWithNotificationData(Intent intent) {
        Intent mainIntent = new Intent(this, MainActivity.class);
        
        // Copy all extras from the original intent
        mainIntent.putExtras(intent);
        
        // Add a flag to indicate this is from a notification
        mainIntent.putExtra("from_notification", true);
        
        // Start main activity with the data
        startActivity(mainIntent);
        finish(); // Close SplashActivity
    }

    /**
     * Checks if the URI corresponds to an OAuth redirect.
     *
     * @param data The URI data to check.
     * @return True if it's an OAuth redirect, false otherwise.
     */
    private boolean isOAuthRedirect(Uri data) {
        return data != null && 
               data.getScheme().equals("https") &&
               data.getHost().equals("rational-photon-380817.web.app") &&
               data.getPath().equals("/redirect_patreon");
    }

    /**
     * Navigates to the MainActivity with the OAuth authorization code.
     *
     * @param code The OAuth authorization code.
     */
    private void navigateToMainActivityWithOAuth(String code) {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("auth_code", code); // Pass the OAuth code to MainActivity
        mainIntent.putExtra("navigate_to_oauth", true); // Optional flag for OAuth navigation
        startActivity(mainIntent);
        finish(); // Close SplashActivity
    }

    /**
     * Navigates to the MainActivity in normal mode (no OAuth code).
     */
    private void navigateToMainActivity() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
        finish(); // Close SplashActivity
    }

    /**
     * Displays the splash screen for a fixed duration and then navigates to the SignInActivity.
     */
    private void showSplashAndNavigateToSignIn() {
        new Handler().postDelayed(() -> {
            Log.d(TAG, "Navigating to SignInActivity...");
            startActivity(new Intent(SplashActivity.this, SignInActivity.class));
            finish(); // Close SplashActivity
        }, SPLASH_DELAY_MS);
    }
}