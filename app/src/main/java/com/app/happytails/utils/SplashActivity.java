package com.app.happytails.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.app.happytails.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 3000; // Delay for splash screen in milliseconds
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Register notification permission launcher
        notificationPermissionLauncher = NotificationHelper.createPermissionLauncher(this);

        // Initialize notification channels
        FirebaseMessagingService.createNotificationChannels();

        // Initialize FCM and refresh token in background
        initializeMessaging();

        // Check notification permission
        if (!NotificationHelper.hasNotificationPermission(this)) {
            // Request notification permission
            NotificationHelper.requestNotificationPermissionWithLauncher(notificationPermissionLauncher);
        }

        // Handle the intent (for deep linking or OAuth redirect)
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
                        // Token handled by NotificationHelper
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
     * Handles the incoming intent, whether it's a deep link, OAuth redirect, or normal app launch.
     *
     * @param intent The incoming intent to process.
     */
    private void handleIntent(Intent intent) {
        // Check for deep link data
        Uri data = intent.getData();
        if (data != null && "https".equals(data.getScheme())) {
            Log.d(TAG, "Deep Link Data: " + data.toString());
            if (isOAuthRedirect(data)) {
                // Handle OAuth redirect
                String code = data.getQueryParameter("code");
                if (code != null) {
                    Log.d(TAG, "OAuth Code Received: " + code);
                    navigateToMainActivityWithOAuth(code);
                } else {
                    Log.e(TAG, "OAuth redirect is missing the 'code' parameter.");
                    Toast.makeText(this, "Failed to authenticate with Patreon.", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity();
                }
            } else {
                Log.d(TAG, "Unknown deep link: " + data.toString());
                navigateToMainActivity();
            }
            // Clear the intent data to prevent reprocessing
            intent.setData(null);
            setIntent(intent);
            return;
        }

        // Default behavior: Check login status and navigate after delay
        showSplashAndNavigate();
    }

    /**
     * Navigates to MainActivity with the OAuth authorization code.
     *
     * @param code The OAuth authorization code.
     */
    private void navigateToMainActivityWithOAuth(String code) {
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("oauth_code", code); // Pass the OAuth code
        startActivity(mainIntent);
        finish(); // Close SplashActivity
    }

    /**
     * Navigates to MainActivity for authenticated users.
     */
    private void navigateToMainActivity() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
        finish(); // Close SplashActivity
    }
    /**
     * Navigates to SignInActivity for unauthenticated users.
     */
    private void navigateToSignInActivity() {
        Intent signInIntent = new Intent(this, SignInActivity.class);
        startActivity(signInIntent);
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
                "https".equals(data.getScheme()) &&
                "rational-photon-380817.web.app".equals(data.getHost()) &&
                "/redirect_patreon".equals(data.getPath());
    }

    /**
     * Displays the splash screen for a fixed duration and then navigates based on login status.
     */
    private void showSplashAndNavigate() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null) {
                Log.d(TAG, "User is logged in, navigating to MainActivity");
                navigateToMainActivity();
            } else {
                Log.d(TAG, "User is not logged in, navigating to SignInActivity");
                navigateToSignInActivity();
            }
        }, SPLASH_DELAY_MS);
    }
}