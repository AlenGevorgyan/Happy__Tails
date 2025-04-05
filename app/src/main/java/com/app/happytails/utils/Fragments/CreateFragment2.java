package com.app.happytails.utils.Fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.GalleryAdapter;
import com.app.happytails.utils.model.HomeModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;

public class CreateFragment2 extends Fragment {

    private EditText dogName, descriptionED;
    private ImageView dogGalleryPic, dogPic;
    private Button nextButton, urgencyLevelButton;
    private RecyclerView recyclerView;
    private Uri mainImageUri;
    private ProgressBar progbar;
    private final ArrayList<Uri> galleryUris = new ArrayList<>();
    private GalleryAdapter galleryAdapter;
    private TextView urgencyLevelValue;
    private int urgencyLevel = 0;

    // Firebase instances
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public CreateFragment2() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dogName = view.findViewById(R.id.dog_name);
        descriptionED = view.findViewById(R.id.dog_description);
        dogPic = view.findViewById(R.id.mainProfileImage);
        dogGalleryPic = view.findViewById(R.id.dogPic);
        recyclerView = view.findViewById(R.id.dogGallery);
        nextButton = view.findViewById(R.id.postNextBtn);
        urgencyLevelValue = view.findViewById(R.id.urgencyLevelValue);
        urgencyLevelButton = view.findViewById(R.id.urgencyLevelTitle);
        progbar = view.findViewById(R.id.create_progress);

        // Setup Firebase
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        galleryAdapter = new GalleryAdapter(getContext(), galleryUris);
        recyclerView.setAdapter(galleryAdapter);

        // Handle button click to navigate
        nextButton.setOnClickListener(v -> handleNextButtonClick());

        // Handle image selection
        dogPic.setOnClickListener(v -> openMainImageGallery());
        dogGalleryPic.setOnClickListener(v -> openGalleryForDogImages());

        // Handle urgency level selection
        urgencyLevelButton.setOnClickListener(v -> openUrgencyLevelDialog());
    }

    private void handleNextButtonClick() {
        String name = dogName.getText().toString();
        String description = descriptionED.getText().toString();
        String creatorId = auth.getCurrentUser().getUid();

        if (name.isEmpty() || description.isEmpty()) {
            Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mainImageUri != null) {
            // Show progress bar and disable next button
            progbar.setVisibility(View.VISIBLE);
            nextButton.setEnabled(false);

            uploadMainImageAndCreatePost(creatorId, name, description, urgencyLevel);
        } else {
            Toast.makeText(getContext(), "Please select a main image", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadMainImageAndCreatePost(String creatorId, String name, String description, int urgencyLevel) {
        StorageReference mainImageRef = storage.getReference().child("dog_images/" + creatorId + "/main_image.jpg");
        UploadTask uploadTask = mainImageRef.putFile(mainImageUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            mainImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                // Once the main image is uploaded, proceed to upload gallery images
                ArrayList<String> galleryImageUrls = new ArrayList<>();
                uploadGalleryImages(creatorId, name, description, urgencyLevel, uri.toString(), galleryImageUrls);
            });
        }).addOnFailureListener(e -> {
            // Hide progress bar and enable next button
            progbar.setVisibility(View.GONE);
            nextButton.setEnabled(true);
            Toast.makeText(getContext(), "Failed to upload main image", Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadGalleryImages(String creatorId, String name, String description, int urgencyLevel, String mainImageUrl, ArrayList<String> galleryImageUrls) {
        StorageReference galleryRef = storage.getReference().child("dog_images/" + creatorId + "/gallery/");

        for (int i = 0; i < galleryUris.size(); i++) {
            Uri galleryUri = galleryUris.get(i);
            StorageReference fileRef = galleryRef.child("gallery_" + i + ".jpg");
            UploadTask uploadTask = fileRef.putFile(galleryUri);

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    galleryImageUrls.add(uri.toString());

                    // Check if all gallery images have been uploaded
                    if (galleryImageUrls.size() == galleryUris.size()) {
                        createDogInFirestore(creatorId, name, description, urgencyLevel, mainImageUrl, galleryImageUrls);
                    }
                });
            }).addOnFailureListener(e -> {
                // Hide progress bar and enable next button
                progbar.setVisibility(View.GONE);
                nextButton.setEnabled(true);
                Toast.makeText(getContext(), "Failed to upload gallery image", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void createDogInFirestore(String creatorId, String name, String description, int urgencyLevel, String mainImageUrl, ArrayList<String> galleryImageUrls) {
        String dogId = firestore.collection("dogs").document().getId();

        HomeModel dog = new HomeModel(creatorId, dogId, name, 0, galleryImageUrls, mainImageUrl, new ArrayList<>(), 0.0, new ArrayList<>(), urgencyLevel);

        firestore.collection("dogs").document(dogId).set(dog)
                .addOnSuccessListener(aVoid -> {
                    // Hide progress bar and enable next button
                    progbar.setVisibility(View.GONE);
                    nextButton.setEnabled(true);
                    Toast.makeText(getContext(), "Dog account created successfully!", Toast.LENGTH_SHORT).show();
                    navigateToHomeFragment();
                })
                .addOnFailureListener(e -> {
                    // Hide progress bar and enable next button
                    progbar.setVisibility(View.GONE);
                    nextButton.setEnabled(true);
                    Toast.makeText(getContext(), "Failed to create dog account", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToHomeFragment() {
        FragmentManager fragmentManager = getParentFragmentManager();
        Fragment homeFragment = new HomeFragment();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, homeFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void openMainImageGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    private void openGalleryForDogImages() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*");
        startActivityForResult(intent, 2);
    }

    private void openUrgencyLevelDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.urgency_level_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = dialogView.findViewById(checkedId);
            String selectedText = radioButton.getText().toString();
            urgencyLevelValue.setText(selectedText);
            urgencyLevel = Integer.parseInt(radioButton.getTag().toString());
            dialog.dismiss();
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK && data != null) {
            if (requestCode == 1) {
                mainImageUri = data.getData();
                dogPic.setImageURI(mainImageUri);

            } else if (requestCode == 2) {
                galleryUris.clear();
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        galleryUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    galleryUris.add(data.getData());
                }

                galleryAdapter.notifyDataSetChanged();
            }
        }
    }
}
