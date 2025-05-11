package com.app.happytails.utils.Fragments;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.GalleryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CreateFragment2 extends Fragment {

    private EditText dogName, descriptionED, patreonUrl;
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
        patreonUrl = view.findViewById(R.id.patreon_url);

        // Setup Firebase instances
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        galleryAdapter = new GalleryAdapter(getContext(), galleryUris);
        recyclerView.setAdapter(galleryAdapter);

        // Handle the button click – now saving dog info
        nextButton.setOnClickListener(v -> handleNextButtonClick());

        // Handle image selections
        dogPic.setOnClickListener(v -> openMainImageGallery());
        dogGalleryPic.setOnClickListener(v -> openGalleryForDogImages());

        // Handle urgency level selection
        urgencyLevelButton.setOnClickListener(v -> openUrgencyLevelDialog());
    }

    private void handleNextButtonClick() {
        String name = dogName.getText().toString().trim();
        String description = descriptionED.getText().toString().trim();
        String patreon_url = patreonUrl.getText().toString().trim();

        if (name.isEmpty() || description.isEmpty() || patreon_url.isEmpty()) {
            Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate the Patreon URL using a robust check.
        if (!isValidPatreonUrl(patreon_url)) {
            // Set an error message directly on the EditText.
            patreonUrl.setError("Incorrect URL: please enter a valid Patreon creator URL");
            patreonUrl.requestFocus();
            return;
        }

        if (mainImageUri != null) {
            // Save all dog info and images to Firestore
            saveDogInfo(name, description, urgencyLevel, patreon_url, mainImageUri, galleryUris);
        } else {
            Toast.makeText(getContext(), "Please select a main image", Toast.LENGTH_SHORT).show();
        }
    }

    // Improved URL validation logic.
    private boolean isValidPatreonUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            // Ensure the URL uses HTTP or HTTPS.
            String protocol = url.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return false;
            }

            // Normalize the host to lower-case.
            String host = url.getHost().toLowerCase();

            // Validate that the host is exactly "patreon.com" or a subdomain of "patreon.com".
            return host.equals("patreon.com") || host.endsWith(".patreon.com");
        } catch (MalformedURLException e) {
            // URL is not properly formed.
            e.printStackTrace();
            return false;
        }
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
