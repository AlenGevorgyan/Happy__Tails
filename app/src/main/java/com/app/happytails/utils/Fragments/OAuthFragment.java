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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.app.happytails.R;
import com.app.happytails.utils.FirebaseUtil;
import com.app.happytails.utils.PatreonOAuthHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OAuthFragment extends Fragment {

    private static final String TAG = "OAuthFragment";

    private Button connectPatreonButton;
    private ProgressBar progressBar;
    private TextView successTextView;
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
        successTextView = view.findViewById(R.id.successTextViewOAuth);
        
        if (successTextView == null) {
            // If the TextView doesn't exist in layout, find a parent to add it to
            ViewGroup parent = (ViewGroup) connectPatreonButton.getParent();
            successTextView = new TextView(requireContext());
            successTextView.setId(View.generateViewId());
            successTextView.setText("Patreon Connected Successfully!");
            successTextView.setTextSize(18);
            successTextView.setTextColor(getResources().getColor(R.color.primary_color));
            successTextView.setVisibility(View.GONE);
            parent.addView(successTextView);
        }

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Handle "Connect to Patreon" button click
        connectPatreonButton.setOnClickListener(v -> startPatreonOAuth());
        
        // Check if user already has a Patreon token
        checkExistingPatreonConnection();
    }
    
    private void checkExistingPatreonConnection() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            firestore.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("patreonAccessToken")) {
                            String token = documentSnapshot.getString("patreonAccessToken");
                            if (token != null && !token.isEmpty()) {
                                // User already has a Patreon connection
                                connectPatreonButton.setVisibility(View.GONE);
                                successTextView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }
    }

    private void startPatreonOAuth() {
        // Show progress bar and disable the button
        progressBar.setVisibility(View.VISIBLE);
        connectPatreonButton.setEnabled(false);

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
                requireActivity().getIntent().setData(null); // Clear the intent data after processing
                
                // Check if arguments contain the auth code (from navigateToOAuthFragment)
                Bundle args = getArguments();
                if (args != null && args.containsKey("auth_code")) {
                    // This means we came back from OAuth - prevent back navigation
                    requireActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
            } else {
                Log.e(TAG, "Authorization failed: Missing code");
                Toast.makeText(getContext(), "Failed to connect to Patreon.", Toast.LENGTH_SHORT).show();
                resetUI();
            }
        } else {
            resetUI(); // Reset the UI if no OAuth data is present
        }
    }


    private void resetUI() {
        // Hide progress bar and enable the button
        progressBar.setVisibility(View.GONE);
        connectPatreonButton.setEnabled(true);
        connectPatreonButton.setVisibility(View.VISIBLE);
        successTextView.setVisibility(View.GONE);
    }
}