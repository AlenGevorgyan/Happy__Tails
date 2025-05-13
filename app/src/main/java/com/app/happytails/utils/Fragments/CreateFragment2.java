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
import androidx.fragment.app.FragmentManager; // Import FragmentManager
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.GalleryAdapter;
import com.app.happytails.utils.model.HomeModel; // Import the HomeModel class

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
// Removed unused imports: Context

public class CreateFragment2 extends Fragment {

    // Removed OnDogInfoCollectedListener interface as navigation is handled internally
    // public interface OnDogInfoCollectedListener {
    //     void onDogInfoCollected(HomeModel dogData);
    // }
    // Removed listener variable
    // private OnDogInfoCollectedListener listener;

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

    public CreateFragment2() {
        // Required empty public constructor
    }

    // Removed onAttach method as listener is no longer used for navigation
    // @Override
    // public void onAttach(@NonNull Context context) {
    //     super.onAttach(context);
    //     if (context instanceof OnDogInfoCollectedListener) {
    //         listener = (OnDogInfoCollectedListener) context;
    //     } else {
    //         throw new RuntimeException(context.toString()
    //                 + " must implement OnDogInfoCollectedListener");
    //     }
    // }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        dogName = view.findViewById(R.id.dog_name);
        descriptionED = view.findViewById(R.id.dog_description);
        dogPic = view.findViewById(R.id.mainProfileImage);
        dogGalleryPic = view.findViewById(R.id.dogPic);
        recyclerView = view.findViewById(R.id.dogGallery);
        nextButton = view.findViewById(R.id.postNextBtn);
        urgencyLevelValue = view.findViewById(R.id.urgencyLevelValue);
        urgencyLevelButton = view.findViewById(R.id.btn_select_urgency_level);
        progbar = view.findViewById(R.id.create_progress);
        patreonUrl = view.findViewById(R.id.patreon_url);

        // Setup RecyclerView (if layout still includes it)
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            galleryAdapter = new GalleryAdapter(getContext(), galleryUris);
            recyclerView.setAdapter(galleryAdapter);
        }

        // Handle the button click – now collecting info and navigating directly
        nextButton.setOnClickListener(v -> handleNextButtonClick());

        // Handle image selections (Keep if image selection is done in this fragment)
        if (dogPic != null) {
            dogPic.setOnClickListener(v -> openMainImageGallery());
        }
        if (dogGalleryPic != null) {
            dogGalleryPic.setOnClickListener(v -> openGalleryForDogImages());
        }

        // Handle urgency level selection
        if (urgencyLevelButton != null) {
            urgencyLevelButton.setOnClickListener(v -> openUrgencyLevelDialog());
        }
    }

    private void handleNextButtonClick() {
        String name = dogName.getText().toString().trim();
        String description = descriptionED.getText().toString().trim();
        String patreon_url = patreonUrl.getText().toString().trim();

        // Perform basic validation
        if (name.isEmpty() || description.isEmpty() || patreon_url.isEmpty() || urgencyLevel == 0) {
            Toast.makeText(getContext(), "Please fill all fields and select urgency level", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate the Patreon URL
        if (!isValidPatreonUrl(patreon_url)) {
            patreonUrl.setError("Incorrect URL: please enter a valid Patreon creator URL");
            patreonUrl.requestFocus();
            return;
        }

        // Removed main image check as image handling is moved to the parent flow
        // if (mainImageUri == null) {
        //     Toast.makeText(getContext(), "Please select a main image", Toast.LENGTH_SHORT).show();
        //     return;
        // }

        // --- Direct Navigation to VetPageSelectionFragment ---
        // Create a Bundle to pass the collected dog data
        Bundle bundle = new Bundle();
        bundle.putString("dogName", name);
        bundle.putString("patreonUrl", patreon_url);
        bundle.putString("description", description);
        bundle.putInt("urgencyLevel", urgencyLevel);
        bundle.putParcelableArrayList("galleryImages", galleryUris);
        bundle.putParcelable("mainImageUrl", mainImageUri);

        // Create an instance of the VetPageSelectionFragment
        VetFragment fragment = new VetFragment();
        // Set the bundle as arguments for the next fragment
        fragment.setArguments(bundle);

        // Get the FragmentManager and start a transaction
        FragmentManager fragmentManager = getParentFragmentManager(); // Use getParentFragmentManager()
        FragmentTransaction ft = fragmentManager.beginTransaction();

        // Replace the current fragment with the VetPageSelectionFragment
        // IMPORTANT: Replace R.id.fragment_container with the actual ID of the
        // container view in your Activity's layout where these fragments are displayed.
        ft.replace(R.id.fragment_container, fragment); // <-- REPLACE R.id.fragment_container

        // Add animation for the transition (optional)
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        ft.addToBackStack(null);

        ft.commit();
    }

    // Keep URL validation logic
    private boolean isValidPatreonUrl(String urlString) {
//        try {
//            URL url = new URL(urlString);
//            String protocol = url.getProtocol();
//            if (!protocol.equals("http") && !protocol.equals("https")) {
//                return false;
//            }
//            String host = url.getHost().toLowerCase();
//            return host.equals("patreon.com") || host.endsWith(".patreon.com");
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//            return false;
//        }
        return true;
    }

    // Keep image selection methods if image selection is done in this fragment
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

    // Keep onActivityResult if image selection is done in this fragment
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK && data != null) {
            if (requestCode == 1) { // Result for main image
                mainImageUri = data.getData();
                if (dogPic != null) {
                    dogPic.setImageURI(mainImageUri);
                }
            } else if (requestCode == 2) { // Result for gallery images
                galleryUris.clear(); // Clear previous selections
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri imageUri = data.getClipData().getItemAt(i).getUri();
                        galleryUris.add(imageUri);
                    }
                } else if (data.getData() != null) {
                    galleryUris.add(data.getData());
                }
                if (galleryAdapter != null) {
                    galleryAdapter.notifyDataSetChanged();
                }
            }
        }
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
            if (urgencyLevelValue != null) {
                urgencyLevelValue.setText(selectedText);
            }
            try {
                urgencyLevel = Integer.parseInt(radioButton.getTag().toString());
            } catch (NumberFormatException e) {
                urgencyLevel = 0;
                Toast.makeText(getContext(), "Invalid urgency level tag", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

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

    // Removed onDetach method as listener is no longer used for navigation
    // @Override
    // public void onDetach() {
    //     super.onDetach();
    //     listener = null;
    // }
}
