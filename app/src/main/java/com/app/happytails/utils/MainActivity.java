package com.app.happytails.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.app.happytails.R;
import com.app.happytails.utils.Fragments.ChatFragment;
import com.app.happytails.utils.Fragments.CreateFragment2;
import com.app.happytails.utils.Fragments.DogProfile;
import com.app.happytails.utils.Fragments.HomeFragment;
import com.app.happytails.utils.Fragments.OAuthFragment;
import com.app.happytails.utils.Fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements ProfileFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNav;
    private ImageButton searchButton;
    private Toolbar toolbar;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register the permission launcher
        notificationPermissionLauncher = NotificationHelper.createPermissionLauncher(this);

        // Hide the default action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set navigation bar color for Lollipop and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(getResources().getColor(R.color.primary_color));
        }

        // Initialize views
        initializeViews();

        // Load the initial fragment (HomeFragment)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
        }

        // Set up bottom navigation
        setupBottomNavigation();

        // Handle incoming intent (OAuth redirect or notification clicks)
        handleIntent(getIntent());

        // Update FCM token
        updateFCMToken();
        
        // Check notification permission
        checkNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the FCM token when returning to the app
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            updateFCMToken();
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                // Show dialog explaining why we need the permission
                Toast.makeText(this, "Please enable notifications to receive important updates", 
                        Toast.LENGTH_LONG).show();
                
                // Request the notification permission
                NotificationHelper.requestNotificationPermissionWithLauncher(notificationPermissionLauncher);
            } else {
                // Permission already granted, initialize notification channels
                FirebaseMessagingService.createNotificationChannels();
            }
        } else {
            // For Android 12 and below, just initialize the channels
            FirebaseMessagingService.createNotificationChannels();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent
        handleIntent(intent); // Handle the new intent
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        bottomNav = findViewById(R.id.bottomNavigation);
        searchButton = findViewById(R.id.searchIcon);

        // Handle search button click
        searchButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(this::handleNavigation);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        // Check for OAuth redirect
        Uri data = intent.getData();
        if (data != null && data.toString().contains("code")) {
            String code = data.getQueryParameter("code");
            if (code != null) {
                navigateToOAuthFragment(code);
            } else {
                Log.e(TAG, "Authorization failed: Missing code");
                Toast.makeText(this, "Failed to authenticate with Patreon.", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Check for notification navigation intent from SplashActivity
        if (intent.getBooleanExtra("from_notification", false)) {
            Log.d(TAG, "Processing notification navigation");
            processNotificationNavigation(intent);
            return;
        }

        // Check for direct notification extras
        if (intent.hasExtra("userId") || intent.hasExtra("senderId") || intent.hasExtra("chatRoomId")) {
            processNotificationNavigation(intent);
        }
    }

    /**
     * Process navigation based on notification data
     */
    private void processNotificationNavigation(Intent intent) {
        // Check if this is a chat notification
        if (intent.hasExtra("senderId")) {
            String senderId = intent.getStringExtra("senderId");
            Log.d(TAG, "Notification for chat with user: " + senderId);
            openChatWithUser(senderId);
            return;
        }

        // Profile notification
        if (intent.hasExtra("userId")) {
            String userId = intent.getStringExtra("userId");
            Log.d(TAG, "Notification for user profile: " + userId);
            loadProfileFragment(userId);
            return;
        }

        // Chat room notification
        if (intent.hasExtra("chatRoomId")) {
            String chatRoomId = intent.getStringExtra("chatRoomId");
            Log.d(TAG, "Notification for chat room: " + chatRoomId);
            // Handle chat room - may need to get users from chatroom ID
            String[] userIds = chatRoomId.split("_");
            for (String userId : userIds) {
                if (!userId.equals(FirebaseUtil.currentUserId())) {
                    openChatWithUser(userId);
                    break;
                }
            }
        }
    }

    private void openChatWithUser(String userId) {
        // Fetch user details and open chat
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Create a UserModel from the document
                    String username = documentSnapshot.getString("username");
                    String email = documentSnapshot.getString("email");
                    String fcmToken = documentSnapshot.getString("fcmToken");
                    
                    // Use the ChatActivity's factory method to create the intent
                    Intent intent = ChatActivity.newInstance(this, userId, username, email, fcmToken);
                    
                    // Start the ChatActivity
                    startActivity(intent);
                    
                    Log.d(TAG, "Navigating to chat with user: " + username);
                } else {
                    Toast.makeText(this, "Could not find user details", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching user data", e);
                Toast.makeText(this, "Error opening chat", Toast.LENGTH_SHORT).show();
            });
    }

    private void navigateToOAuthFragment(String authCode) {
        OAuthFragment oAuthFragment = new OAuthFragment();

        // Pass the authorization code as an argument
        Bundle args = new Bundle();
        args.putString("auth_code", authCode);
        oAuthFragment.setArguments(args);

        // Replace the current fragment with the OAuthFragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, oAuthFragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadProfileFragment(String userId) {
        ProfileFragment profileFragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("creator", userId);
        profileFragment.setArguments(args);
        loadFragment(profileFragment, true);
    }

    private boolean handleNavigation(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.createPostMenu) {
            fragment = new CreateFragment2();
            loadFragment(fragment, true);
        } else if (itemId == R.id.homeMenu) {
            fragment = new HomeFragment();
            loadFragment(fragment, false);
        } else if (itemId == R.id.chatsMenu) {
            fragment = new ChatFragment();
            loadFragment(fragment, false);
        } else if (itemId == R.id.profileMenu) {
            fragment = new ProfileFragment();
            loadFragment(fragment, true);
        }
        return true;
    }

    private void loadFragment(Fragment fragment, boolean disableToolbar) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        toggleToolbarVisibility(!disableToolbar);
    }

    private void updateFCMToken() {
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Failed to get FCM token", task.getException());
                    return;
                }
                
                String token = task.getResult();
                Log.d(TAG, "FCM Token: " + token);
                
                // Save token to user document in Firestore
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
            });
    }

    @Override
    public void onProfileFragmentClosed() {
        toggleToolbarVisibility(true);
    }

    private void toggleToolbarVisibility(boolean isVisible) {
        if (toolbar != null) {
            toolbar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            bottomNav.setSelectedItemId(R.id.homeMenu);
        } else if (currentFragment instanceof ChatFragment) {
            bottomNav.setSelectedItemId(R.id.chatsMenu);
        } else if (currentFragment instanceof ProfileFragment
                || currentFragment instanceof CreateFragment2
                || currentFragment instanceof DogProfile) {
            super.onBackPressed();
            int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
            if (backStackCount <= 1) {
                toggleToolbarVisibility(true);
                bottomNav.setVisibility(View.VISIBLE);
            } else {
                Fragment previousFragment = getSupportFragmentManager().getFragments()
                        .get(backStackCount - 2);
                toggleToolbarVisibility(!(previousFragment instanceof ProfileFragment));
                bottomNav.setVisibility(previousFragment instanceof ProfileFragment ? View.GONE : View.VISIBLE);
            }
        } else {
            super.onBackPressed();
        }
    }
}
