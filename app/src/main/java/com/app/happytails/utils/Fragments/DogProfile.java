package com.app.happytails.utils.Fragments;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.app.happytails.R;
import com.app.happytails.utils.PatreonOAuthHelper;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.OkHttpClient;

public class DogProfile extends Fragment {

    private static final String TAG = "DogProfile";

    private TextView dogNameTv, dogDescriptionTv, urgencyLevelTv;
    private TextView fundingAmountTv, targetAmountTv;
    private ProgressBar fundingProgress;
    private CircleImageView dogImage;
    private Button donateButton;

    private BottomNavigationView navigationView;

    private String dogId;
    private FirebaseFirestore db;
    private ListenerRegistration dogListener;
    private ArrayList<String> galleryImageUrls, supporters;
    private double targetAmount, currentAmount;
    private FirebaseAuth firebaseAuth;
    private OkHttpClient httpClient;

    // Fields to store vet information, now including last visit date and diagnosis
    private String vetName, clinicName, clinicAddress, vetPhone, vetEmail, medicalHistory;
    private String vetLastVisitDate, vetDiagnosis;
    private boolean hasVetInfo = false; // Flag to check if vet info exists

    // Constant for the FrameLayout container ID
    private static final int DOG_FRAGMENT_CONTAINER_ID = R.id.dog_fragment_container; // <-- Verify this ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout with FrameLayout and BottomNavigationView
        View view = inflater.inflate(R.layout.fragment_dog_profile, container, false); // Use the correct layout

        // Initialize views
        dogNameTv = view.findViewById(R.id.dogNameTV);
        dogDescriptionTv = view.findViewById(R.id.descriptionTV);
        urgencyLevelTv = view.findViewById(R.id.urgencyLevelValue);
        fundingProgress = view.findViewById(R.id.funding_bar_profile);
        fundingAmountTv = view.findViewById(R.id.fundingAmountTV);
        targetAmountTv = view.findViewById(R.id.targetAmountTV);
        dogImage = view.findViewById(R.id.dogProfileImage);
        donateButton = view.findViewById(R.id.donateButton);

        // Initialize BottomNavigationView
        navigationView = view.findViewById(R.id.dogBottomNavigation);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        httpClient = new OkHttpClient(); // Note: OkHttpClient should ideally be a singleton or managed lifecycle-aware

        // Setup toolbar back navigation
        setupToolbar(view);

        if (getArguments() != null) {
            dogId = getArguments().getString("dogId");
        }

        // Load data from Firestore
        if (dogId != null) {
            loadDogData();
        } else {
            // Handle case where dogId is missing (shouldn't happen if navigated correctly)
            Log.e(TAG, "Dog ID is missing in arguments!");
            Toast.makeText(getContext(), "Error: Dog ID not provided.", Toast.LENGTH_SHORT).show();
            handleBackPress(); // Go back if essential data is missing
        }


        // Setup bottom navigation
        setupBottomNavigation();

        // Do NOT load the default nested fragment here. Load it after data is fetched.
        // if (savedInstanceState == null) {
        //     loadNestedFragment(new GalleryFragment());
        // }


        donateButton.setOnClickListener(v -> initiateDonation());

