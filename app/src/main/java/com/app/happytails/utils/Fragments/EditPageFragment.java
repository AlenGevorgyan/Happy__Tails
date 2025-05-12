package com.app.happytails.utils.Fragments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

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
    private ImageButton backButton;

    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

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

        // Initialize views
        usernameEt = view.findViewById(R.id.emailEditText);
        statusEt = view.findViewById(R.id.passwordEditText);
        profileImage = view.findViewById(R.id.profileImageEdit);
        saveBtn = view.findViewById(R.id.saveProfileBtn);
        backButton = view.findViewById(R.id.editBackBtn);
        progressBar = view.findViewById(R.id.progressBar);

        loadUserData();

        profileImage.setOnClickListener(v -> openImagePicker());

        saveBtn.setOnClickListener(v -> updateProfile());

        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void loadUserData() {
        if (currentUser == null) return;

        DocumentReference userRef = db.collection("users").document(currentUser.getUid());
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                String status = documentSnapshot.getString("status");
                String profileURL = documentSnapshot.getString("userImage");

                usernameEt.setText(username);
                statusEt.setText(status);

                if (profileURL != null && !profileURL.isEmpty()) {
                    Glide.with(requireContext()).load(profileURL).into(profileImage);
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show());
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            profileImage.setImageURI(imageUri);
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = requireActivity().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void updateProfile() {
        String newUsername = usernameEt.getText().toString().trim();
        String newStatus = statusEt.getText().toString().trim();

        if (newUsername.isEmpty()) {
            usernameEt.setError("Username cannot be empty");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        if (imageUri != null) {
            new AsyncTask<Uri, Void, String[]>() {
                @Override
                protected String[] doInBackground(Uri... uris) {
                    try {
                        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                                "cloud_name", "dzwoyslx4",
                                "api_key", "936129888839456",
                                "api_secret", "K4vL432ZheS8N6uJARlvzUh1Yww"
                        ));

                        InputStream inputStream = requireActivity().getContentResolver().openInputStream(uris[0]);
                        Map uploadResult = cloudinary.uploader().upload(inputStream, ObjectUtils.asMap("folder", "profile_images", "public_id", "profile_" + currentUser.getUid()));

                        String imageUrl = (String) uploadResult.get("secure_url");
                        String publicId = (String) uploadResult.get("public_id");

                        return new String[]{publicId, imageUrl};

                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(String[] result) {
                    super.onPostExecute(result);
                    if (result != null) {
                        String publicId = result[0];
                        String imageUrl = result[1];
                        saveProfileToDatabase(newUsername, newStatus, publicId, imageUrl);
                    } else {
                        Toast.makeText(getContext(), "Failed to upload image", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }.execute(imageUri);

        } else {
            saveProfileToDatabase(newUsername, newStatus, null, null);
        }
    }

    private void saveProfileToDatabase(String username, String status, String publicId, String imageUrl) {
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("status", status);
        if (imageUrl != null) updates.put("userImage", imageUrl);
        if (publicId != null) updates.put("public_id", publicId);

        userRef.update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }
}
