package com.app.happytails.utils.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout; // Import LinearLayout for the container
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.happytails.R;

public class VetPageFragment extends Fragment { // Renamed class

    // Updated TextView members to match the improved layout IDs
    private TextView tvVetName, tvClinicName, tvClinicAddress, tvVetPhone, tvVetEmail, tvMedicalHistory;
    private TextView tvNoVetInfo; // TextView for the "no info" message
    private LinearLayout vetInfoContainer; // LinearLayout to show/hide vet details

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the improved layout
        return inflater.inflate(R.layout.fragment_vet_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize TextViews with IDs from the improved layout
        tvVetName = view.findViewById(R.id.tv_display_vet_name);
        tvClinicName = view.findViewById(R.id.tv_display_clinic_name);
        tvClinicAddress = view.findViewById(R.id.tv_display_clinic_address);
        tvVetPhone = view.findViewById(R.id.tv_display_vet_phone);
        tvVetEmail = view.findViewById(R.id.tv_display_vet_email);
        tvMedicalHistory = view.findViewById(R.id.tv_display_medical_history);

        // Initialize the "no info" TextView and the container LinearLayout
        tvNoVetInfo = view.findViewById(R.id.tv_no_vet_info);
        vetInfoContainer = view.findViewById(R.id.vet_info_container);


        loadVetInformation();
    }

    private void loadVetInformation() {
        Bundle args = getArguments();
        if (args != null) {
            // Retrieve the hasVetInfo flag
            boolean hasVetInfo = args.getBoolean("hasVetInfo", false);

            if (hasVetInfo) {
                // If vet info exists, show the container and hide the "no info" message
                vetInfoContainer.setVisibility(View.VISIBLE);
                tvNoVetInfo.setVisibility(View.GONE);

                // Retrieve and display vet information using keys passed from DogProfile.java
                tvVetName.setText(args.getString("vetName", "N/A"));
                tvClinicName.setText(args.getString("clinicName", "N/A"));
                tvClinicAddress.setText(args.getString("clinicAddress", "N/A"));
                tvVetPhone.setText(args.getString("vetPhone", "N/A"));
                tvVetEmail.setText(args.getString("vetEmail", "N/A"));
                tvMedicalHistory.setText(args.getString("medicalHistory", "N/A"));

            } else {
                // If no vet info, hide the container and show the "no info" message
                vetInfoContainer.setVisibility(View.GONE);
                tvNoVetInfo.setVisibility(View.VISIBLE);
                // The text for tvNoVetInfo is already set in the layout
            }
        } else {
            // Handle case where no arguments were passed
            vetInfoContainer.setVisibility(View.GONE);
            tvNoVetInfo.setVisibility(View.VISIBLE);
            tvNoVetInfo.setText("Error loading vet information."); // Provide an error message
        }
    }
}