        return view;
    }

    // Setup toolbar back navigation
    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.dog_toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                // Handle back button click - typically pop the fragment from the back stack
                getParentFragmentManager().popBackStack();
            });
            // Optional: Set toolbar title dynamically if needed
            // if (dogNameTv != null && dogNameTv.getText() != null) {
            //     toolbar.setTitle(dogNameTv.getText().toString());
            // } else {
            //     toolbar.setTitle("Dog Profile");
            // }
        }
    }


    private void loadDogData() {
        DocumentReference ref = db.collection("dogs").document(dogId);
        dogListener = ref.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                Toast.makeText(getContext(), "Error loading dog data", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Map<String, Object> data = snapshot.getData();

                // Update UI with main dog data
                dogNameTv.setText(snapshot.getString("dogName"));
                dogDescriptionTv.setText(snapshot.getString("description"));

                currentAmount = snapshot.getDouble("fundingAmount") != null ? snapshot.getDouble("fundingAmount") : 0.0;
                targetAmount = snapshot.getDouble("targetAmount") != null ? snapshot.getDouble("targetAmount") : 0.0;

                fundingAmountTv.setText(String.format("$%.2f", currentAmount));
                targetAmountTv.setText(String.format("$%.2f", targetAmount));

                Long fundingPercentage = snapshot.getLong("fundingPercentage");
                fundingProgress.setProgress(fundingPercentage != null ? fundingPercentage.intValue() : 0);

                Long urgencyLevel = snapshot.getLong("urgencyLevel");
                setUrgencyLevelText(urgencyLevel);

                // Load main image *after* getting the URL
                String mainImage = snapshot.getString("mainImage");
                if (mainImage != null && !mainImage.isEmpty()) {
                    // Use requireContext() for Glide
                    Glide.with(requireContext())
                            .load(mainImage)
                            .placeholder(R.drawable.user_icon) // Optional: Placeholder while loading
                            .error(R.drawable.user_icon) // Optional: Error image if loading fails
                            .into(dogImage);
                } else {
                    // Set a default image if mainImage URL is null or empty
                    dogImage.setImageResource(R.drawable.user_icon);
                }

                // Get gallery image URLs and supporters list
                galleryImageUrls = (ArrayList<String>) snapshot.get("galleryImages");
                supporters = (ArrayList<String>) snapshot.get("supporters");

                // --- Load Vet Information ---
                vetName = snapshot.getString("vetName");
                clinicName = snapshot.getString("clinicName");
                clinicAddress = snapshot.getString("clinicAddress");
                vetPhone = snapshot.getString("vetPhone");
                vetEmail = snapshot.getString("vetEmail");
                medicalHistory = snapshot.getString("medicalHistory");
                vetLastVisitDate = snapshot.getString("vetLastVisitDate"); // Fetch Last Visit Date
                vetDiagnosis = snapshot.getString("vetDiagnosis"); // Fetch Diagnosis

                // Check if vet info exists (at least required fields)
                hasVetInfo = (vetName != null && !vetName.isEmpty() && vetPhone != null && !vetPhone.isEmpty());
                Log.d(TAG, "Dog has vet info: " + hasVetInfo);

                // --- Data is now loaded. Load the default nested fragment (Gallery) ---
                // Only load if no nested fragment is currently in the container
                FragmentManager fragmentManager = getChildFragmentManager();
                if (fragmentManager.findFragmentById(DOG_FRAGMENT_CONTAINER_ID) == null) {
                    // Load GalleryFragment as the default when data is ready
                    loadNestedFragment(new GalleryFragment());
                    // Select the corresponding item in the bottom navigation
                    navigationView.setSelectedItemId(R.id.galleryMenu);
                } else {
                    // If a fragment is already loaded (e.g., after rotation),
                    // you might need to update its data if it's one of the data-dependent fragments.
                    // A simpler approach is to ensure nested fragments fetch data from arguments
                    // in their own lifecycle methods (like onViewCreated or onResume).
                    // The current approach of passing data via arguments and letting the nested
                    // fragment read them in onViewCreated should work.
                }


            } else {
                Log.d(TAG, "Current dog data: null");
                Toast.makeText(getContext(), "Dog data not found", Toast.LENGTH_SHORT).show();
                // Optionally handle case where dog data doesn't exist
                handleBackPress(); // Go back if dog data is missing
            }
        });
    }

    private void setUrgencyLevelText(Long urgencyLevel) {
        if (urgencyLevel != null) {
            switch (urgencyLevel.intValue()) {
                case 1:
                    urgencyLevelTv.setText("Basic Needs");
                    break;
                case 2:
                    urgencyLevelTv.setText("Mild Support");
                    break;
                case 3:
                    urgencyLevelTv.setText("Moderate Help");
                    break;
                case 4:
                    urgencyLevelTv.setText("Urgent Care");
                    break;
                case 5:
                    urgencyLevelTv.setText("Critical");
                    break;
                default:
                    urgencyLevelTv.setText("Unknown");
                    break;
            }
        } else {
            urgencyLevelTv.setText("Unknown");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Handle deep links when the fragment is resumed
        Uri data = requireActivity().getIntent().getData();
        if (data != null) {
            handleDeepLink(data);
            // Clear the intent data after handling to prevent reprocessing
            requireActivity().getIntent().setData(null);
        }
    }

    private void initiateDonation() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "Please login to donate", Toast.LENGTH_SHORT).show();
            return;
        }
        PatreonOAuthHelper.startPatreonOAuth(requireContext());
    }

    public void handleDeepLink(Uri data) {
        if (data != null && "com.happytails".equals(data.getScheme()) && "oauth".equals(data.getHost())) {
            // Check if the deep link is for OAuth redirect
            String code = data.getQueryParameter("code");
            if (code != null) {
                Log.d(TAG, "Handling OAuth code from deep link");
                handleOAuthCode(code);
            } else {
                Toast.makeText(getContext(), "Invalid OAuth response", Toast.LENGTH_SHORT).show();
            }
        } else if (data != null) {
            // Handle other potential deep links if necessary
            Log.d(TAG, "Received unhandled deep link: " + data.toString());
        }
    }

    private void handleOAuthCode(String code) {
        String userId = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.e(TAG, "No user signed in");
            Toast.makeText(getContext(), "User not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        PatreonOAuthHelper.exchangeCodeForToken(requireContext(), code, new PatreonOAuthHelper.TokenCallback() {
            @Override
            public void onSuccess(String accessToken) {
                Log.d(TAG, "Token exchanged successfully");
                // Optionally save the access token for the user if needed for future API calls
                // FirebaseFirestore.getInstance().collection("users").document(userId).update("patreonAccessToken", accessToken);

                PatreonOAuthHelper.checkPatronStatus(requireContext(), accessToken, new PatreonOAuthHelper.PatronStatusCallback() { // Pass accessToken here
                    @Override
                    public void onSuccess(boolean isPatron, double pledgeAmount) {
                        if (isPatron) {
                            // Update funding if the user is a patron and has a pledge
                            if (pledgeAmount > 0) {
                                updateFundingInFirestore(pledgeAmount);
                                Toast.makeText(getContext(), "Thank you for your donation!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "You are a patron, but your pledge is $0.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "You are not a patron yet.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Failed to check patron status: " + errorMessage);
                        Toast.makeText(getContext(), "Error verifying donation: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Token exchange failed: " + errorMessage);
                Toast.makeText(getContext(), "Donation failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateFundingInFirestore(double amount) {
        // Ensure targetAmount is not zero to avoid division by zero
        if (targetAmount <= 0) {
            Log.w(TAG, "Target amount is zero or less, cannot calculate percentage.");
            // Optionally set a default target or handle this case
            targetAmount = 100.0; // Example: set a default target if not set
        }

        double newAmount = currentAmount + amount;
        int newPercentage = (int) ((newAmount / targetAmount) * 100);
        // Cap percentage at 100
        if (newPercentage > 100) {
            newPercentage = 100;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fundingAmount", newAmount);
        updates.put("fundingPercentage", newPercentage);

        db.collection("dogs").document(dogId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Funding updated successfully");
                    // UI update is handled by the snapshot listener
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update funding", e);
                    Toast.makeText(getContext(), "Failed to update funding", Toast.LENGTH_SHORT).show();
                });
    }

    // Setup bottom navigation
    private void setupBottomNavigation() {
        navigationView.setOnItemSelectedListener(this::handleNavigation);
    }

    // Handle BottomNavigationView item clicks
    private boolean handleNavigation(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.galleryMenu) {
            fragment = new GalleryFragment();
            // Pass data to GalleryFragment
            if (galleryImageUrls != null) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("galleryImageUrls", galleryImageUrls);
                fragment.setArguments(bundle);
            }
        } else if (itemId == R.id.supportersMenu) {
            fragment = new SupportersFragment();
            // Pass data to SupportersFragment
            if (supporters != null && !supporters.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putString("dogId", dogId); // SupportersFragment might need dogId
                // You might also pass the list of supporter UIDs if you have them
                // bundle.putStringArrayList("supporters", supporters);
                fragment.setArguments(bundle);
            }
        } else if (itemId == R.id.vetMenu) { // Handle the Vet Info menu item
            fragment = new VetPageFragment(); // Use VetPageFragment here
            // Pass vet info data to VetPageFragment
            Bundle bundle = new Bundle();
            bundle.putString("vetName", vetName);
            bundle.putString("clinicName", clinicName);
            bundle.putString("clinicAddress", clinicAddress);
            bundle.putString("vetPhone", vetPhone);
            bundle.putString("vetEmail", vetEmail);
            bundle.putString("medicalHistory", medicalHistory);
            bundle.putString("vetLastVisitDate", vetLastVisitDate); // Pass Last Visit Date
            bundle.putString("diagnosis", vetDiagnosis); // Pass Diagnosis
            bundle.putBoolean("hasVetInfo", hasVetInfo); // Pass the flag
            fragment.setArguments(bundle);
        }

        if (fragment != null) {
            loadNestedFragment(fragment); // Load the selected fragment into the FrameLayout
            return true;
        }
        return false;
    }

    // Helper method to load nested fragments into the FrameLayout
    private void loadNestedFragment(Fragment fragment) {
        FragmentManager fragmentManager = getChildFragmentManager(); // Use getChildFragmentManager for nested fragments
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Add animation for transitions (optional)
        fragmentTransaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
        );

        // Replace the content of the FrameLayout with the new fragment
        fragmentTransaction.replace(DOG_FRAGMENT_CONTAINER_ID, fragment);

        // Add to back stack if you want to allow navigating back through nested fragments
        // This is important for the back button to navigate through nested tabs
        fragmentTransaction.addToBackStack(null);

        fragmentTransaction.commit();
    }


    private void handleBackPress() {
        // Handle back press for nested fragments first
        if (getChildFragmentManager().getBackStackEntryCount() > 0) {
            getChildFragmentManager().popBackStack();
        } else {
            // If no nested fragments, handle back press for the DogProfile fragment itself
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            } else {
                // If DogProfile is the root fragment, perform default back press
                requireActivity().onBackPressed();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the Firestore listener when the view is destroyed
        if (dogListener != null) {
            dogListener.remove();
        }
    }
}
