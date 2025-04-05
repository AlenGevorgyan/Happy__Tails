package com.app.happytails.utils.Fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.app.happytails.R;

public class VetPageFragment extends Fragment {

    private TextView vetName, vetDoctorName, vetLastVisitDate, vetDiagnosis, dogAgeTv, dogGender;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vet_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dogAgeTv = view.findViewById(R.id.dogAgeVet);
        dogGender = view.findViewById(R.id.dogGenderVet);
        vetName = view.findViewById(R.id.vetName);
        vetDoctorName = view.findViewById(R.id.vetDoctorName);
        vetLastVisitDate = view.findViewById(R.id.vet_last_visit_date);
        vetDiagnosis = view.findViewById(R.id.vetDiagnosis);

        loadVetInformation();
    }

    private void loadVetInformation() {
        if (getArguments() != null) {
            String name = getArguments().getString("vetName");
            String doctorName = getArguments().getString("docName");
            String lastVisitDate = getArguments().getString("vetLastVisitDate");
            String diagnosis = getArguments().getString("diagnosis");
            long dogAge = getArguments().getLong("dogAge");
            String gender = getArguments().getString("dogGender");

            vetName.setText("Clinic Name: " + name);
            vetDoctorName.setText("Doctor: " + doctorName);
            vetLastVisitDate.setText("Last Visit Date: " + lastVisitDate);
            vetDiagnosis.setText("Diagnosis: " + diagnosis);
            dogGender.setText("Gender: " + gender);
            dogAgeTv.setText("Estimated Age: " + String.valueOf(dogAge));
        }
    }
}