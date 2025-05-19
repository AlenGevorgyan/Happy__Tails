package com.app.happytails.utils.Fragments;

import android.content.Context;
import android.content.Intent;
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
import org.json.JSONException;
import org.json.JSONObject;

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
        httpClient = new OkHttpClient();

        // Setup toolbar back navigation
        setupToolbar(view);

        // Get dogId from arguments
        if (getArguments() != null) {
            dogId = getArguments().getString("dogId");
            Log.d(TAG, "DogProfile created with dogId: " + dogId);
        }

        // Load data from Firestore
        if (dogId != null) {
            loadDogData();
        } else {
            // Handle case where dogId is missing (shouldn't happen if navigated correctly)
            Log.e(TAG, "Dog ID is missing in arguments!");
            Toast.makeText(getContext(), "Error: Dog ID not provided.", Toast.LENGTH_SHORT).show();
            handleBackPress(); // Go back if essential data is missing
            return view;
        }

        // Setup bottom navigation
        setupBottomNavigation();

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
            Log.e(TAG, "User not authenticated");
            requireActivity().runOnUiThread(() -> 
                Toast.makeText(requireContext(), "Please log in to donate", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        if (dogId == null) {
            Log.e(TAG, "Dog ID is null");
            requireActivity().runOnUiThread(() -> 
                Toast.makeText(requireContext(), "Error: Dog ID not found", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        Log.d(TAG, "Starting donation process for user: " + user.getUid() + ", dog: " + dogId);

        // First get the dog's Patreon URL from Firestore
        db.collection("dogs").document(dogId)
            .get()
            .addOnSuccessListener(dogSnapshot -> {
                if (!dogSnapshot.exists()) {
                    Log.e(TAG, "Dog document not found");
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Error: Dog profile not found", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String patreonUrl = dogSnapshot.getString("patreonUrl");
                if (patreonUrl == null || patreonUrl.isEmpty()) {
                    Log.e(TAG, "Patreon URL not found for dog");
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "Error: Patreon link not found", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // Check if user already has Patreon tokens
                db.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(userSnapshot -> {
                        if (userSnapshot.exists()) {
                            String accessToken = userSnapshot.getString("patreonAccessToken");
                            if (accessToken != null && !accessToken.isEmpty()) {
                                Log.d(TAG, "User has Patreon tokens, checking patron status");
                                // User has tokens, check patron status
                                PatreonOAuthHelper.checkPatronStatus(requireContext(), user.getUid(), new PatreonOAuthHelper.PatronStatusCallback() {
                                    @Override
                                    public void onSuccess(boolean isPatron, double pledgeAmount) {
                                        if (isAdded()) {
                                            requireActivity().runOnUiThread(() -> {
                                                if (isPatron) {
                                                    // User is already a patron, show their pledge amount
                                                    Log.d(TAG, "User is already a patron with pledge: $" + pledgeAmount);
                                                    Toast.makeText(requireContext(),
                                                        String.format("You are already supporting with $%.2f/month", pledgeAmount),
                                                        Toast.LENGTH_LONG).show();
                                                    updateDonationUI(true, pledgeAmount);
                                                } else {
                                                    // User has tokens but isn't a patron, redirect to Patreon
                                                    Log.d(TAG, "User has tokens but isn't a patron, redirecting to Patreon");
                                                    Toast.makeText(requireContext(), "Redirecting to Patreon...", Toast.LENGTH_SHORT).show();
                                                    // Open Patreon URL directly
                                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(patreonUrl));
                                                    startActivity(intent);
                                                }
                                            });
                                        }
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        if (isAdded()) {
                                            Log.e(TAG, "Failed to check patron status: " + errorMessage);
                                            // If check fails, redirect to Patreon
                                            requireActivity().runOnUiThread(() -> {
                                                Log.d(TAG, "Redirecting to Patreon due to status check failure");
                                                Toast.makeText(requireContext(), "Redirecting to Patreon...", Toast.LENGTH_SHORT).show();
                                                // Open Patreon URL directly
                                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(patreonUrl));
                                                startActivity(intent);
                                            });
                                        }
                                    }
                                });
                            } else {
                                // No existing tokens, start OAuth flow
                                Log.d(TAG, "No existing Patreon tokens, starting OAuth");
                                requireActivity().runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), "Starting Patreon authorization...", Toast.LENGTH_SHORT).show();
                                    PatreonOAuthHelper.startPatreonOAuth(requireContext(), dogId);
                                });
                            }
                        } else {
                            Log.e(TAG, "User document not found");
                            requireActivity().runOnUiThread(() -> 
                                Toast.makeText(requireContext(), "Error: User profile not found", Toast.LENGTH_SHORT).show()
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking user document", e);
                        requireActivity().runOnUiThread(() -> 
                            Toast.makeText(requireContext(), "Error checking donation status", Toast.LENGTH_SHORT).show()
                        );
                    });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching dog document", e);
                requireActivity().runOnUiThread(() -> 
                    Toast.makeText(requireContext(), "Error fetching dog profile", Toast.LENGTH_SHORT).show()
                );
            });
    }

    private void updateDonationUI(boolean isPatron, double pledgeAmount) {
        if (!isAdded()) return;

        // Update the donate button text and appearance
        if (donateButton != null) {
            if (isPatron) {
                donateButton.setText(String.format("Supporting: $%.2f/month", pledgeAmount));
                donateButton.setEnabled(false); // Disable button if already supporting
                // Optionally change button color or style
                donateButton.setBackgroundTintList(getResources().getColorStateList(R.color.success_green));
            } else {
                donateButton.setText("Donate Now");
                donateButton.setEnabled(true);
                // Reset button style
                donateButton.setBackgroundTintList(getResources().getColorStateList(R.color.accent_color));
            }
        }

        // Update funding progress if needed
        if (isPatron) {
            // Add pledge amount to current funding
            currentAmount += pledgeAmount;
            updateFundingUI();
        }
    }

    private void updateFundingUI() {
        if (fundingAmountTv != null && targetAmountTv != null && fundingProgress != null) {
            fundingAmountTv.setText(String.format("$%.2f", currentAmount));
            targetAmountTv.setText(String.format("$%.2f", targetAmount));

            int progress = (int) ((currentAmount / targetAmount) * 100);
            fundingProgress.setProgress(progress);
        }
    }

    // Update handleDeepLink to use the new UI update method
    public void handleDeepLink(Uri data) {
        if (!isAdded()) {
            Log.e(TAG, "Fragment not attached to activity");
            return;
        }

        if (data == null) {
            Log.e(TAG, "Deep link data is null");
            return;
        }

        String code = data.getQueryParameter("code");
        String state = data.getQueryParameter("state");

        Log.d(TAG, "Received deep link - code: " + (code != null ? "present" : "null"));
        Log.d(TAG, "Received deep link - state: " + (state != null ? "present" : "null"));

        if (code == null || state == null) {
            Log.e(TAG, "Missing code or state in deep link");
            Toast.makeText(requireContext(), "Error: Missing required parameters", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse state to get userId and dogId
            JSONObject stateData = new JSONObject(state);
            String userId = stateData.getString("userId");
            String receivedDogId = stateData.getString("dogId");

            Log.d(TAG, "Parsed state - userId: " + userId + ", dogId: " + receivedDogId);

            // Verify we're on the correct dog profile
            if (!receivedDogId.equals(dogId)) {
                Log.e(TAG, "Dog ID mismatch - received: " + receivedDogId + ", current: " + dogId);
                Toast.makeText(requireContext(), "Error: Wrong dog profile", Toast.LENGTH_SHORT).show();
                return;
            }

            // Exchange code for tokens
            Log.d(TAG, "Exchanging authorization code for tokens");
            PatreonOAuthHelper.exchangeCodeForTokens(requireContext(), code, new PatreonOAuthHelper.TokenExchangeCallback() {
                @Override
                public void onSuccess(String accessToken, String refreshToken) {
                    Log.d(TAG, "Successfully obtained access tokens");
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Authorization successful!", Toast.LENGTH_SHORT).show();

                        // Check patron status using the new access token
                        PatreonOAuthHelper.checkPatronStatus(requireContext(), userId, new PatreonOAuthHelper.PatronStatusCallback() {
                            @Override
                            public void onSuccess(boolean isPatron, double pledgeAmount) {
                                if (isAdded()) {
                                    if (isPatron) {
                                        Log.d(TAG, "User is a patron with pledge amount: $" + pledgeAmount);
                                        Toast.makeText(requireContext(),
                                            String.format("Thank you for your $%.2f pledge!", pledgeAmount),
                                            Toast.LENGTH_LONG).show();
                                        updateDonationUI(true, pledgeAmount);
                                    } else {
                                        Log.d(TAG, "User is not a patron");
                                        updateDonationUI(false, 0);
                                    }
                                    // Reload data to show updated status
                                    loadDogData();
                                }
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                if (isAdded()) {
                                    Log.e(TAG, "Failed to check patron status: " + errorMessage);
                                    Toast.makeText(requireContext(),
                                        "Authorization successful, but couldn't verify pledge status",
                                        Toast.LENGTH_LONG).show();
                                    updateDonationUI(false, 0);
                                    loadDogData(); // Still reload data
                                }
                            }
                        });
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Failed to exchange code for tokens: " + errorMessage);
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                            "Error completing authorization: " + errorMessage,
                            Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse state JSON", e);
            Toast.makeText(requireContext(), "Error: Invalid state data", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFundingProgress(int newPledgeAmountCents) {
        if (dogId != null) {
            db.collection("dogs").document(dogId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double currentFundingAmount = documentSnapshot.getDouble("fundingAmount") != null ?
                            documentSnapshot.getDouble("fundingAmount") : 0.0;
                        double newAmount = currentFundingAmount + (newPledgeAmountCents / 100.0);

                        // Update the funding amount in Firestore
                        db.collection("dogs").document(dogId)
                            .update("fundingAmount", newAmount)
                            .addOnSuccessListener(aVoid -> {
                                // Update UI
                                currentAmount = newAmount;
                                updateFundingUI();

                                // Log success
                                Log.d(TAG, "Funding amount updated successfully. New amount: $" + newAmount);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating funding amount", e);
                                Toast.makeText(getContext(), "Error updating funding amount", Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        Log.e(TAG, "Dog document not found");
                        Toast.makeText(getContext(), "Error: Dog profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching dog document", e);
                    Toast.makeText(getContext(), "Error fetching dog profile", Toast.LENGTH_SHORT).show();
                });
        }
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

    private void checkPatronStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(requireContext(), "Please log in to check donation status", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Checking patron status for user: " + user.getUid());
        PatreonOAuthHelper.checkPatronStatus(requireContext(), user.getUid(), new PatreonOAuthHelper.PatronStatusCallback() {
            @Override
            public void onSuccess(boolean isPatron, double pledgeAmount) {
                if (isAdded()) {
                    if (isPatron) {
                        Log.d(TAG, "User is a patron with pledge: $" + pledgeAmount);
                        Toast.makeText(requireContext(),
                            String.format("You are supporting with $%.2f/month", pledgeAmount),
                            Toast.LENGTH_LONG).show();
                        updateDonationUI(true, pledgeAmount);
                    } else {
                        Log.d(TAG, "User is not a patron");
                        Toast.makeText(requireContext(), "You are not currently supporting", Toast.LENGTH_SHORT).show();
                        updateDonationUI(false, 0);
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    Log.e(TAG, "Failed to check patron status: " + errorMessage);
                    Toast.makeText(requireContext(), "Error checking donation status", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
