package com.app.happytails.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.GalleryEditAdapter;
import com.app.happytails.utils.model.GalleryImage; // Import GalleryImage model
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChangeDogActivity extends AppCompatActivity {

    private static final String TAG = "ChangeDogActivity";

    // UI Elements from the layout (fragment_edit_dog_page_improved.xml)
    private MaterialToolbar toolbar;
    private CircleImageView dogImageEdit;
    private TextInputLayout inputLayoutDogName, inputLayoutPatreonUrl, inputLayoutDogDescription;
    private TextInputEditText editTextDogName, editTextPatreonUrl, editTextDogDescription;
    private MaterialButton btnSelectUrgencyLevel;
    private TextView urgencyLevelValue;
    private TextInputLayout inputLayoutVetName, inputLayoutClinicName, inputLayoutClinicAddress,
            inputLayoutVetPhone, inputLayoutVetEmail, inputLayoutMedicalHistory,
            inputLayoutLastVisitDate, inputLayoutDiagnosis;
    private TextInputEditText editTextVetName, editTextClinicName, editTextClinicAddress,
            editTextVetPhone, editTextVetEmail, editTextMedicalHistory,
            editTextLastVisitDate, editTextDiagnosis;
    private MaterialCardView cardAddGalleryImage;
    private ImageView imageAddGallery;
    private RecyclerView recyclerViewDogGallery;
    private MaterialButton btnSaveDogChanges;
    private ProgressBar progressBarEditDog;

    // Firebase Instances
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference;

    // Data
    private String dogId;
    private Uri mainImageUri; // Uri for the newly selected main image
    private ArrayList<Uri> galleryImageUris = new ArrayList<>(); // Uris for newly added gallery images
    // Removed galleryImagesToDelete list as deletion is handled by the adapter

    private String existingMainImageUrl; // Existing main image URL from Firestore

    // Adapter for the gallery RecyclerView
    private GalleryEditAdapter galleryEditAdapter;

    private static final int PICK_MAIN_IMAGE_REQUEST = 1;
    private static final int PICK_GALLERY_IMAGE_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming activity_change_dog.xml contains the layout for editing
        setContentView(R.layout.activity_change_dog); // Use your improved layout file name


        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference("dog_images"); // Storage path for dog images

        // Get the dogId from the Intent
        dogId = getIntent().getStringExtra("dogId");

        if (dogId == null || dogId.isEmpty()) {
            Log.e(TAG, "Dog ID not provided in Intent.");
            Toast.makeText(this, "Error: Cannot load dog profile for editing.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if dogId is missing
            return;
        }

        // Initialize UI elements
        initViews();

        // Setup toolbar back navigation
        setupToolbar();

        // Load existing dog data from Firestore
        loadDogData(dogId);

        // Setup listeners
        setupListeners();

        // Setup Gallery RecyclerView
        setupGalleryRecyclerView();
    }

    private void initViews() {
        toolbar = findViewById(R.id.edit_dog_toolbar); // Assuming this ID in your layout
        dogImageEdit = findViewById(R.id.dog_image_edit);
        inputLayoutDogName = findViewById(R.id.input_layout_dog_name);
        editTextDogName = findViewById(R.id.edit_text_dog_name);
        inputLayoutPatreonUrl = findViewById(R.id.input_layout_patreon_url);
        editTextPatreonUrl = findViewById(R.id.edit_text_patreon_url);
        inputLayoutDogDescription = findViewById(R.id.input_layout_dog_description);
        editTextDogDescription = findViewById(R.id.edit_text_dog_description);
        btnSelectUrgencyLevel = findViewById(R.id.btn_select_urgency_level);
        urgencyLevelValue = findViewById(R.id.urgency_level_value);
        inputLayoutVetName = findViewById(R.id.input_layout_vet_name);
        editTextVetName = findViewById(R.id.edit_text_vet_name);
        inputLayoutClinicName = findViewById(R.id.input_layout_clinic_name);
        editTextClinicName = findViewById(R.id.edit_text_clinic_name);
        inputLayoutClinicAddress = findViewById(R.id.input_layout_clinic_address);
        editTextClinicAddress = findViewById(R.id.edit_text_clinic_address);
        inputLayoutVetPhone = findViewById(R.id.input_layout_vet_phone);
        editTextVetPhone = findViewById(R.id.edit_text_vet_phone);
        inputLayoutVetEmail = findViewById(R.id.input_layout_vet_email);
        editTextVetEmail = findViewById(R.id.edit_text_vet_email);
        inputLayoutMedicalHistory = findViewById(R.id.input_layout_medical_history);
        editTextMedicalHistory = findViewById(R.id.edit_text_medical_history);
        inputLayoutLastVisitDate = findViewById(R.id.input_layout_last_visit_date);
        editTextLastVisitDate = findViewById(R.id.edit_text_last_visit_date);
        inputLayoutDiagnosis = findViewById(R.id.input_layout_diagnosis);
        editTextDiagnosis = findViewById(R.id.edit_text_diagnosis);
        cardAddGalleryImage = findViewById(R.id.card_add_image); // Card for adding gallery images
        imageAddGallery = findViewById(R.id.image_add_gallery); // ImageView inside the card
        recyclerViewDogGallery = findViewById(R.id.recycler_view_dog_gallery);
        btnSaveDogChanges = findViewById(R.id.btn_save_dog_changes);
        progressBarEditDog = findViewById(R.id.progress_bar_edit_dog);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back arrow
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            // Toolbar title is set in the layout XML
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Handle back arrow click
        return true;
    }

    private void setupListeners() {
        dogImageEdit.setOnClickListener(v -> openImagePicker(PICK_MAIN_IMAGE_REQUEST));
        cardAddGalleryImage.setOnClickListener(v -> openImagePicker(PICK_GALLERY_IMAGE_REQUEST)); // Listener for adding gallery images
        btnSelectUrgencyLevel.setOnClickListener(v -> showUrgencyLevelPicker()); // You'll need to implement this method
        btnSaveDogChanges.setOnClickListener(v -> saveDogChanges());
    }

    private void setupGalleryRecyclerView() {
        // Initialize the adapter with an empty list - no listener needed
        galleryEditAdapter = new GalleryEditAdapter(new ArrayList<>());
        recyclerViewDogGallery.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewDogGallery.setAdapter(galleryEditAdapter);
    }


    private void loadDogData(String dogId) {
        db.collection("dogs").document(dogId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Load main dog info
                    editTextDogName.setText(documentSnapshot.getString("dogName"));
                    editTextPatreonUrl.setText(documentSnapshot.getString("patreonUrl"));
                    editTextDogDescription.setText(documentSnapshot.getString("description"));

                    Long urgencyLevel = documentSnapshot.getLong("urgencyLevel");
                    if (urgencyLevel != null) {
                        // Set the urgency level text (you might need a helper method to map int to text)
                        urgencyLevelValue.setText("Level: " + urgencyLevel); // Example
                    }

                    // Load main image
                    existingMainImageUrl = documentSnapshot.getString("mainImage");
                    if (existingMainImageUrl != null && !existingMainImageUrl.isEmpty()) {
                        Glide.with(this).load(existingMainImageUrl).into(dogImageEdit);
                    } else {
                        dogImageEdit.setImageResource(R.drawable.user_icon); // Default image
                    }

                    // Load vet info
                    editTextVetName.setText(documentSnapshot.getString("vetName"));
                    editTextClinicName.setText(documentSnapshot.getString("clinicName"));
                    editTextClinicAddress.setText(documentSnapshot.getString("clinicAddress"));
                    editTextVetPhone.setText(documentSnapshot.getString("vetPhone"));
                    editTextVetEmail.setText(documentSnapshot.getString("vetEmail"));
                    editTextMedicalHistory.setText(documentSnapshot.getString("medicalHistory"));
                    editTextLastVisitDate.setText(documentSnapshot.getString("vetLastVisitDate")); // Load Last Visit Date
                    editTextDiagnosis.setText(documentSnapshot.getString("vetDiagnosis")); // Load Diagnosis

                    // Load gallery images and convert to GalleryImage objects with String URLs
                    List<String> urls = (ArrayList<String>) documentSnapshot.get("galleryImages");
                    List<GalleryImage> galleryImages = new ArrayList<>();
                    if (urls != null) {
                        for (String url : urls) {
                            // Ensure the URL is a String and create GalleryImage with String URL
                            if (url != null && !url.isEmpty()) {
                                galleryImages.add(new GalleryImage(url)); // Assuming GalleryImage constructor takes String URL
                            }
                        }
                    }
                    // Update the gallery adapter with existing images
                    galleryEditAdapter.setImages(galleryImages);

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading dog data", e);
                    Toast.makeText(this, "Failed to load dog data.", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity on error
                });
    }

    private void openImagePicker(int requestCode) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData();
            if (requestCode == PICK_MAIN_IMAGE_REQUEST) {
                mainImageUri = selectedImageUri;
                dogImageEdit.setImageURI(mainImageUri); // Display selected main image
            } else if (requestCode == PICK_GALLERY_IMAGE_REQUEST) {
                // Add the selected gallery image URI to a temporary list
                galleryImageUris.add(selectedImageUri);
                // Optional: Add a visual representation of the new image to the adapter immediately
                // This requires modifying the adapter and potentially the GalleryImage model
                // to handle Uris before they are uploaded.
                // For now, we'll rely on the upload and save process to update the adapter.
                Toast.makeText(this, "Gallery image selected. Will be uploaded on save.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // You'll need to implement showUrgencyLevelPicker()
    private void showUrgencyLevelPicker() {
        // Example: Show a DialogFragment or AlertDialog to select urgency level
        Toast.makeText(this, "Urgency Level picker not implemented yet.", Toast.LENGTH_SHORT).show();
        // When a level is selected, update urgencyLevelValue.setText(...)
    }

    private void saveDogChanges() {
        String newDogName = editTextDogName.getText().toString().trim();
        String newPatreonUrl = editTextPatreonUrl.getText().toString().trim();
        String newDescription = editTextDogDescription.getText().toString().trim();
        // Get selected urgency level (you'll need to store this from the picker)
        // int selectedUrgencyLevel = ...; // Get from where you store the selected level

        // Get vet info from input fields
        String newVetName = editTextVetName.getText().toString().trim();
        String newClinicName = editTextClinicName.getText().toString().trim();
        String newClinicAddress = editTextClinicAddress.getText().toString().trim();
        String newVetPhone = editTextVetPhone.getText().toString().trim();
        String newVetEmail = editTextVetEmail.getText().toString().trim();
        String newMedicalHistory = editTextMedicalHistory.getText().toString().trim();
        String newLastVisitDate = editTextLastVisitDate.getText().toString().trim();
        String newDiagnosis = editTextDiagnosis.getText().toString().trim();


        if (newDogName.isEmpty()) {
            inputLayoutDogName.setError("Dog Name is required");
            return;
        }
        // Add other validation as needed

        progressBarEditDog.setVisibility(View.VISIBLE);
        btnSaveDogChanges.setEnabled(false); // Disable button while saving

        // --- Handle Main Image Upload/Deletion ---
        if (mainImageUri != null && existingMainImageUrl != null) {
            // New main image selected, delete the old one first
            deleteImageFromStorage(existingMainImageUrl, () -> {
                // After deleting old main image, upload the new main image
                uploadMainImage(newDogName, newPatreonUrl, newDescription, /*selectedUrgencyLevel,*/
                        newVetName, newClinicName, newClinicAddress, newVetPhone,
                        newVetEmail, newMedicalHistory, newLastVisitDate, newDiagnosis);
            });
        } else if (mainImageUri != null) {
            // No existing main image, just upload the new one
            uploadMainImage(newDogName, newPatreonUrl, newDescription, /*selectedUrgencyLevel,*/
                    newVetName, newClinicName, newClinicAddress, newVetPhone,
                    newVetEmail, newMedicalHistory, newLastVisitDate, newDiagnosis);
        } else {
            // No new main image, proceed to upload gallery images
            uploadGalleryImages(newDogName, newPatreonUrl, newDescription, /*selectedUrgencyLevel,*/
                    newVetName, newClinicName, newClinicAddress, newVetPhone,
                    newVetEmail, newMedicalHistory, newLastVisitDate, newDiagnosis,
                    existingMainImageUrl); // Pass existing main image URL
        }
        // Note: Gallery image deletion is handled directly by the adapter when the delete button is clicked.
        // The adapter's internal list will be the source of truth for gallery images to save.
    }

    // Helper to delete an image from Firebase Storage by URL
    private void deleteImageFromStorage(String imageUrl, Runnable onSuccess) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            onSuccess.run(); // Nothing to delete
            return;
        }

        try {
            // Get StorageReference from the image URL
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

            // Delete the file
            imageRef.delete().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Image deleted successfully: " + imageUrl);
                onSuccess.run();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to delete image: " + imageUrl, e);
                // Continue even if deletion fails for one image
                onSuccess.run();
            });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid URL for deletion: " + imageUrl, e);
            onSuccess.run(); // Invalid URL, proceed
        }
    }


    private void uploadMainImage(String dogName, String patreonUrl, String description, /*int urgencyLevel,*/
                                 String vetName, String clinicName, String clinicAddress, String vetPhone,
                                 String vetEmail, String medicalHistory, String lastVisitDate, String diagnosis) {

        StorageReference fileReference = storageReference.child(dogId + "_main_" + System.currentTimeMillis() + ".jpg");

        fileReference.putFile(mainImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d(TAG, "Main image uploaded. URL: " + downloadUrl);
                        // After main image upload, upload gallery images
                        uploadGalleryImages(dogName, patreonUrl, description, /*urgencyLevel,*/
                                vetName, clinicName, clinicAddress, vetPhone,
                                vetEmail, medicalHistory, lastVisitDate, diagnosis,
                                downloadUrl); // Pass the new main image URL
                    }).addOnFailureListener(e -> handleSaveError("Failed to get main image URL", e));
                })
                .addOnFailureListener(e -> handleSaveError("Failed to upload main image", e));
    }

    private void uploadGalleryImages(String dogName, String patreonUrl, String description, /*int urgencyLevel,*/
                                     String vetName, String clinicName, String clinicAddress, String vetPhone,
                                     String vetEmail, String medicalHistory, String lastVisitDate, String diagnosis,
                                     String mainImageUrl) { // Receive the main image URL

        if (galleryImageUris.isEmpty()) {
            // No new gallery images to upload, proceed to save Firestore data
            // Get the current list of images from the adapter (includes existing minus deleted)
            List<GalleryImage> currentGalleryImages = galleryEditAdapter.getCurrentImages();
            List<String> finalGalleryUrls = new ArrayList<>();
            for (GalleryImage img : currentGalleryImages) {
                // Ensure the URL is a String before adding
                if (img.getPicUrl() != null) {
                    finalGalleryUrls.add(String.valueOf(img.getPicUrl()));
                } else {
                    Log.w(TAG, "Skipping invalid gallery image URL from adapter: " + img.getPicUrl());
                }
            }

            saveDogDataToFirestore(dogName, patreonUrl, description, /*selectedUrgencyLevel,*/
                    vetName, clinicName, clinicAddress, vetPhone,
                    vetEmail, medicalHistory, lastVisitDate, diagnosis,
                    mainImageUrl, finalGalleryUrls); // Pass the final list of URLs
            return;
        }

        List<String> newGalleryUrls = new ArrayList<>();
        // Simplified sequential upload for new gallery images
        uploadNextGalleryImage(0, galleryImageUris, newGalleryUrls, dogName, patreonUrl, description, /*urgencyLevel,*/
                vetName, clinicName, clinicAddress, vetPhone, vetEmail, medicalHistory,
                lastVisitDate, diagnosis, mainImageUrl);
    }

    private void uploadNextGalleryImage(int index, List<Uri> urisToUpload, List<String> uploadedUrls,
                                        String dogName, String patreonUrl, String description, /*int urgencyLevel,*/
                                        String vetName, String clinicName, String clinicAddress, String vetPhone,
                                        String vetEmail, String medicalHistory, String lastVisitDate, String diagnosis,
                                        String mainImageUrl) {

        if (index >= urisToUpload.size()) {
            // All new gallery images uploaded, combine with existing and save Firestore data
            // Get the current list of images from the adapter (includes existing minus deleted)
            List<GalleryImage> currentGalleryImages = galleryEditAdapter.getCurrentImages();
            List<String> finalGalleryUrls = new ArrayList<>();
            for (GalleryImage img : currentGalleryImages) {
                // Ensure the URL is a String before adding
                if (img.getPicUrl() != null) {
                    finalGalleryUrls.add(img.getPicUrl());
                } else {
                    Log.w(TAG, "Skipping invalid gallery image URL from adapter: " + img.getPicUrl());
                }
            }
            finalGalleryUrls.addAll(uploadedUrls); // Add the newly uploaded URLs

            saveDogDataToFirestore(dogName, patreonUrl, description, /*urgencyLevel,*/
                    vetName, clinicName, clinicAddress, vetPhone,
                    vetEmail, medicalHistory, lastVisitDate, diagnosis,
                    mainImageUrl, finalGalleryUrls); // Pass the final list of URLs
            return;
        }

        Uri currentUri = urisToUpload.get(index);
        StorageReference fileReference = storageReference.child(dogId + "_gallery_" + System.currentTimeMillis() + "_" + index + ".jpg");

        fileReference.putFile(currentUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        uploadedUrls.add(uri.toString());
                        Log.d(TAG, "Gallery image uploaded. URL: " + uri.toString());
                        // Upload the next image
                        uploadNextGalleryImage(index + 1, urisToUpload, uploadedUrls, dogName, patreonUrl, description, /*urgencyLevel,*/
                                vetName, clinicName, clinicAddress, vetPhone, vetEmail, medicalHistory,
                                lastVisitDate, diagnosis, mainImageUrl);
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to get gallery image URL for index " + index, e);
                        // Continue with the next image even if this one fails
                        uploadNextGalleryImage(index + 1, urisToUpload, uploadedUrls, dogName, patreonUrl, description, /*urgencyLevel,*/
                                vetName, clinicName, clinicAddress, vetPhone, vetEmail, medicalHistory,
                                lastVisitDate, diagnosis, mainImageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gallery image upload failed for index " + index, e);
                    // Continue with the next image even if this one fails
                    uploadNextGalleryImage(index + 1, urisToUpload, uploadedUrls, dogName, patreonUrl, description, /*urgencyLevel,*/
                            vetName, clinicName, clinicAddress, vetPhone, vetEmail, medicalHistory,
                            lastVisitDate, diagnosis, mainImageUrl);
                });
    }


    private void saveDogDataToFirestore(String dogName, String patreonUrl, String description, /*int urgencyLevel,*/
                                        String vetName, String clinicName, String clinicAddress, String vetPhone,
                                        String vetEmail, String medicalHistory, String lastVisitDate, String diagnosis,
                                        String mainImageUrl, List<String> galleryImageUrls) { // Receive final URLs

        Map<String, Object> updates = new HashMap<>();
        updates.put("dogName", dogName);
        updates.put("patreonUrl", patreonUrl);
        updates.put("description", description);
        // updates.put("urgencyLevel", urgencyLevel); // Add urgency level when implemented
        if (mainImageUrl != null) {
            updates.put("mainImage", mainImageUrl); // Update main image URL if changed
        }
        updates.put("galleryImages", galleryImageUrls); // Update gallery image URLs

        // Update vet info fields
        updates.put("vetName", vetName);
        updates.put("clinicName", clinicName);
        updates.put("clinicAddress", clinicAddress);
        updates.put("vetPhone", vetPhone);
        updates.put("vetEmail", vetEmail);
        updates.put("medicalHistory", medicalHistory);
        updates.put("vetLastVisitDate", lastVisitDate); // Save Last Visit Date
        updates.put("vetDiagnosis", diagnosis); // Save Diagnosis

        // Add other fields if they are editable (e.g., targetAmount)
        // updates.put("targetAmount", newTargetAmount);


        db.collection("dogs").document(dogId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Dog document updated successfully");
                    Toast.makeText(this, "Dog profile updated!", Toast.LENGTH_SHORT).show();
                    progressBarEditDog.setVisibility(View.GONE);
                    btnSaveDogChanges.setEnabled(true);
                    finish(); // Close the activity after saving
                })
                .addOnFailureListener(e -> handleSaveError("Failed to update dog document", e));
    }

    private void handleSaveError(String message, Exception e) {
        Log.e(TAG, message, e);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        progressBarEditDog.setVisibility(View.GONE);
        btnSaveDogChanges.setEnabled(true);
        // Consider if you need to revert any UI changes or data
    }

    // Helper to get file extension (optional, Firebase Storage can often infer)
    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }
}
