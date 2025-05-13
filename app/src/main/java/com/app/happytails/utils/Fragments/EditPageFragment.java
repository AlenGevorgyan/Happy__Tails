package com.app.happytails.utils.Fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.happytails.R;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar; // Keep if toolbar is used in layout
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
// Import Firebase Storage classes
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

// Removed Cloudinary imports
// import com.cloudinary.Cloudinary;
// import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditPageFragment extends Fragment {

    private EditText usernameEt, statusEt;
    private CircleImageView profileImage;
    private Button saveBtn;
    private ProgressBar progressBar;
    private ImageButton backButton; // Keep if back button is ImageButton

    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private StorageReference storageReference; // Firebase Storage Reference

    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    private static final String TAG = "EditPageFragment"; // Added TAG for logging

    public EditPageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();
        // Initialize Firebase Storage Reference
        storageReference = FirebaseStorage.getInstance().getReference("profile_images"); // Create a reference to 'profile_images' folder

        // Initialize views
        usernameEt = view.findViewById(R.id.emailEditText); // Assuming this is for username now
        statusEt = view.findViewById(R.id.passwordEditText); // Assuming this is for status now
        profileImage = view.findViewById(R.id.profileImageEdit);
        saveBtn = view.findViewById(R.id.saveProfileBtn);
        backButton = view.findViewById(R.id.editBackBtn); // Assuming back button ID
        progressBar = view.findViewById(R.id.progressBar);

        // Setup toolbar back navigation if your layout uses MaterialToolbar with navigation icon
        setupToolbar(view);

        loadUserData();

        profileImage.setOnClickListener(v -> openImagePicker());

        saveBtn.setOnClickListener(v -> updateProfile());

        // If not using toolbar navigation, use this back button listener
        if (backButton != null) {
            backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        }
    }

    // Setup toolbar back navigation if your layout uses MaterialToolbar with navigation icon
    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.edit_page_toolbar); // Assuming toolbar ID
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                // Handle back button click - typically pop the fragment from the back stack
                requireActivity().onBackPressed(); // Use requireActivity()
            });
            // Optional: Set toolbar title
            // toolbar.setTitle("Edit Profile");
        }
    }


    private void loadUserData() {
        if (currentUser == null) {
            Log.e(TAG, "No current user signed in.");
            // Optionally navigate back or show an error
            Toast.makeText(getContext(), "User not signed in.", Toast.LENGTH_SHORT).show();
            // requireActivity().onBackPressed(); // Example: go back if no user
            return;
        }

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String status = documentSnapshot.getString("status");
                String profileURL = documentSnapshot.getString("userImage"); // Field name in Firestore

                if (username != null) usernameEt.setText(username);
                if (status != null) statusEt.setText(status);

                if (profileURL != null && !profileURL.isEmpty()) {
                    // Use requireContext() for Glide
                    Glide.with(requireContext())
                            .load(profileURL)
                            .placeholder(R.drawable.user_icon) // Optional: Placeholder
                            .error(R.drawable.user_icon) // Optional: Error image
                            .into(profileImage);
                } else {
                    // Set a default image if no profile URL is available
                    profileImage.setImageResource(R.drawable.user_icon);
                }
            } else {
                Log.d(TAG, "User document does not exist.");
                // Optionally set default values or show a message
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load user data", e);
            Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Use startActivityForResult from the fragment
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri); // Display the selected image
        }
    }

    // Removed getFileExtension as it's not strictly needed for Firebase Storage upload

    private void updateProfile() {
        String newUsername = usernameEt.getText().toString().trim();
        String newStatus = statusEt.getText().toString().trim();

        if (newUsername.isEmpty()) {
            usernameEt.setError("Username cannot be empty");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        if (imageUri != null) {
            // Upload image to Firebase Storage
            uploadImageToFirebaseStorage(newUsername, newStatus);
        } else {
            // No new image selected, just save profile data
            saveProfileToDatabase(newUsername, newStatus, null); // Pass null for image URL
        }
    }

    private void uploadImageToFirebaseStorage(String username, String status) {
        if (currentUser == null) {
            Log.e(TAG, "Cannot upload image, user not signed in.");
            Toast.makeText(getContext(), "Error: User not signed in.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Create a unique file name for the image (e.g., using user ID and timestamp)
        StorageReference fileReference = storageReference.child(currentUser.getUid() + "_" + System.currentTimeMillis() + ".jpg"); // Using .jpg extension

        // Start the upload task
        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the download URL after successful upload
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d(TAG, "Image uploaded successfully. Download URL: " + downloadUrl);
                        // Save profile data with the new image URL
                        saveProfileToDatabase(username, status, downloadUrl);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get download URL", e);
                        Toast.makeText(getContext(), "Failed to get image URL", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed", e);
                    Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnProgressListener(taskSnapshot -> {
                    // Optional: Show upload progress
                    double progress = (100.0 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                    Log.d(TAG, "Upload is " + progress + "% done");
                    // Update a progress bar if you have one
                });
    }


    // Modified saveProfileToDatabase to accept image URL
    private void saveProfileToDatabase(String username, String status, @Nullable String imageUrl) {
        if (currentUser == null) {
            Log.e(TAG, "Cannot save profile, user not signed in.");
            Toast.makeText(getContext(), "Error: User not signed in.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("status", status);
        // Only update userImage field if a new image URL is provided
        if (imageUrl != null) {
            updates.put("userImage", imageUrl); // Save the Firebase Storage download URL
            // You might also want to save the storage path or public ID if needed for deletion later
            // updates.put("userImagePath", fileReference.getPath()); // Example
        }


        userRef.update(updates).addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Profile updated successfully");
            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            // Optionally navigate back after successful save
            // requireActivity().onBackPressed();
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to update profile", e);
            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }

}
