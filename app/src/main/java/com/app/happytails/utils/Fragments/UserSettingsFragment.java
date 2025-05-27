package com.app.happytails.utils.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.Toolbar;

import com.app.happytails.R;
import com.app.happytails.utils.ForgetPassword;
import com.app.happytails.utils.SignInActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class UserSettingsFragment extends Fragment {

    private Toolbar toolbar;
    private ImageButton backBtn;
    private LinearLayout editPageLayout, logoutLayout, helpLayout, changePasswordLayout;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        
        toolbar = view.findViewById(R.id.settings_toolbar);
        backBtn = view.findViewById(R.id.settingsBackBtn);
        editPageLayout = view.findViewById(R.id.edit_page_layout);
        logoutLayout = view.findViewById(R.id.logout_layout);
        helpLayout = view.findViewById(R.id.help_layout);
        changePasswordLayout = view.findViewById(R.id.change_password_layout);

        backBtn.setOnClickListener(v -> requireActivity().onBackPressed());

        editPageLayout.setOnClickListener(v -> {
            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new EditPageFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        logoutLayout.setOnClickListener(v -> showLogoutConfirmationDialog());

        helpLayout.setOnClickListener(v -> showHelpDialog());

        changePasswordLayout.setOnClickListener(v -> navigateToForgotPassword());
    }

    private void navigateToForgotPassword() {
        // Navigate to the forgot password screen
        Intent intent = new Intent(getActivity(), ForgetPassword.class);
        intent.putExtra("showForgotPassword", true);
        startActivity(intent);
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                // Unsubscribe from FCM topic
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users");
                
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut();
                
                // Navigate to SignInActivity
                Intent intent = new Intent(getActivity(), SignInActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void showHelpDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Help")
            .setMessage("Need help? Contact us at:\n\nsupport@happytails.com\n\nWe'll get back to you as soon as possible!")
            .setPositiveButton("OK", null)
            .show();
    }
}
