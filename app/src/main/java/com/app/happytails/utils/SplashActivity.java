package com.app.happytails.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.app.happytails.R;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 3000; // Delay for splash screen in milliseconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Handle the intent (for deep linking or OAuth redirect)
        handleIntent(getIntent());
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
     * Checks if the URI corresponds to an OAuth redirect.
     *
     * @param data The URI data to check.
     * @return True if it's an OAuth redirect, false otherwise.
     */
    private boolean isOAuthRedirect(Uri data) {
        return data.getScheme().equals("https") &&
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