package com.app.happytails.utils;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
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
import com.app.happytails.utils.Fragments.OAuthFragment; // Keep if used elsewhere
import com.app.happytails.utils.Fragments.ProfileFragment;
// Removed import for VetInfoChoiceFragment
// import com.app.happytails.utils.Fragments.VetInfoChoiceFragment;
import com.app.happytails.utils.Fragments.VetFragment; // Assuming VetFragment is your VetPageFragment

import com.app.happytails.utils.model.HomeModel; // Import HomeModel

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.Serializable; // Import Serializable
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

// Implement the listeners for fragments involved in the dog creation flow
public class MainActivity extends AppCompatActivity
        implements ProfileFragment.OnFragmentInteractionListener,
        // Removed implementation for VetInfoChoiceFragment.OnVetInfoChoiceListener
        VetFragment.OnDogCreationCompleteListener { // Implement listener from VetFragment (your VetPageFragment)


    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNav;
    private ImageButton searchButton;
    private Toolbar toolbar;
    private TextView mainAppName;
    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;

    // Variable to hold the collected dog data across fragments
    private HomeModel currentDogData;

    // IMPORTANT: Ensure this ID matches the FrameLayout in your activity_main.xml
    private static final int FRAGMENT_CONTAINER_ID = R.id.fragment_container; // <-- Verify this ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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

        // Load the initial fragment (HomeFragment) if the activity is created for the first time
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
        }

        // Set up bottom navigation
        setupBottomNavigation();

        // Handle incoming intent from SplashActivity (including OAuth code)
        handleIntent(getIntent());

        // Update FCM token
        updateFCMToken();

        // Check notification permission
        checkNotificationPermission();

        // Update toolbar with username
        updateToolbarUsername();
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
        mainAppName = findViewById(R.id.mainAppName);
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

        Uri data = intent.getData();
        if (data == null) return;

        // Check if the intent is a deep link for Patreon OAuth success
        if ("com.happytails".equals(data.getScheme()) && "oauth-success".equals(data.getHost())) {
            Log.d(TAG, "Received Patreon OAuth deep link: " + data.toString());
            
            String code = data.getQueryParameter("code");
            String state = data.getQueryParameter("state");

            if (code != null && state != null) {
                try {
                    // Parse state to get userId and dogId
                    JSONObject stateData = new JSONObject(state);
                    String userId = stateData.getString("userId");
                    String dogId = stateData.getString("dogId");

                    Log.d(TAG, "Extracted userId: " + userId);
                    Log.d(TAG, "Extracted dogId: " + dogId);

                    if (dogId != null) {
                        // Create and load DogProfile fragment with the dogId
                        DogProfile dogProfileFragment = new DogProfile();
                        Bundle args = new Bundle();
                        args.putString("dogId", dogId);
                        dogProfileFragment.setArguments(args);

                        // Load the fragment
                        loadFragment(dogProfileFragment, true);

                        // Wait for the fragment to be attached and initialized
                        getSupportFragmentManager().executePendingTransactions();

                        // Use Handler to delay the deep link handling
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (dogProfileFragment.isAdded() && dogProfileFragment.getView() != null) {
                                Log.d(TAG, "Handling deep link after fragment initialization");
                                dogProfileFragment.handleDeepLink(data);
                            } else {
                                Log.e(TAG, "Fragment not ready after delay");
                                Toast.makeText(this, "Error: Fragment not ready", Toast.LENGTH_SHORT).show();
                            }
                        }, 1000); // 1 second delay to ensure fragment is ready

                    } else {
                        Log.e(TAG, "Dog ID not found in state parameter");
                        Toast.makeText(this, "Error: Dog ID not found in state parameter", Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing state parameter: " + e.getMessage());
                    Toast.makeText(this, "Error processing donation data", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "Missing code or state in deep link");
                Toast.makeText(this, "Error: Missing required parameters", Toast.LENGTH_LONG).show();
            }

            // Clear the intent data to prevent reprocessing
            setIntent(new Intent());
        }
    }

    private void loadProfileFragment(String userId) {
        ProfileFragment profileFragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString("creator", userId);
        profileFragment.setArguments(args);
        loadFragment(profileFragment, true); // ProfileFragment likely disables toolbar
    }

    private boolean handleNavigation(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();
        boolean disableToolbar = false; // Flag to indicate if toolbar should be disabled

        if (itemId == R.id.createPostMenu) {
            // Start the dog creation flow with CreateFragment2
            fragment = new CreateFragment2();
            disableToolbar = true; // Disable toolbar for creation flow fragments
        } else if (itemId == R.id.homeMenu) {
            fragment = new HomeFragment();
            disableToolbar = false; // HomeFragment likely has toolbar
        } else if (itemId == R.id.chatsMenu) {
            fragment = new ChatFragment();
            disableToolbar = false; // ChatFragment likely has toolbar
        } else if (itemId == R.id.profileMenu) {
            fragment = new ProfileFragment();
            disableToolbar = true; // ProfileFragment likely disables toolbar
        } else {
            return false; // Item not handled
        }

        if (fragment != null) {
            // For bottom navigation, we usually replace without adding to back stack
            // unless navigating to a detail screen from a list.
            // For creation flow, we add to back stack to allow stepping back.
            boolean addToBackStack = (itemId == R.id.createPostMenu || itemId == R.id.profileMenu); // Add to back stack for creation/profile

            loadFragment(fragment, disableToolbar, addToBackStack); // Use the updated loadFragment
        }


        // Check for pending OAuth code after navigation (only relevant if navigating to DogProfile)
        String pendingOAuthCode = getIntent().getStringExtra("pending_oauth_code");
        if (pendingOAuthCode != null && fragment instanceof DogProfile) {
            Uri data = Uri.parse("com.happytails://oauth/redirect?code=" + pendingOAuthCode);
            ((DogProfile) fragment).handleDeepLink(data);
            getIntent().removeExtra("pending_oauth_code");
        }
        return true;
    }

    // Updated loadFragment method to include addToBackStack parameter
    private void loadFragment(Fragment fragment, boolean disableToolbar, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Add a fade animation for transitions (optional but nice)
        fragmentTransaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        fragmentTransaction.replace(FRAGMENT_CONTAINER_ID, fragment);

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        } else {
            // If not adding to back stack, remove all previous fragments from the stack
            // This is typical for bottom navigation primary destinations (Home, Chats)
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        fragmentTransaction.commit();
        toggleToolbarVisibility(!disableToolbar); // Toggle toolbar visibility based on the flag
        // bottomNav visibility is handled in onBackPressed based on fragment type
    }

    // Overload loadFragment for backward compatibility if needed (optional)
    private void loadFragment(Fragment fragment, boolean disableToolbar) {
        loadFragment(fragment, disableToolbar, false); // Default to not adding to back stack
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
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        String userId = currentUser.getUid();
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(userId)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved successfully"))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));
                    }
                });
    }

    @Override
    public void onProfileFragmentClosed() {
        // This callback is likely from ProfileFragment when it's closed
        // You might need similar callbacks from other fragments if they manage toolbar/bottom nav visibility
        toggleToolbarVisibility(true); // Assuming toolbar should be visible when ProfileFragment is closed
        bottomNav.setVisibility(View.VISIBLE); // Assuming bottom nav should be visible
    }

    private void toggleToolbarVisibility(boolean isVisible) {
        if (toolbar != null) {
            toolbar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDogCreationComplete() {
        Log.d(TAG, "Dog creation process complete (saved or skipped vet info).");
        // The VetFragment already navigates to HomeFragment and pops the back stack.
        // This callback can be used for any additional cleanup or state management in MainActivity.
        // For example, you might want to ensure bottom nav is visible and toolbar is enabled.
        bottomNav.setVisibility(View.VISIBLE);
        toggleToolbarVisibility(true);
    }


    // Placeholder method for the final dog profile creation logic
    // This method will be called when the user skips vet info
    // Implement the logic to save the HomeModel data to Firestore or your backend.
    // This is where you would handle image uploads, setting the creator ID, dog ID, etc.
    // Show a progress indicator if needed.
    private void finalizeDogCreation(HomeModel finalDogData) {
        Log.d(TAG, "Finalizing dog creation for: " + finalDogData.getDogName() + " (Skipped Vet Info)");

        // TODO: Implement your Firebase/backend saving logic here for the case where vet info is skipped.
        // This logic should be similar to the saveDogInfo in VetFragment, but without vet info fields.
        // You might want to move the image upload and Firestore saving logic into MainActivity
        // or a separate class if it's complex and shared between the "Save" and "Skip" paths.

        // Example: Show a message and navigate
        Toast.makeText(this, "Dog profile creation complete (skipped vet info)!", Toast.LENGTH_SHORT).show();
        // Navigate to HomeFragment or the new dog's profile page
        loadFragment(new HomeFragment(), false, false); // Navigate to Home, clear back stack
    }

    // Override onBackPressed to handle fragment back stack navigation
    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        int backStackEntryCount = fragmentManager.getBackStackEntryCount();

        if (backStackEntryCount > 0) {
            // Get the tag of the current fragment before popping
            Fragment currentFragment = fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID);

            fragmentManager.popBackStack(); // Pop the top fragment

            // After popping, the previous fragment becomes visible.
            // Check the type of the fragment that is now visible to adjust UI (toolbar/bottom nav)
            // This requires checking the fragment manager's state after the pop.
            // A better approach might be to manage toolbar/bottom nav visibility
            // in the onResume/onPause of each fragment, or using a central observer.

            // Simple approach: Check the back stack count after popping
            int newBackStackCount = fragmentManager.getBackStackEntryCount();
            if (newBackStackCount == 0) {
                // Back stack is empty, likely returned to a primary bottom nav fragment
                bottomNav.setVisibility(View.VISIBLE);
                toggleToolbarVisibility(true); // Assuming primary fragments show toolbar
                // Ensure the correct bottom nav item is selected based on the visible fragment
                Fragment visibleFragment = fragmentManager.findFragmentById(FRAGMENT_CONTAINER_ID);
                if (visibleFragment instanceof HomeFragment) {
                    bottomNav.setSelectedItemId(R.id.homeMenu);
                } else if (visibleFragment instanceof ChatFragment) {
                    bottomNav.setSelectedItemId(R.id.chatsMenu);
                } // Add other primary fragments if needed
            } else {
                // Still fragments on the back stack, likely in a flow (creation, profile, etc.)
                // You might need more sophisticated logic here to determine which fragment is now visible
                // and adjust toolbar/bottom nav accordingly.
                // For simplicity, let's assume fragments in a flow hide bottom nav and manage their own toolbar
                bottomNav.setVisibility(View.GONE);
                // Toolbar visibility is likely managed by the fragment itself or loadFragment
            }

        } else {
            // If no fragments on the back stack, call the super method to close the activity
            super.onBackPressed();
        }
    }

    private void updateToolbarUsername() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        if (username != null && !username.isEmpty()) {
                            mainAppName.setText(username);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching username", e);
                    mainAppName.setText(R.string.app_name);
                });
        }
    }

    public void onScrollChanged(int scrollY, boolean isScrollingDown) {
        // Get the current fragment
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(FRAGMENT_CONTAINER_ID);
        
        // Only apply scroll behavior for profile fragments
        if (currentFragment instanceof ProfileFragment || currentFragment instanceof DogProfile) {
            if (isScrollingDown) {
                // Show toolbar when scrolling down
                toolbar.setVisibility(View.VISIBLE);
                toolbar.animate()
                    .translationY(0)
                    .setDuration(200)
                    .start();
            } else {
                // Hide toolbar when scrolling up and at the top
                if (scrollY <= 0) {
                    toolbar.animate()
                        .translationY(-toolbar.getHeight())
                        .setDuration(200)
                        .start();
                }
            }
        } else {
            // For other fragments, keep toolbar visible
            toolbar.setVisibility(View.VISIBLE);
            toolbar.setTranslationY(0);
        }
    }

}
