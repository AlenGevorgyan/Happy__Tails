package com.app.happytails.utils.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.bumptech.glide.Glide;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class DogProfile extends Fragment {

    private TextView dogNameTv, dogDescriptionTv, urgencyLevelTv;
    private ProgressBar fundingProgress;
    private ImageButton backBtn;
    private CircleImageView dogImage;
    private BottomNavigationView navigationView;
    private String dogId;
    private FirebaseFirestore db;
    private ListenerRegistration dogListener;
    private ArrayList<String> galleryImageUrls, supporters;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dog_profile, container, false);

        dogNameTv = view.findViewById(R.id.dogNameTV);
        dogDescriptionTv = view.findViewById(R.id.descriptionTV);
        urgencyLevelTv = view.findViewById(R.id.urgencyLevelValue);
        fundingProgress = view.findViewById(R.id.funding_bar_profile);
        dogImage = view.findViewById(R.id.dogProfileImage);
        navigationView = view.findViewById(R.id.dogBottomNavigation);
        backBtn = view.findViewById(R.id.dogBackBtn);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            dogId = getArguments().getString("dogId");
        }

        if (dogId != null) {
            loadDogData();
        } else {
            Toast.makeText(getContext(), "Dog ID is missing", Toast.LENGTH_SHORT).show();
        }

        navigationView.setOnNavigationItemSelectedListener(this::handleNavigation);
        backBtn.setOnClickListener(v -> handleBackPress());

        return view;
    }

    private void loadDogData() {
        DocumentReference dogRef = db.collection("dogs").document(dogId);

        dogListener = dogRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String dogName = snapshot.getString("dogName");
                String dogDescription = snapshot.getString("description");
                String profileImageUrl = snapshot.getString("mainImage");
                Long fundingPercentage = snapshot.getLong("fundingPercentage");
                Long urgencyLevel = snapshot.getLong("urgencylevel");

                galleryImageUrls = (ArrayList<String>) snapshot.get("galleryImages");
                supporters = (ArrayList<String>) snapshot.get("supporters");

                // Set data to views
                dogNameTv.setText(dogName);
                dogDescriptionTv.setText(dogDescription);
                fundingProgress.setProgress(fundingPercentage != null ? fundingPercentage.intValue() : 0);
                urgencyLevelTv.setText(String.valueOf(urgencyLevel != null ? urgencyLevel.intValue() : 0));
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

                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(getContext()).load(profileImageUrl).into(dogImage);
                } else {
                    dogImage.setImageResource(R.drawable.baseline_add_24);
                }
            } else {
                Toast.makeText(getContext(), "Dog data not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleBackPress() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else {
            requireActivity().onBackPressed();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dogListener != null) {
            dogListener.remove();
        }
    }
}