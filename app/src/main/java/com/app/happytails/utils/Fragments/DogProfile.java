package com.app.happytails.utils.Fragments;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.OkHttpClient;

public class DogProfile extends Fragment {

    private static final String TAG = "DogProfile";

    private TextView dogNameTv, dogDescriptionTv, urgencyLevelTv;
    private TextView fundingAmountTv, targetAmountTv;
    private ProgressBar fundingProgress;
    private ImageButton backBtn;
    private CircleImageView dogImage;
    private BottomNavigationView navigationView;
    private Button donateButton;

    private String dogId;
    private FirebaseFirestore db;
    private ListenerRegistration dogListener;
    private ArrayList<String> galleryImageUrls, supporters;
    private double targetAmount, currentAmount;
    private FirebaseAuth firebaseAuth;
    private OkHttpClient httpClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dog_profile, container, false);
        dogNameTv = view.findViewById(R.id.dogNameTV);
        dogDescriptionTv = view.findViewById(R.id.descriptionTV);
        urgencyLevelTv = view.findViewById(R.id.urgencyLevelValue);
        fundingProgress = view.findViewById(R.id.funding_bar_profile);
        fundingAmountTv = view.findViewById(R.id.fundingAmountTV);
        targetAmountTv = view.findViewById(R.id.targetAmountTV);
        dogImage = view.findViewById(R.id.dogProfileImage);
        navigationView = view.findViewById(R.id.dogBottomNavigation);
        backBtn = view.findViewById(R.id.dogBackBtn);
        donateButton = view.findViewById(R.id.donateButton);

        db = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        httpClient = new OkHttpClient();

        if (getArguments() != null) {
            dogId = getArguments().getString("dogId");
        }
        if (dogId != null) {
            loadDogData();
        }

        navigationView.setOnNavigationItemSelectedListener(this::handleNavigation);
        backBtn.setOnClickListener(v -> handleBackPress());
        donateButton.setOnClickListener(v -> initiateDonation());

        return view;
    }

    private void loadDogData() {
        DocumentReference ref = db.collection("dogs").document(dogId);
        dogListener = ref.addSnapshotListener((snapshot, e) -> {
            if (e != null || snapshot == null || !snapshot.exists()) {
                Toast.makeText(getContext(), "Error loading dog data", Toast.LENGTH_SHORT).show();
                return;
            }

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

            String mainImage = snapshot.getString("mainImage");
            if (mainImage != null && !mainImage.isEmpty()) {
                Glide.with(requireContext()).load(mainImage).into(dogImage);
            }

            galleryImageUrls = (ArrayList<String>) snapshot.get("galleryImages");
            supporters = (ArrayList<String>) snapshot.get("supporters");
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
        Uri data = requireActivity().getIntent().getData();
        if (data != null) {
            String code = data.getQueryParameter("code");
            if (code != null) {
                handleOAuthCode(code);
            } else {
                Toast.makeText(getContext(), "Invalid OAuth response", Toast.LENGTH_SHORT).show();
            }
            requireActivity().getIntent().setData(null); // Clear intent to prevent reprocessing
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
        if (data != null && "https".equals(data.getScheme())) {
            String code = data.getQueryParameter("code");
            if (code != null) {
                handleOAuthCode(code);
            } else {
                Toast.makeText(getContext(), "Invalid OAuth response", Toast.LENGTH_SHORT).show();
            }
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
                Log.d(TAG, "Token exchanged successfully: " + accessToken);
                PatreonOAuthHelper.checkPatronStatus(requireContext(), userId, new PatreonOAuthHelper.PatronStatusCallback() {
                    @Override
                    public void onSuccess(boolean isPatron, double pledgeAmount) {
                        if (isPatron) {
                            updateFundingInFirestore(pledgeAmount);
                            Toast.makeText(getContext(), "Thank you for your donation!", Toast.LENGTH_SHORT).show();
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
        double newAmount = currentAmount + amount;
        int newPercentage = (int) ((newAmount / targetAmount) * 100);
        db.collection("dogs").document(dogId)
                .update("fundingAmount", newAmount, "fundingPercentage", newPercentage)
                .addOnSuccessListener(aVoid -> {
                    fundingAmountTv.setText(String.format("$%.2f", newAmount));
                    fundingProgress.setProgress(newPercentage);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update funding", e);
                    Toast.makeText(getContext(), "Failed to update funding", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean handleNavigation(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.galleryMenu) {
            fragment = new GalleryFragment();
            if (galleryImageUrls != null) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("galleryImageUrls", galleryImageUrls);
                fragment.setArguments(bundle);
            }
        } else if (itemId == R.id.supportersMenu) {
            fragment = new SupportersFragment();
            if (supporters != null && !supporters.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putString("dogId", dogId);
                fragment.setArguments(bundle);
            }
        }

        if (fragment != null) {
            loadFragment(fragment);
            return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.dog_fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void handleBackPress() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            requireActivity().onBackPressed();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dogListener != null) {
            dogListener.remove();
        }
    }
}