package com.app.happytails.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.app.happytails.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int SPLASH_DELAY_MS = 4500; // Delay for splash screen in milliseconds
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logoView = findViewById(R.id.app_logo);
        TextView appNameView = findViewById(R.id.app_name);
        TextView taglineView = findViewById(R.id.app_tagline);
        ImageView pawPrintLeftView = findViewById(R.id.paw_print_left);
        ImageView pawPrintRightView = findViewById(R.id.paw_print_right);
        TextView versionView = findViewById(R.id.app_version);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Register notification permission launcher
        notificationPermissionLauncher = NotificationHelper.createPermissionLauncher(this);

        // Initialize notification channels
        FirebaseMessagingService.createNotificationChannels();

        setupAnimations(logoView, appNameView, taglineView,
                pawPrintLeftView, pawPrintRightView, versionView);

        // Initialize FCM and refresh token in background
        initializeMessaging();

        // Check notification permission
        if (!NotificationHelper.hasNotificationPermission(this)) {
            // Request notification permission
            NotificationHelper.requestNotificationPermissionWithLauncher(notificationPermissionLauncher);
        }

        // Check for OAuth redirect
        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data != null && "com.happytails".equals(data.getScheme()) && 
            "oauth".equals(data.getHost()) && "/redirect".equals(data.getPath())) {
            String code = data.getQueryParameter("code");
            if (code != null) {
                Log.d(TAG, "Received OAuth code: " + code);
                // Add delay before OAuth navigation
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    navigateToMainActivityWithOAuth(code);
                }, SPLASH_DELAY_MS);
                return;
            }
        }

        // Normal app startup flow with delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (FirebaseUtil.isLoggedIn()) {
                navigateToMainActivity();
            } else {
                navigateToSignInActivity();
            }
        }, SPLASH_DELAY_MS);
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

    private void setupAnimations(ImageView logoView, TextView appNameView,
                                 TextView taglineView, ImageView pawPrintLeftView,
                                 ImageView pawPrintRightView, TextView versionView) {
        // Initially hide everything
        logoView.setAlpha(0f);
        logoView.setScaleX(0.6f);
        logoView.setScaleY(0.6f);
        appNameView.setAlpha(0f);
        appNameView.setTranslationY(50f);
        taglineView.setAlpha(0f);
        taglineView.setTranslationY(50f);
        pawPrintLeftView.setAlpha(0f);
        pawPrintLeftView.setTranslationX(50f);
        pawPrintRightView.setAlpha(0f);
        pawPrintRightView.setTranslationX(-50f);
        versionView.setAlpha(0f);

        // Animate logo
        ObjectAnimator logoFadeIn = ObjectAnimator.ofFloat(logoView, View.ALPHA, 0f, 1f);
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoView, View.SCALE_X, 0.6f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoView, View.SCALE_Y, 0.6f, 1f);

        AnimatorSet logoAnimSet = new AnimatorSet();
        logoAnimSet.playTogether(logoFadeIn, logoScaleX, logoScaleY);
        logoAnimSet.setDuration(800);
        logoAnimSet.setInterpolator(new OvershootInterpolator());

        // Animate app name
        ObjectAnimator titleFadeIn = ObjectAnimator.ofFloat(appNameView, View.ALPHA, 0f, 1f);
        ObjectAnimator titleTranslateY = ObjectAnimator.ofFloat(appNameView, View.TRANSLATION_Y, 50f, 0f);

        AnimatorSet titleAnimSet = new AnimatorSet();
        titleAnimSet.playTogether(titleFadeIn, titleTranslateY);
        titleAnimSet.setDuration(500);
        titleAnimSet.setStartDelay(300);
        titleAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());

        // Animate tagline
        ObjectAnimator taglineFadeIn = ObjectAnimator.ofFloat(taglineView, View.ALPHA, 0f, 1f);
        ObjectAnimator taglineTranslateY = ObjectAnimator.ofFloat(taglineView, View.TRANSLATION_Y, 50f, 0f);

        AnimatorSet taglineAnimSet = new AnimatorSet();
        taglineAnimSet.playTogether(taglineFadeIn, taglineTranslateY);
        taglineAnimSet.setDuration(500);
        taglineAnimSet.setStartDelay(450);
        taglineAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());

        // Animate paw prints
        ObjectAnimator pawLeftFadeIn = ObjectAnimator.ofFloat(pawPrintLeftView, View.ALPHA, 0f, 0.7f);
        ObjectAnimator pawLeftTranslateX = ObjectAnimator.ofFloat(pawPrintLeftView, View.TRANSLATION_X, 50f, 0f);

        AnimatorSet pawLeftAnimSet = new AnimatorSet();
        pawLeftAnimSet.playTogether(pawLeftFadeIn, pawLeftTranslateX);
        pawLeftAnimSet.setDuration(600);
        pawLeftAnimSet.setStartDelay(650);
        pawLeftAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator pawRightFadeIn = ObjectAnimator.ofFloat(pawPrintRightView, View.ALPHA, 0f, 0.7f);
        ObjectAnimator pawRightTranslateX = ObjectAnimator.ofFloat(pawPrintRightView, View.TRANSLATION_X, -50f, 0f);

        AnimatorSet pawRightAnimSet = new AnimatorSet();
        pawRightAnimSet.playTogether(pawRightFadeIn, pawRightTranslateX);
        pawRightAnimSet.setDuration(600);
        pawRightAnimSet.setStartDelay(650);
        pawRightAnimSet.setInterpolator(new AccelerateDecelerateInterpolator());

        // Create a separate AnimatorSet for the paw prints to play them together
        AnimatorSet pawPrintsTogether = new AnimatorSet();
        pawPrintsTogether.playTogether(pawLeftAnimSet, pawRightAnimSet);


        // Animate version text
        ObjectAnimator versionFadeIn = ObjectAnimator.ofFloat(versionView, View.ALPHA, 0f, 0.7f);
        versionFadeIn.setDuration(500);
        versionFadeIn.setStartDelay(800); // Adjusted delay slightly to play after paw prints
        versionFadeIn.setInterpolator(new AccelerateDecelerateInterpolator());


        // Play all animations sequentially
        AnimatorSet fullAnimSet = new AnimatorSet();
        fullAnimSet.playSequentially(
                logoAnimSet,
                titleAnimSet,
                taglineAnimSet,
                pawPrintsTogether, // Use the separate AnimatorSet here
                versionFadeIn
        );
        fullAnimSet.start();
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
        mainIntent.putExtra("oauth_code", code);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainIntent);
        finish();
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
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }, SPLASH_DELAY_MS);
    }
}