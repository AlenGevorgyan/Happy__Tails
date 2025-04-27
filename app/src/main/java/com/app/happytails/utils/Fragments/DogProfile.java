package com.app.happytails.utils.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
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
import com.app.happytails.utils.Fragments.GalleryFragment;
import com.app.happytails.utils.Fragments.SupportersFragment;
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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class DogProfile extends Fragment {

    private static final String TAG = "DogProfile";
    private static final String CLIENT_ID = "rInp7p5sJ3MZwEIh4_wZaljG1UISzx2R7w-hPVhPbe1XfZJ44-6nQX9jtzvzQLWc"; // Replace with your actual Client ID
    private static final String REDIRECT_URI = "https://happytails.page.link/UkMX"; // Replace with your actual Redirect URI (e.g., your Firebase Dynamic Link)

    private TextView dogNameTv, dogDescriptionTv, urgencyLevelTv;
    private TextView fundingAmountTv, targetAmountTv;
    private ProgressBar fundingProgress;
    private ImageButton backBtn;
    private CircleImageView dogImage;
    private BottomNavigationView navigationView;
    private Button donateButton;

    private String dogId;
    private String patreonUrl;
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

        if (getArguments() != null) dogId = getArguments().getString("dogId");
        if (dogId != null) loadDogData();

        navigationView.setOnNavigationItemSelectedListener(this::handleNavigation);
        backBtn.setOnClickListener(v -> handleBackPress());
        donateButton.setOnClickListener(v -> startDonationProcess());

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

            currentAmount = snapshot.getDouble("fundingAmount") != null ?
                    snapshot.getDouble("fundingAmount") : 0.0;
            targetAmount = snapshot.getDouble("targetAmount") != null ?
                    snapshot.getDouble("targetAmount") : 0.0;

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

            patreonUrl = snapshot.getString("patreonUrl");
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

    private void startDonationProcess() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("PatreonPrefs", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("patreon_access_token", null);

        if (accessToken != null) {
            // Use helper to open the campaign page for this dog
            PatreonOAuthHelper.openCreatorCampaigns(requireContext(), accessToken, dogId);
        } else {
            startOAuthFlow();
        }
    }

    private void startOAuthFlow() {
        // Use helper to get the OAuth URL with all required scopes and state
        String authUrl = PatreonOAuthHelper.getAuthorizationUrl();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
        startActivity(intent);
    }

    // Call this from MainActivity or wherever you handle deep links
    public void handleOAuthRedirect(Uri data) {
        Log.d(TAG, "Handling OAuth Redirect in DogProfile (from Browser): " + data);
        // Verify state parameter for CSRF protection
        if (!PatreonOAuthHelper.verifyOAuthResponse(data)) {
            Toast.makeText(requireContext(), "Invalid OAuth state. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        String code = data.getQueryParameter("code");
        if (code != null) {
            sendCodeToServer(code);
        } else if (data.getQueryParameter("error") != null) {
            String error = data.getQueryParameter("error");
            String errorDescription = data.getQueryParameter("error_description");
            Log.e(TAG, "OAuth Error in Redirect (Browser): " + error + " - " + errorDescription);
            Toast.makeText(requireContext(), "Patreon authentication failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendCodeToServer(String code) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        // Use OkHttp to send the code to your backend (as before)
        RequestBody requestBody = new FormBody.Builder()
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("firebaseUid", uid)
                .build();

        Request request = new Request.Builder()
                .url("https://us-central1-rational-photon-380817.cloudfunctions.net/exchangePatreonCode") // Replace with your Cloud Function URL
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error communicating with the backend.", Toast.LENGTH_LONG).show());
                    Log.e(TAG, "Error sending code to backend: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body().string();
                Log.d(TAG, "Backend response: " + responseData);

                if (response.isSuccessful()) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Patreon authentication successful.", Toast.LENGTH_SHORT).show();
                            onPatreonAuthSuccess(); // Notify DogProfile
                        });
                    }
                } else {
                    Log.e(TAG, "Backend error during token exchange: " + response.code() + " - " + responseData);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Backend error during token exchange.", Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }

    public void onPatreonAuthSuccess() {
        Toast.makeText(getContext(), "Patreon authentication successful!", Toast.LENGTH_SHORT).show();
        // After successful auth, open the campaign page for this dog
        SharedPreferences prefs = requireActivity().getSharedPreferences("PatreonPrefs", Context.MODE_PRIVATE);
        String accessToken = prefs.getString("patreon_access_token", null);
        if (accessToken != null) {
            PatreonOAuthHelper.openCreatorCampaigns(requireContext(), accessToken, dogId);
        } else {
            Toast.makeText(getContext(), "Access token not found. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    // Call this from MainActivity or deep link handler after Patreon redirects back
    public void handlePatreonReturn(Uri data) {
        // Check if this is a successful donation
        if (PatreonOAuthHelper.checkDonationSuccess(data)) {
            double amount = PatreonOAuthHelper.getPledgeAmount(data);
            String returnedDogId = PatreonOAuthHelper.getDogIdFromSuccessUrl(data);
            if (returnedDogId != null && !returnedDogId.isEmpty()) {
                PatreonOAuthHelper.updateFundingAmount(returnedDogId, amount, new PatreonOAuthHelper.DonationCallback() {
                    @Override
                    public void onSuccess(double newAmount, int fundingPercentage) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Thank you for your donation!", Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Failed to update funding: " + errorMessage, Toast.LENGTH_LONG).show()
                        );
                    }
                });
            }
        }
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
        if (dogListener != null) dogListener.remove();
    }
}