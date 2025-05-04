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
import com.app.happytails.utils.Fragments.ChatFragment;
import com.app.happytails.utils.Fragments.CreateFragment2;
import com.app.happytails.utils.Fragments.DogProfile;
import com.app.happytails.utils.Fragments.HomeFragment;
import com.app.happytails.utils.Fragments.OAuthFragment;
import com.app.happytails.utils.Fragments.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends AppCompatActivity implements ProfileFragment.OnFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNav;
    private ImageButton searchButton;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        // Get FCM token if user is authenticated and email is verified
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            getFCMToken();
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

        // Check for notification click
        if (intent.hasExtra("userId")) {
            String userId = intent.getStringExtra("userId");
            loadProfileFragment(userId);
        }
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

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    FirebaseUtil.currentUserDetails().update("fcmToken", token)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token updated successfully"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM Token", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to retrieve FCM Token", e));
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