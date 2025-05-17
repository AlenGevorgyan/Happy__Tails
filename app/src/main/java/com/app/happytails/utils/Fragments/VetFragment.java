package com.app.happytails.utils.Fragments;

import android.content.Context;
import android.net.Uri;
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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.appbar.MaterialToolbar;

import com.app.happytails.R;
import com.app.happytails.utils.model.HomeModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VetFragment extends Fragment {

    public interface OnDogCreationCompleteListener {
        void onDogCreationComplete();
    }

    private OnDogCreationCompleteListener listener;
    private TextInputLayout inputVetName, inputClinicName, inputClinicAddress, inputVetPhone, inputVetEmail, inputMedicalHistory;
    private TextInputEditText editVetName, editClinicName, editClinicAddress, editVetPhone, editVetEmail, editMedicalHistory;
    private Button btnSave, btnSkip;
    private ProgressBar progressBar;
    private MaterialToolbar toolbar;

    // Fields to hold initial dog data
    private String dogName;
    private String description;
    private String patreonUrl;
    private int urgencyLevel;
    private ArrayList<Uri> galleryImages;
    private Uri mainImageUrl;

    // Firebase instances
    private FirebaseStorage storage;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private static final String TAG = "VetFragment";

    public VetFragment() {}

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnDogCreationCompleteListener) {
            listener = (OnDogCreationCompleteListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnDogCreationCompleteListener");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vet, container, false);

        // Retrieve initial dog data
        Bundle bundle = getArguments();
        if (bundle != null) {
            dogName = bundle.getString("dogName");
            patreonUrl = bundle.getString("patreonUrl");
            description = bundle.getString("description");
            urgencyLevel = bundle.getInt("urgencyLevel");
            mainImageUrl = bundle.getParcelable("mainImageUrl");
            galleryImages = bundle.getParcelableArrayList("galleryImages");
            Log.d(TAG, "Received: dogName=" + dogName + ", urgencyLevel=" + urgencyLevel + ", mainImageUrl=" + mainImageUrl);
        } else {
            Log.e(TAG, "No initial dog data found in arguments!");
            Toast.makeText(getContext(), "Error loading initial dog data.", Toast.LENGTH_SHORT).show();
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

        // Initialize Firebase
        storage = FirebaseStorage.getInstance();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Setup toolbar
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
            if (dogName != null) {
                toolbar.setTitle("Vet Info for " + dogName);
            }
        }

        // Set click listeners
        btnSave.setOnClickListener(v -> handleSaveButtonClick());
        btnSkip.setOnClickListener(v -> handleSkipButtonClick());

        return view;
    }

    private void handleSaveButtonClick() {
        // Collect vet data
        String vetName = editVetName.getText() != null ? editVetName.getText().toString().trim() : "";
        String clinicName = editClinicName.getText() != null ? editClinicName.getText().toString().trim() : "";
        String clinicAddress = editClinicAddress.getText() != null ? editClinicAddress.getText().toString().trim() : "";
        String vetPhone = editVetPhone.getText() != null ? editVetPhone.getText().toString().trim() : "";
        String vetEmail = editVetEmail.getText() != null ? editVetEmail.getText().toString().trim() : "";
        String medicalHistory = editMedicalHistory.getText() != null ? editMedicalHistory.getText().toString().trim() : "";

        // Validate required fields
        if (vetName.isEmpty()) {
            inputVetName.setError("Vet Name is required");
            return;
        } else {
            inputVetName.setError(null);
        }
        if (vetPhone.isEmpty()) {
            inputVetPhone.setError("Vet Phone is required");
            return;
        } else {
            inputVetPhone.setError(null);
        }

        // Create HomeModel with all data
        HomeModel dogData = new HomeModel();
        dogData.setDogName(dogName);
        dogData.setDescription(description);
        dogData.setPatreonUrl(patreonUrl);
        dogData.setUrgencylevel(urgencyLevel);
        dogData.setVetName(vetName);
        dogData.setClinicName(clinicName);
        dogData.setClinicAddress(clinicAddress);
        dogData.setVetPhone(vetPhone);
        dogData.setVetEmail(vetEmail);
        dogData.setMedicalHistory(medicalHistory);
        dogData.setCreator(auth.getCurrentUser().getUid());
        dogData.setFundingPercentage(0);
        dogData.setFundingAmount(0);
        dogData.setDonationsAmount(0);
        dogData.setSupporters(new ArrayList<>());

        // Generate dogId early
        String dogId = firestore.collection("dogs").document().getId();
        dogData.setDogId(dogId);

        // Save to Firebase
        if (mainImageUrl != null) {
            saveDogInfo(mainImageUrl, galleryImages, dogData);
        } else {
            Toast.makeText(getContext(), "Main image is missing, cannot save.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Main image Uri is null");
            if (listener != null) {
                listener.onDogCreationComplete();
            }
            getParentFragmentManager().popBackStack();
        }
    }

    private void handleSkipButtonClick() {
        Log.d(TAG, "Skipping vet info");
        HomeModel dogData = new HomeModel();
        dogData.setDogName(dogName);
        dogData.setDescription(description);
        dogData.setPatreonUrl(patreonUrl);
        dogData.setUrgencylevel(urgencyLevel);
        dogData.setCreator(auth.getCurrentUser().getUid());
        dogData.setFundingPercentage(0);
        dogData.setFundingAmount(0);
        dogData.setDonationsAmount(0);
        dogData.setSupporters(new ArrayList<>());

        // Generate dogId early
        String dogId = firestore.collection("dogs").document().getId();
        dogData.setDogId(dogId);

        if (mainImageUrl != null) {
            saveDogInfo(mainImageUrl, galleryImages, dogData);
        } else {
            Toast.makeText(getContext(), "Main image is missing, cannot save.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Main image Uri is null");
            if (listener != null) {
                listener.onDogCreationComplete();
            }
            getParentFragmentManager().popBackStack();
        }
    }

    private void saveDogInfo(Uri mainImageUri, ArrayList<Uri> galleryUris, HomeModel dogData) {
        progressBar.setVisibility(View.VISIBLE);

        // Upload main image
        StorageReference mainImageRef = storage.getReference()
                .child("dogs/" + auth.getCurrentUser().getUid() + "/" + dogData.getDogId() + "_main.jpg");
        mainImageRef.putFile(mainImageUri)
                .addOnSuccessListener(taskSnapshot -> mainImageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            dogData.setMainImage(uri.toString());

                            // Upload gallery images
                            ArrayList<String> galleryUrls = new ArrayList<>();
                            if (galleryUris != null && !galleryUris.isEmpty()) {
                                uploadGalleryImages(0, galleryUris, galleryUrls, dogData);
                            } else {
                                dogData.setGalleryImages(galleryUrls);
                                saveFirestoreDogData(dogData);
                            }
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(getContext(), "Error getting main image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error getting main image URL", e);
                            if (listener != null) {
                                listener.onDogCreationComplete();
                            }
                            getParentFragmentManager().popBackStack();
                        }))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error uploading main image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error uploading main image", e);
                    if (listener != null) {
                        listener.onDogCreationComplete();
                    }
                    getParentFragmentManager().popBackStack();
                });
    }

    private void uploadGalleryImages(int index, ArrayList<Uri> galleryUris, ArrayList<String> galleryUrls, HomeModel dogData) {
        if (index >= galleryUris.size()) {
            dogData.setGalleryImages(galleryUrls);
            saveFirestoreDogData(dogData);
            return;
        }

        Uri currentUri = galleryUris.get(index);
        StorageReference galleryImageRef = storage.getReference()
                .child("dogs/" + auth.getCurrentUser().getUid() + "/" + dogData.getDogId() + "_gallery" + index + ".jpg");

        galleryImageRef.putFile(currentUri)
                .addOnSuccessListener(taskSnapshot -> galleryImageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            galleryUrls.add(uri.toString());
                            uploadGalleryImages(index + 1, galleryUris, galleryUrls, dogData);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error getting gallery image URL for index " + index, e);
                            uploadGalleryImages(index + 1, galleryUris, galleryUrls, dogData);
                        }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error uploading gallery image for index " + index, e);
                    uploadGalleryImages(index + 1, galleryUris, galleryUrls, dogData);
                });
    }

    private void saveFirestoreDogData(HomeModel dogData) {
        Map<String, Object> dogMap = new HashMap<>();
        dogMap.put("dogId", dogData.getDogId()); // Explicitly include dogId
        dogMap.put("creator", dogData.getCreator());
        dogMap.put("dogName", dogData.getDogName());
        dogMap.put("description", dogData.getDescription());
        dogMap.put("urgencyLevel", dogData.getUrgencylevel());
        dogMap.put("patreonUrl", dogData.getPatreonUrl());
        dogMap.put("mainImage", dogData.getMainImage());
        dogMap.put("galleryImages", dogData.getGalleryImages());
        dogMap.put("vetName", dogData.getVetName());
        dogMap.put("clinicName", dogData.getClinicName());
        dogMap.put("clinicAddress", dogData.getClinicAddress());
        dogMap.put("vetPhone", dogData.getVetPhone());
        dogMap.put("vetEmail", dogData.getVetEmail());
        dogMap.put("medicalHistory", dogData.getMedicalHistory());
        dogMap.put("fundingPercentage", dogData.getFundingPercentage());
        dogMap.put("fundingAmount", dogData.getFundingAmount());
        dogMap.put("donationsAmount", dogData.getDonationsAmount());
        dogMap.put("supporters", dogData.getSupporters());
        dogMap.put("accessToken", dogData.getAccessToken());
        dogMap.put("dog_lower", dogData.getDogName() != null ? dogData.getDogName().toLowerCase() : "");
        dogMap.put("dog_keywords", generateKeywords(dogData.getDogName()));

        // Save to Firestore with explicit document ID
        firestore.collection("dogs").document(dogData.getDogId())
                .set(dogMap)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Dog saved successfully", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Dog saved with ID: " + dogData.getDogId());

                    // Navigate to HomeFragment
                    FragmentManager fragmentManager = getParentFragmentManager();
                    FragmentTransaction ft = fragmentManager.beginTransaction();
                    HomeFragment homeFragment = new HomeFragment();
                    ft.replace(R.id.fragment_container, homeFragment);
                    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    ft.commit();

                    if (listener != null) {
                        listener.onDogCreationComplete();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error saving dog to Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving dog to Firestore", e);
                    if (listener != null) {
                        listener.onDogCreationComplete();
                    }
                    getParentFragmentManager().popBackStack();
                });
    }

    // Helper method to generate keywords (simplified example)
    private ArrayList<String> generateKeywords(String dogName) {
        ArrayList<String> keywords = new ArrayList<>();
        if (dogName != null && !dogName.isEmpty()) {
            String lowerName = dogName.toLowerCase();
            for (int i = 0; i < lowerName.length(); i++) {
                for (int j = i + 1; j <= lowerName.length(); j++) {
                    keywords.add(lowerName.substring(i, j));
                }
            }
        }
        return keywords;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}