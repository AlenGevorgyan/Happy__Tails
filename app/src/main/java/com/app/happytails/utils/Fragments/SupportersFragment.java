package com.app.happytails.utils.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.app.happytails.R;
import com.app.happytails.utils.Adapters.SupportersAdapter;
import com.app.happytails.utils.model.UserModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SupportersFragment extends Fragment {

    private RecyclerView supportersRecyclerView;
    private SupportersAdapter supportersAdapter;
    private List<UserModel> supportersList;

    public SupportersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_supporters, container, false);

        supportersRecyclerView = view.findViewById(R.id.supporters_recycler_view);
        supportersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        supportersList = new ArrayList<>();

        supportersAdapter = new SupportersAdapter(requireContext(), supportersList, getParentFragmentManager());
        supportersRecyclerView.setAdapter(supportersAdapter);

        loadSupporters();

        return view;
    }

    private void loadSupporters() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Bundle args = getArguments();
        if (args != null) {
            String dogId = args.getString("dogId");
            if (dogId != null) {
                db.collection("dogs").document(dogId)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document != null && document.exists()) {
                                    List<String> supportersIds = (List<String>) document.get("supporters");
                                    if (supportersIds != null && !supportersIds.isEmpty()) {
                                        supportersList.clear();
                                        for (String userId : supportersIds) {
                                            db.collection("users").document(userId)
                                                    .get()
                                                    .addOnCompleteListener(userTask -> {
                                                        if (userTask.isSuccessful()) {
                                                            DocumentSnapshot userDoc = userTask.getResult();
                                                            if (userDoc != null && userDoc.exists()) {
                                                                UserModel supporter = userDoc.toObject(UserModel.class);
                                                                if (supporter != null) {
                                                                    supportersList.add(supporter);
                                                                }
                                                            }
                                                        } else {
                                                            Toast.makeText(getContext(), "Error fetching supporter data", Toast.LENGTH_SHORT).show();
                                                        }

                                                        supportersAdapter.notifyDataSetChanged();
                                                    });
                                        }
                                    } else {
                                        Toast.makeText(getContext(), "No supporters found", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
                                Toast.makeText(getContext(), "Error getting dog data", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(getContext(), "Dog ID is missing", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Arguments are missing", Toast.LENGTH_SHORT).show();
        }
    }
}