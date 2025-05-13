package com.app.happytails.utils.Fragments;

import android.content.Context;
import android.net.Uri; // Keep Uri for image handling
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager; // Import FragmentManager
import androidx.fragment.app.FragmentTransaction; // Import FragmentTransaction

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.appbar.MaterialToolbar;

import com.app.happytails.R;
import com.app.happytails.utils.model.HomeModel; // Keep HomeModel import
import com.google.firebase.auth.FirebaseAuth; // Import FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore; // Import FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage; // Import FirebaseStorage
import com.google.firebase.storage.StorageReference; // Import StorageReference

import java.io.Serializable; // Keep Serializable import if HomeModel is Serializable
import java.util.ArrayList;
import java.util.HashMap; // Import HashMap
import java.util.Map; // Import Map

public class VetFragment extends Fragment {

    // Define an interface to communicate completion or skipping back to the parent
    // The parent activity or fragment hosting this fragment must implement this interface
    public interface OnDogCreationCompleteListener {
        /**
         * Called when the dog creation process is complete (saved or skipped vet info).
         * The parent can use this to manage the flow (e.g., pop this fragment).
         */
        void onDogCreationComplete();
        // You might also pass the HomeModel here if the parent needs the final data
        // void onDogCreationComplete(HomeModel finalDogData);
    }

    private OnDogCreationCompleteListener listener;
    // Removed dogData variable as initial data is stored in separate fields

    private TextInputLayout inputVetName, inputClinicName, inputClinicAddress, inputVetPhone, inputVetEmail, inputMedicalHistory;
    private TextInputEditText editVetName, editClinicName, editClinicAddress, editVetPhone, editVetEmail, editMedicalHistory;
    private Button btnSave, btnSkip;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;

    // Fields to hold initial dog data received from the previous fragment
    private String dogName;
    private String description; // Assuming description is passed and stored here
    private String patreonUrl;
    private int urgencyLevel;
    private ArrayList<Uri> galleryImages; // Assuming Uris are passed
    private Uri mainImageUrl; // Assuming Uri is passed

    // Firebase instances
    private FirebaseStorage storage; // Added Firebase Storage
    private FirebaseFirestore firestore; // Added Firebase Firestore
    private FirebaseAuth auth; // Added FirebaseAuth

    private static final String TAG = "VetFragment"; // Renamed TAG for clarity

