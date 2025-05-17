package com.app.happytails.utils.Fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable; // Import Parcelable
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar; // Keep ProgressBar if used in layout for visual feedback
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
import com.app.happytails.utils.Adapters.GalleryAdapter; // Assuming this adapter is for displaying selected Uris
// Removed unused imports: HomeModel, FirebaseFirestore, MalformedURLException, URL

import java.util.ArrayList;
import java.util.List;
import java.net.MalformedURLException; // Keep MalformedURLException for URL validation
import java.net.URL; // Keep URL for URL validation

public class CreateFragment2 extends Fragment {

    private static final String TAG = "CreateFragment2";

    // UI elements from fragment_create layout (matching create_dog_account_layout immersive)
    private EditText dogNameEt, descriptionEt, patreonUrlEt; // Renamed for clarity
    private ImageView mainDogImage, addGalleryImage; // Renamed for clarity
    private Button nextButton;
    private RecyclerView galleryRecyclerView; // Renamed for clarity
    private ProgressBar progbar; // Keep ProgressBar if used in layout for visual feedback
    private TextView urgencyLevelValue;
    private Button urgencyLevelButton; // Renamed for clarity

    // Data to be collected
    private Uri mainImageUri;
    private final ArrayList<Uri> galleryUris = new ArrayList<>();
    private GalleryAdapter galleryAdapter; // Adapter for displaying selected gallery images (Uris)
    private int urgencyLevel = 0; // Default urgency level

    // Removed Firebase Firestore/Storage instances as initial save is later
    // private FirebaseFirestore db;
    // private StorageReference storageReference;

    // Removed dogId as it's generated later
    // private String dogId;

    // Assume the container ID in your Activity's layout where fragments are hosted
    private static final int FRAGMENT_CONTAINER_ID = R.id.fragment_container; // <-- Verify this ID

    public CreateFragment2() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment (assuming fragment_create.xml matches create_dog_account_layout)
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Removed Firebase Firestore initialization
        // db = FirebaseFirestore.getInstance();
        // storageReference = FirebaseStorage.getInstance().getReference("dog_images");

        // Initialize views (using IDs from create_dog_account_layout immersive)
        dogNameEt = view.findViewById(R.id.dog_name); // EditText for dog name
        descriptionEt = view.findViewById(R.id.dog_description); // EditText for description
        patreonUrlEt = view.findViewById(R.id.patreon_url); // EditText for Patreon URL
        mainDogImage = view.findViewById(R.id.mainProfileImage); // CircleImageView for main image
        addGalleryImage = view.findViewById(R.id.dogPic); // ImageView for adding gallery images (CardView wrapper not included here)
        galleryRecyclerView = view.findViewById(R.id.dogGallery); // RecyclerView for gallery images
        nextButton = view.findViewById(R.id.postNextBtn); // Button to proceed
        urgencyLevelValue = view.findViewById(R.id.urgencyLevelValue); // TextView for selected urgency level
        urgencyLevelButton = view.findViewById(R.id.btn_select_urgency_level); // Button to open urgency picker
        progbar = view.findViewById(R.id.create_progress); // ProgressBar (if used for visual feedback before navigation)


        // Setup RecyclerView for displaying selected gallery images (Uris)
        if (galleryRecyclerView != null) {
            galleryRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            // Initialize GalleryAdapter with the list of Uris
            galleryAdapter = new GalleryAdapter(getContext(), galleryUris); // Assuming GalleryAdapter takes Context and List<Uri>
            galleryRecyclerView.setAdapter(galleryAdapter);
        }

        // Handle the button click to collect info and navigate to the vet choice
        if (nextButton != null) {
            nextButton.setOnClickListener(v -> handleNextButtonClick());
        }

        // Handle image selections
        if (mainDogImage != null) {
            mainDogImage.setOnClickListener(v -> openGalleryForImage(1)); // Request code 1 for main image
        }
        if (addGalleryImage != null) {
            addGalleryImage.setOnClickListener(v -> openGalleryForImage(2)); // Request code 2 for gallery images
        }

        // Handle urgency level selection
        if (urgencyLevelButton != null) {
            urgencyLevelButton.setOnClickListener(v -> openUrgencyLevelDialog());
        }

