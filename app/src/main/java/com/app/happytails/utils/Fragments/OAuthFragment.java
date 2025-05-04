package com.app.happytails.utils.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.happytails.R;
import com.app.happytails.utils.PatreonOAuthHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OAuthFragment extends Fragment {

    private static final String TAG = "OAuthFragment";

    private Button connectPatreonButton;
    private ProgressBar progressBar;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public OAuthFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_oauth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        connectPatreonButton = view.findViewById(R.id.connectPatreonButton);
        progressBar = view.findViewById(R.id.progressBarOAuth);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Handle "Connect to Patreon" button click
        connectPatreonButton.setOnClickListener(v -> startPatreonOAuth());
    }

    private void startPatreonOAuth() {
        // Show progress bar and disable the button
        progressBar.setVisibility(View.VISIBLE);
        connectPatreonButton.setEnabled(false);

        // Start the OAuth flow
        PatreonOAuthHelper.startPatreonOAuthForOwners(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();

        // Handle OAuth Redirect
        Intent intent = requireActivity().getIntent();
        Uri data = intent.getData();

        if (data != null && data.toString().contains("code")) {
            String code = data.getQueryParameter("code");
            if (code != null) {
                exchangeOAuthCodeForToken(code);
                requireActivity().getIntent().setData(null); // Clear the intent data after processing
            } else {
                Log.e(TAG, "Authorization failed: Missing code");
                Toast.makeText(getContext(), "Failed to connect to Patreon.", Toast.LENGTH_SHORT).show();
                resetUI();
            }
        } else {
            resetUI(); // Reset the UI if no OAuth data is present
        }
    }

    private void exchangeOAuthCodeForToken(String code) {
        PatreonOAuthHelper.exchangeCodeForToken(requireContext(), code, new PatreonOAuthHelper.OAuthCallback() {
            @Override
            public void onSuccess(String accessToken) {
                Log.d(TAG, "Successfully authenticated with Patreon. Token: " + accessToken);

                // Save the access token to Firestore
                saveAccessTokenToFirestore(accessToken);

                Toast.makeText(getContext(), "Patreon connected successfully!", Toast.LENGTH_SHORT).show();
                resetUI();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to authenticate with Patreon: " + errorMessage);
                Toast.makeText(getContext(), "Failed to connect to Patreon.", Toast.LENGTH_SHORT).show();
                resetUI();
            }
        });
    }

    private void saveAccessTokenToFirestore(String accessToken) {
        String userId = auth.getCurrentUser().getUid();

        // Save the token under the user's document in Firestore
        firestore.collection("users").document(userId)
                .update("patreonAccessToken", accessToken)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Access token saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save access token: " + e.getMessage()));
    }

    private void resetUI() {
        // Hide progress bar and enable the button
        progressBar.setVisibility(View.GONE);
        connectPatreonButton.setEnabled(true);
    }
}