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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.app.happytails.R;
import com.app.happytails.utils.Fragments.CreateFragment2;
import com.app.happytails.utils.Fragments.DogProfile;
import com.app.happytails.utils.Fragments.HomeFragment;
import com.app.happytails.utils.Fragments.ChatFragment;
import com.app.happytails.utils.Fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements ProfileFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNav;
    private ImageButton searchButton;
    private Toolbar toolbar;
    private String storedPatreonCode; // To store code if DogProfile isn't immediately available

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle incoming intents (deep links, OAuth redirects)
        handleIntent(getIntent());

        // Hide the default action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set navigation bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(getResources().getColor(R.color.primary_color));
        }

        // Initialize views
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        bottomNav = findViewById(R.id.bottomNavigation);
        searchButton = findViewById(R.id.searchIcon);

        // Handle search button click
        searchButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));

        // Load the initial fragment (HomeFragment)
        loadFragment(new HomeFragment(), false);

        // Set up bottom navigation
        bottomNav.setOnItemSelectedListener(this::handleNavigation);

        // Handle incoming intent at startup
        handleIncomingIntent(getIntent());

        // Get FCM token if user is authenticated and email is verified
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            getFCMToken();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Log.d("OAUTH", "Raw intent: " + intent);
        Uri data = intent.getData();
        Log.d("OAUTH", "Incoming URI: " + data);
        if (data != null && data.toString().startsWith("https://happytails.page.link/UkMX")) {
            String code = data.getQueryParameter("code");
            String state = data.getQueryParameter("state");
            Log.d("OAUTH", "Extracted code: " + code);
            Log.d("OAUTH", "Extracted state: " + state);
            // Pass to your DogProfile fragment or handler
            FragmentManager fm = getSupportFragmentManager();
            for (Fragment fragment : fm.getFragments()) {
                if (fragment instanceof DogProfile) {
                    ((DogProfile) fragment).handleOAuthRedirect(data);
                }
            }
        } else {
            Log.d("OAUTH", "no data or not matching dynamic link");
        }
    }

    private void handleOAuthRedirect(Uri data) {
        Log.d(TAG, "handleOAuthRedirect() - Data: " + data);
        String code = data.getQueryParameter("code");
        String error = data.getQueryParameter("error");

        if (code != null) {
            Log.d(TAG, "handleOAuthRedirect() - Code: " + code);
            // Attempt to pass code to DogProfile if it's the current fragment
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof DogProfile) {
                ((DogProfile) currentFragment).handleOAuthRedirect(data);
                storedPatreonCode = null; // Clear stored code
            } else {
                // Store the code to be passed to DogProfile when it's shown
                storedPatreonCode = code;
                Log.w(TAG, "handleOAuthRedirect() - DogProfile not found, storing code.");
            }
        } else if (error != null) {
            Log.e(TAG, "handleOAuthRedirect() - Error: " + error);
            String errorDescription = data.getQueryParameter("error_description");
            Log.e(TAG, "handleOAuthRedirect() - Error Description: " + errorDescription);
            Toast.makeText(this, "Patreon authentication failed: " + errorDescription, Toast.LENGTH_LONG).show();
        } else {
            Log.w(TAG, "handleOAuthRedirect() - No code or error parameter found!");
        }
    }

    private void handleDeepLink(Uri data) {
        // Handle other deep links here (e.g., for viewing a specific dog)
        Log.d(TAG, "handleDeepLink: Data = " + data);
        // Example:
        String path = data.getPath();
        if (path != null && path.startsWith("/dog/")) {
            String dogId = path.substring(5); // Extract the dog ID from the path
            loadDogProfile(dogId); // Navigate to DogProfile
        } else if (path != null && path.startsWith("/user/")){
            String userId = path.substring(6);
            loadProfileFragment(userId);
        }
        else {
            // Handle unknown deep links
            Log.w(TAG, "handleDeepLink: Unknown deep link: " + data);
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && intent.hasExtra("userId")) {
            String userId = intent.getStringExtra("userId");
            loadProfileFragment(userId);
        }
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

    void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    FirebaseUtil.currentUserDetails().update("fcmToken", token);
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
        } else if (currentFragment instanceof ProfileFragment || currentFragment instanceof CreateFragment2 || currentFragment instanceof DogProfile) {
            super.onBackPressed();
            if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
                toggleToolbarVisibility(true);
                bottomNav.setVisibility(View.VISIBLE);
            } else {
                Fragment previousFragment = getSupportFragmentManager().getFragments().get(getSupportFragmentManager().getBackStackEntryCount() - 2);
                toggleToolbarVisibility(!(previousFragment instanceof ProfileFragment));
                bottomNav.setVisibility(previousFragment instanceof ProfileFragment ? View.GONE : View.VISIBLE);
            }
        } else {
            super.onBackPressed();
        }
    }

    // Method to load DogProfile
    public void loadDogProfile(String dogId) {
        DogProfile dogProfile = new DogProfile();
        Bundle args = new Bundle();
        args.putString("dogId", dogId);
        dogProfile.setArguments(args);

        // Use the fragment_container
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, dogProfile, "dogProfileFragmentTag"); // Use a tag
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

        // Hide toolbar and bottom navigation
        toggleToolbarVisibility(false);
        bottomNav.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for stored Patreon code when MainActivity resumes
        if (storedPatreonCode != null) {
            Log.d(TAG, "onResume: Found stored Patreon code, attempting to pass to DogProfile.");
            Fragment dogProfileFragment = getSupportFragmentManager().findFragmentByTag("dogProfileFragmentTag");
            if (dogProfileFragment instanceof DogProfile) {
                // Create a Uri to pass the code
                Uri uri = Uri.parse("happytails://patreon/oauth?code=" + storedPatreonCode);
                ((DogProfile) dogProfileFragment).handleOAuthRedirect(uri);
                storedPatreonCode = null; // Clear the code after passing it
            } else {
                Log.w(TAG, "onResume: DogProfile fragment not found.");
                //  Ideally, you would have a more robust way to ensure that the code is passed.
            }
        }
    }
}