    public VetFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Check if the parent context (Activity or Fragment) implements the listener interface
        if (context instanceof OnDogCreationCompleteListener) {
            listener = (OnDogCreationCompleteListener) context;
        } else {
            // If the parent doesn't implement the interface, throw a RuntimeException
            throw new RuntimeException(context.toString()
                    + " must implement OnDogCreationCompleteListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_vet, container, false);

        // Retrieve the initial dog data passed from the previous fragment
        Bundle bundle = this.getArguments();
        if(bundle != null){
            dogName = bundle.getString("dogName");
            patreonUrl = bundle.getString("patreonUrl");
            description = bundle.getString("description"); // Retrieve description
            urgencyLevel = bundle.getInt("urgencyLevel");
            // Ensure these are passed as Parcelable/Serializable Uris
            mainImageUrl = bundle.getParcelable("mainImageUrl"); // Assuming Parcelable Uri
            galleryImages = bundle.getParcelableArrayList("galleryImages"); // Assuming Parcelable Uris in ArrayList
            Log.d(TAG, "Received initial dog data. Name: " + dogName + ", Urgency: " + urgencyLevel);
            if (mainImageUrl != null) {
                Log.d(TAG, "Received main image Uri: " + mainImageUrl.toString());
            }
            if (galleryImages != null) {
                Log.d(TAG, "Received " + galleryImages.size() + " gallery image Uris.");
            }

        } else {
            Log.e(TAG, "No initial dog data found in arguments!");
            Toast.makeText(getContext(), "Error loading initial dog data.", Toast.LENGTH_SHORT).show();
            // Optionally pop this fragment off the stack if data is missing
            getParentFragmentManager().popBackStack();
        }


        // Initialize views
        toolbar = view.findViewById(R.id.vet_page_toolbar);
        inputVetName = view.findViewById(R.id.input_vet_name);
        editVetName = view.findViewById(R.id.edit_vet_name);
        inputClinicName = view.findViewById(R.id.input_clinic_name);
        editClinicName = view.findViewById(R.id.edit_clinic_name);
        inputClinicAddress = view.findViewById(R.id.input_clinic_address);
        editClinicAddress = view.findViewById(R.id.edit_clinic_address);
        inputVetPhone = view.findViewById(R.id.input_vet_phone);
        editVetPhone = view.findViewById(R.id.edit_vet_phone);
        inputVetEmail = view.findViewById(R.id.input_vet_email);
        editVetEmail = view.findViewById(R.id.edit_vet_email);
        inputMedicalHistory = view.findViewById(R.id.input_medical_history);
        editMedicalHistory = view.findViewById(R.id.edit_medical_history);
        btnSave = view.findViewById(R.id.btn_save_vet_info);
        btnSkip = view.findViewById(R.id.btn_skip_vet_info);
        progressBar = view.findViewById(R.id.progress_vet_page);

        // Initialize Firebase instances
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Setup toolbar navigation (handles back button)
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                // Handle back button click - typically pop the fragment from the back stack
                getParentFragmentManager().popBackStack();
            });
            // Optional: Set toolbar title dynamically if needed
            if (dogName != null) {
                toolbar.setTitle("Vet Info for " + dogName);
            }
        }

        // Set click listeners for buttons
        btnSave.setOnClickListener(v -> handleSaveButtonClick());
        btnSkip.setOnClickListener(v -> handleSkipButtonClick());

        // Optional: If editing existing data, populate fields here
        // This would involve checking if the received HomeModel already contains vet info
        // and populating the EditText fields accordingly.

        return view;
    }

    private void handleSaveButtonClick() {
        // Collect data from input fields
        String vetName = editVetName.getText().toString().trim();
        String clinicName = editClinicName.getText().toString().trim();
        String clinicAddress = editClinicAddress.getText().toString().trim();
        String vetPhone = editVetPhone.getText().toString().trim();
        String vetEmail = editVetEmail.getText().toString().trim();
        String medicalHistory = editMedicalHistory.getText().toString().trim();

        // Perform validation for required fields
        if (vetName.isEmpty()) {
            inputVetName.setError("Vet Name is required");
            return;
        } else {
            inputVetName.setError(null); // Clear error if valid
        }

        if (vetPhone.isEmpty()) {
            inputVetPhone.setError("Vet Phone is required");
            return;
        } else {
            inputVetPhone.setError(null); // Clear error if valid
        }

        // Optional: Add more robust validation for phone and email formats if needed

        // --- Combine initial dog data with collected vet data into HomeModel ---
        // Create a new HomeModel instance and populate it with all data
        HomeModel finalDogData = new HomeModel();
        // Set initial data from fragment fields
        finalDogData.setDogName(this.dogName);
        finalDogData.setDescription(this.description); // Set description
        finalDogData.setPatreonUrl(this.patreonUrl);
        finalDogData.setUrgencylevel(this.urgencyLevel);

        // Set vet info from collected fields
        finalDogData.setVetName(vetName);
        finalDogData.setClinicName(clinicName);
        finalDogData.setClinicAddress(clinicAddress);
        finalDogData.setVetPhone(vetPhone);
        finalDogData.setVetEmail(vetEmail);
        finalDogData.setMedicalHistory(medicalHistory);

        // The HomeModel constructor or setters should handle setting dog_lower and dog_keywords
        // based on the dogName when it's set.

        // --- Call the saveDogInfo method to handle Firebase operations ---
        if (mainImageUrl != null) {
            // Pass the HomeModel object directly to saveDogInfo
            saveDogInfo(this.mainImageUrl, this.galleryImages, finalDogData);
        } else {
            Toast.makeText(getContext(), "Main image is missing, cannot save.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Main image Uri is null when trying to save.");
        }

        // Removed the listener call here as saving and navigation are handled internally
    }

    private void handleSkipButtonClick() {
        Log.d(TAG, "Skipping vet info. Proceeding with save.");

        // --- Create a HomeModel with only the initial data ---
        HomeModel finalDogData = new HomeModel();
        finalDogData.setDogName(this.dogName);
        finalDogData.setDescription(this.description); // Set description
        finalDogData.setPatreonUrl(this.patreonUrl);
        finalDogData.setUrgencylevel(this.urgencyLevel);
        // Vet info fields will be null/empty in this case

        // --- Call the saveDogInfo method to handle Firebase operations ---
        // Pass the HomeModel with only initial data
        if (mainImageUrl != null) {
            saveDogInfo(this.mainImageUrl, this.galleryImages, finalDogData);
        } else {
            Toast.makeText(getContext(), "Main image is missing, cannot save.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Main image Uri is null when trying to save on skip.");
            // Notify parent even if save fails due to missing image
            if (listener != null) {
                listener.onDogCreationComplete();
            }
            // Pop fragment even if save fails
            getParentFragmentManager().popBackStack();
        }

        // Removed the listener call here as saving and navigation are handled internally
    }

    // --- Integrated saveDogInfo method, now taking HomeModel ---
    private void saveDogInfo(Uri mainImageUri, ArrayList<Uri> galleryUris, HomeModel dogDataToSave) {
        progressBar.setVisibility(View.VISIBLE); // Use the fragment's ProgressBar

        // Upload main image to Firebase Storage
        StorageReference mainImageRef = storage.getReference().child("dogs").child(auth.getCurrentUser().getUid()).child(System.currentTimeMillis() + "_main.jpg"); // Include user ID in path
        mainImageRef.putFile(mainImageUri)
                .addOnSuccessListener(taskSnapshot ->
                        mainImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String mainImageUrl = uri.toString();
                            dogDataToSave.setMainImage(mainImageUrl); // Set the main image URL in the HomeModel

                            // --- Handle Gallery Image Uploads ---
                            ArrayList<String> galleryUrls = new ArrayList<>();
                            if (galleryUris != null && !galleryUris.isEmpty()) {
                                // Implement logic to upload gallery images
                                // This is a simplified example; a real implementation would handle
                                // multiple uploads and wait for all to complete before saving Firestore data.
                                // For now, we'll just upload them sequentially (not recommended for many images)
                                uploadGalleryImages(0, galleryUris, galleryUrls, dogDataToSave);
                            } else {
                                // No gallery images, proceed to save Firestore data
                                dogDataToSave.setGalleryImages(galleryUrls); // Set empty gallery URLs in HomeModel
                                saveFirestoreDogData(dogDataToSave);
                            }

                        }).addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Error getting main image download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error getting main image download URL", e);
                            // Notify parent even if save fails
                            if (listener != null) {
                                listener.onDogCreationComplete();
                            }
                            // Pop fragment even if save fails
                            getParentFragmentManager().popBackStack();
                        }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error uploading main image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error uploading main image", e);
                    // Notify parent even if save fails
                    if (listener != null) {
                        listener.onDogCreationComplete();
                    }
                    // Pop fragment even if save fails
                    getParentFragmentManager().popBackStack();
                });
    }

    // Recursive helper method to upload gallery images sequentially
    private void uploadGalleryImages(int index, ArrayList<Uri> galleryUris, ArrayList<String> galleryUrls, HomeModel dogDataToSave) {
        if (index >= galleryUris.size()) {
            // All images uploaded, proceed to save Firestore data
            dogDataToSave.setGalleryImages(galleryUrls); // Set gallery URLs in HomeModel
            saveFirestoreDogData(dogDataToSave);
            return;
        }

        Uri currentUri = galleryUris.get(index);
        StorageReference galleryImageRef = storage.getReference().child("dogs").child(auth.getCurrentUser().getUid()).child(System.currentTimeMillis() + "_gallery" + index + ".jpg");

        galleryImageRef.putFile(currentUri)
                .addOnSuccessListener(taskSnapshot ->
                        galleryImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            galleryUrls.add(uri.toString());
                            // Upload the next image
                            uploadGalleryImages(index + 1, galleryUris, galleryUrls, dogDataToSave);
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting gallery download URL for index " + index + ": " + e.getMessage());
                            // Continue with the next image even if this one fails
                            uploadGalleryImages(index + 1, galleryUris, galleryUrls, dogDataToSave);
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading gallery image for index " + index + ": " + e.getMessage());
                    // Continue with the next image even if this one fails
                    uploadGalleryImages(index + 1, galleryUris, galleryUrls, dogDataToSave);
                });
    }


    // Helper method to save data to Firestore after images are uploaded
    private void saveFirestoreDogData(HomeModel dogDataToSave) {
        // Prepare a map of dog info to store in Firestore using HomeModel getters.
        Map<String, Object> dogMap = new HashMap<>();
        dogMap.put("creator", auth.getCurrentUser().getUid()); // Set the creator ID
        dogMap.put("dogId", dogDataToSave.getDogId()); // Include dogId if already set (e.g., for updates)
        dogMap.put("dogName", dogDataToSave.getDogName());
        dogMap.put("description", dogDataToSave.getDescription()); // Get description from HomeModel
        dogMap.put("urgencyLevel", dogDataToSave.getUrgencylevel());
        dogMap.put("patreonUrl", dogDataToSave.getPatreonUrl());
        dogMap.put("mainImage", dogDataToSave.getMainImage()); // Get main image URL from HomeModel
        dogMap.put("galleryImages", dogDataToSave.getGalleryImages()); // Get gallery image URLs from HomeModel
        dogMap.put("dog_lower", dogDataToSave.dog_lower); // Access directly or use getter if public
        dogMap.put("dog_keywords", dogDataToSave.dog_keywords); // Access directly or use getter if public

        // Add vet info from HomeModel getters (these might be null if skipping vet info)
        dogMap.put("vetName", dogDataToSave.getVetName());
        dogMap.put("clinicName", dogDataToSave.getClinicName());
        dogMap.put("clinicAddress", dogDataToSave.getClinicAddress());
        dogMap.put("vetPhone", dogDataToSave.getVetPhone());
        dogMap.put("vetEmail", dogDataToSave.getVetEmail());
        dogMap.put("medicalHistory", dogDataToSave.getMedicalHistory());

        // Add other fields from HomeModel if needed (e.g., fundingAmount, supporters)
        // dogMap.put("fundingPercentage", dogDataToSave.getFundingPercentage());
        // dogMap.put("fundingAmount", dogDataToSave.getFundingAmount());
        // dogMap.put("donationsAmount", dogDataToSave.getDonationsAmount());
        // dogMap.put("supporters", dogDataToSave.getSupporters());
        // dogMap.put("accessToken", dogDataToSave.getAccessToken());


        firestore.collection("dogs")
                .add(dogMap) // Add a new document to the "dogs" collection
                .addOnSuccessListener(documentReference -> {
                    // Get the newly created dog's ID and set it in the HomeModel
                    String dogId = documentReference.getId();
                    dogDataToSave.setDogId(dogId); // Set dogId in the HomeModel

                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Dog saved successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Dog saved with ID: " + dogId);

                    // --- Navigate to HomeFragment upon successful save ---
                    FragmentManager fragmentManager = getParentFragmentManager();
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    HomeFragment homeFragment = new HomeFragment(); // Assuming HomeFragment exists

                    // Replace the current fragment (VetFragment) with HomeFragment
                    // IMPORTANT: Replace R.id.fragment_container with the actual ID of the
                    // container view in your Activity's layout where these fragments are displayed.
                    ft.replace(R.id.fragment_container, homeFragment); // <-- REPLACE R.id.fragment_container

                    // Optional: Add animation for the transition
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

                    // Optional: Clear the back stack up to the first fragment of this flow
                    // This prevents the user from going back to the creation fragments
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    // Or just add to back stack if you want to allow going back
                    // ft.addToBackStack(null);

                    ft.commit();
                    // --- End Navigation ---

                    // Notify the parent that the creation process is complete
                    if (listener != null) {
                        listener.onDogCreationComplete(); // Or pass the finalDogData
                    }

                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error saving dog to Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving dog to Firestore", e);
                    // Notify parent even if save fails
                    if (listener != null) {
                        listener.onDogCreationComplete();
                    }
                    // Pop fragment even if save fails
                    getParentFragmentManager().popBackStack();
                });
    }


    @Override
    public void onDetach() {
        super.onDetach();
        // Release the listener reference when the fragment is detached
        listener = null;
    }

    // You will need to add the following fields and their getters/setters to your HomeModel class:
    /*
    // Add these fields to your HomeModel class:
    private String vetName;
    private String clinicName;
    private String clinicAddress;
    private String vetPhone;
    private String vetEmail;
    private String medicalHistory;
    private String description; // Add description field if it's not already there

    // Add getters and setters for the new fields:
    public String getVetName() { return vetName; }
    public void setVetName(String vetName) { this.vetName = vetName; }
    // ... add getters and setters for other new fields

    public String getDescription() { return description; } // Add getter for description
    public void setDescription(String description) { this.description = description; } // Add setter for description
    public String getDogId() { return dogId; } // Add getter for dogId if not already there
    public void setDogId(String dogId) { this.dogId = dogId; } // Add setter for dogId if not already there
    public String getMainImage() { return mainImage; } // Add getter for mainImage if not already there
    public void setMainImage(String mainImage) { this.mainImage = mainImage; } // Add setter for mainImage if not already there
    public ArrayList<String> getGalleryImages() { return galleryImages; } // Add getter for galleryImages if not already there
    public void setGalleryImages(ArrayList<String> galleryImages) { this.galleryImages = galleryImages; } // Add setter for galleryImages if not already there

    // Ensure dog_lower and dog_keywords are handled when dogName is set, e.g., in the constructor or setDogName method:
    // public void setDogName(String dogName) {
    //     this.dogName = dogName;
    //     this.dog_lower = dogName.toLowerCase();
    //     this.dog_keywords = generateThreeCharKeywords(dogName);
    // }
    */
}