        // Hide progress bar initially
        if (progbar != null) {
            progbar.setVisibility(View.GONE);
        }
    }

    /**
     * Collects basic dog info and navigates to the VetPageSelectionFragment.
     */
    private void handleNextButtonClick() {
        String name = dogNameEt.getText().toString().trim();
        String description = descriptionEt.getText().toString().trim();
        String patreon_url = patreonUrlEt.getText().toString().trim();

        // Perform basic validation
        if (name.isEmpty()) {
            dogNameEt.setError("Dog name is required");
            dogNameEt.requestFocus();
            return;
        }
        if (description.isEmpty()) {
            descriptionEt.setError("Description is required");
            descriptionEt.requestFocus();
            return;
        }
        if (patreon_url.isEmpty()) {
            patreonUrlEt.setError("Patreon URL is required");
            patreonUrlEt.requestFocus();
            return;
        }
        if (urgencyLevel == 0) {
            Toast.makeText(getContext(), "Please select urgency level", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mainImageUri == null) {
            Toast.makeText(getContext(), "Please select a main image", Toast.LENGTH_SHORT).show();
            return;
        }


        // Validate the Patreon URL
        if (!isValidPatreonUrl(patreon_url)) {
            patreonUrlEt.setError("Incorrect URL: please enter a valid Patreon creator URL");
            patreonUrlEt.requestFocus();
            return;
        }

        // --- Navigate to VetPageSelectionFragment ---
        // Create a Bundle to pass the collected dog data
        Bundle bundle = new Bundle();
        bundle.putString("dogName", name);
        bundle.putString("patreonUrl", patreon_url);
        bundle.putString("description", description);
        bundle.putInt("urgencyLevel", urgencyLevel);
        // Pass image Uris as ParcelableArrayList
        bundle.putParcelableArrayList("galleryImages", galleryUris);
        bundle.putParcelable("mainImageUrl", mainImageUri); // Pass main image Uri

        // Create an instance of the VetFragment (assuming this is your vet choice fragment)
        VetFragment vetFragment = new VetFragment(); // <-- Use your actual VetFragment class name
        // Set the bundle as arguments for the next fragment
        vetFragment.setArguments(bundle);

        // Get the FragmentManager and start a transaction
        FragmentManager fragmentManager = getParentFragmentManager(); // Use getParentFragmentManager()
        FragmentTransaction ft = fragmentManager.beginTransaction();

        // Replace the current fragment with the VetFragment
        // IMPORTANT: Replace R.id.fragment_container with the actual ID of the
        // container view in your Activity's layout where these fragments are displayed.
        ft.replace(FRAGMENT_CONTAINER_ID, vetFragment); // <-- Use the correct container ID

        // Add animation for the transition (optional)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        ft.addToBackStack(null); // Add to back stack to allow navigating back

        ft.commit();
    }

    /**
     * Validates if the provided URL is a valid Patreon URL.
     * @param urlString The URL string to validate.
     * @return true if the URL is a valid Patreon URL, false otherwise.
     */
    private boolean isValidPatreonUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https")) {
                return false;
            }
            String host = url.getHost().toLowerCase();
            return host.equals("patreon.com") || host.endsWith(".patreon.com");
        } catch (MalformedURLException e) {
            Log.e(TAG, "Invalid URL format: " + urlString, e);
            return false;
        }
    }

    /**
     * Opens the gallery to select an image.
     * @param requestCode The request code to identify the image selection purpose (main or gallery).
     */
    private void openGalleryForImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        if (requestCode == 2) { // For gallery images, allow multiple selection
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        }
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK && data != null) {
            if (requestCode == 1) { // Result for main image
                mainImageUri = data.getData();
                if (mainDogImage != null) {
                    mainDogImage.setImageURI(mainImageUri);
                }
            } else if (requestCode == 2) { // Result for gallery images
                galleryUris.clear(); // Clear previous selections for gallery
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        galleryUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    galleryUris.add(data.getData());
                }
                // Update the gallery adapter with the new list of Uris
                if (galleryAdapter != null) {
                    galleryAdapter.setImages(galleryUris); // Assuming GalleryAdapter has a setImages method
                }
            }
        }
    }

    /**
     * Opens a dialog to select the urgency level.
     */
    private void openUrgencyLevelDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.urgency_level_dialog, null); // Assuming this layout exists

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroup); // Assuming this ID in urgency_level_dialog
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton radioButton = dialogView.findViewById(checkedId);
            if (radioButton != null) {
                String selectedText = radioButton.getText().toString();
                if (urgencyLevelValue != null) {
                    urgencyLevelValue.setText(selectedText);
                }
                try {
                    // Get urgency level from the tag (assuming tag is set as integer string)
                    urgencyLevel = Integer.parseInt(radioButton.getTag().toString());
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid urgency level tag: " + radioButton.getTag(), e);
                    urgencyLevel = 0; // Reset to default on error
                    Toast.makeText(getContext(), "Error selecting urgency level", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss(); // Dismiss dialog after selection
            }
        });

        // Pre-select the urgency level if one was previously chosen
        if (urgencyLevel != 0) {
            int radioButtonId = getResources().getIdentifier("level" + urgencyLevel, "id", getContext().getPackageName());
            if (radioButtonId != 0) {
                RadioButton currentRadioButton = dialogView.findViewById(radioButtonId);
                if (currentRadioButton != null) {
                    currentRadioButton.setChecked(true);
                }
            }
        }
    }
}
