package com.app.happytails.utils.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;

import com.app.happytails.R;
import com.app.happytails.utils.SignInActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class UserSettingsFragment extends Fragment {

    private Toolbar toolbar;
    private ImageButton backBtn;
    private View editPage, logout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        toolbar = view.findViewById(R.id.profile_settings_toolbar);
        backBtn = view.findViewById(R.id.back_profile_settings);
        editPage = view.findViewById(R.id.edit_page_card);
        logout = view.findViewById(R.id.logout_card);

        backBtn.setOnClickListener(v -> requireActivity().onBackPressed());

        editPage.setOnClickListener(v -> {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new EditPageFragment())
                    .addToBackStack(null)
                    .commit();
        });

        logout.setOnClickListener(v -> {

            FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(getActivity(), SignInActivity.class));
                        getActivity().finish();
                    }
                }
            });

        });
    }
}
