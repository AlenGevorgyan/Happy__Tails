package com.app.happytails.utils.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.app.happytails.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VetPageSelectionFragment extends Fragment {

    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private String dogName, description, patreonUrl;
    private int urgencyLevel;
    private ArrayList<Uri> galleryImages;
    private Uri mainImageUrl;

    private CircularProgressIndicator progbar;


    // Define an interface to communicate the user's choice back to the parent
    public interface OnVetInfoChoiceListener {
        void onVetInfoChoice(boolean hasVetInfo);
    }

    private OnVetInfoChoiceListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure the parent activity/fragment implements the listener interface
        if (context instanceof OnVetInfoChoiceListener) {
            listener = (OnVetInfoChoiceListener) context;
        } else {
            // If the parent doesn't implement the interface, throw an exception
            // This helps ensure the communication channel is set up correctly
            throw new RuntimeException(context.toString()
                    + " must implement OnVetInfoChoiceListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_vet_page_selection, container, false);

        Button btnYes = view.findViewById(R.id.btn_vet_info_yes);
        Button btnNo = view.findViewById(R.id.btn_vet_info_no);

        Bundle bundle = this.getArguments();
        if(bundle != null){
            dogName = bundle.getString("dogName");
            patreonUrl = bundle.getString("patreonUrl");
            description = bundle.getString("description");
            urgencyLevel = bundle.getInt("urgencyLevel");
            mainImageUrl = bundle.getParcelable("mainImageUrl");
            galleryImages = bundle.getParcelableArrayList("galleryImages");
        }

        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        progbar = view.findViewById(R.id.progress_bar);

        // Set click listener for the "Yes" button
        btnYes.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVetInfoChoice(true);
            }
            VetFragment vetFragment = new VetFragment();
            vetFragment.setArguments(bundle);
            FragmentManager fragmentManager = getParentFragmentManager(); // Use getParentFragmentManager()
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.fragment_container, vetFragment); // <-- REPLACE R.id.fragment_container
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

            ft.addToBackStack(null);

            ft.commit();
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        // Set click listener for the "No" button
        btnNo.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVetInfoChoice(false); // Communicate "No" choice
            }
            saveDogInfo(dogName, description, urgencyLevel, patreonUrl, mainImageUrl, galleryImages);
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        return view;
    }

    private void saveDogInfo(String name, String description, int urgencyLevel, String patreonUrl, Uri mainImageUri, ArrayList<Uri> galleryUris) {
        progbar.setVisibility(View.VISIBLE);
        // Upload main image to Firebase Storage
        StorageReference mainImageRef = storage.getReference().child("dogs").child(System.currentTimeMillis() + "_main.jpg");
        mainImageRef.putFile(mainImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        mainImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String mainImageUrl = uri.toString();

                            // For gallery images, similar upload logic can be implemented.
                            // For brevity, we'll use an empty list for gallery URLs.
                            ArrayList<String> galleryUrls = new ArrayList<>();

                            // Prepare a map of dog info to store in Firestore.
                            Map<String, Object> dogData = new HashMap<>();
                            dogData.put("name", name);
                            dogData.put("description", description);
                            dogData.put("urgencyLevel", urgencyLevel);
                            dogData.put("patreonUrl", patreonUrl);
                            dogData.put("mainImageUrl", mainImageUrl);
                            dogData.put("galleryUrls", galleryUrls);

                            firestore.collection("dogs")
                                    .add(dogData)
                                    .addOnSuccessListener(documentReference -> {
                                        progbar.setVisibility(View.GONE);
                                        FragmentManager fragmentManager = getParentFragmentManager(); // Use getParentFragmentManager()
                                        FragmentTransaction ft = fragmentManager.beginTransaction();
                                        HomeFragment fragment = new HomeFragment();

                                        // Replace the current fragment with the VetPageSelectionFragment
                                        // IMPORTANT: Replace R.id.fragment_container with the actual ID of the
                                        // container view in your Activity's layout where these fragments are displayed.
                                        ft.replace(R.id.fragment_container, fragment); // <-- REPLACE R.id.fragment_container

                                        // Add animation for the transition (optional)
                                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

                                        ft.addToBackStack(null);

                                        ft.commit();
                                        Toast.makeText(getContext(), "Dog saved successfully", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        progbar.setVisibility(View.GONE);
                                        Toast.makeText(getContext(), "Error saving dog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }).addOnFailureListener(e -> {
                            progbar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Error getting download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    progbar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null; // Release the listener when the fragment is detached
    }
}
