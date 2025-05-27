package com.app.happytails.utils.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.HomeAdapter;
import com.app.happytails.utils.model.HomeModel;
import com.app.happytails.utils.SearchActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private HomeAdapter homeAdapter;
    private List<HomeModel> postList;
    private FirebaseFirestore db;
    private CollectionReference dogsCollection;
    private ListenerRegistration dogsListener;

    private static final String TAG = "HomeFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 🔍 Setup Search Icon Click
        ImageView searchIcon = view.findViewById(R.id.searchIcon);
        searchIcon.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SearchActivity.class);
            startActivity(intent);
        });

        recyclerView = view.findViewById(R.id.homeRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        postList = new ArrayList<>();
        homeAdapter = new HomeAdapter(getContext(), postList);
        recyclerView.setAdapter(homeAdapter);


        db = FirebaseFirestore.getInstance();
        dogsCollection = db.collection("dogs");

        loadPosts();

        return view;
    }

    private void loadPosts() {
        dogsListener = dogsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Firestore listen failed.", e);
                    return;
                }

                if (queryDocumentSnapshots != null) {
                    postList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        HomeModel post = mapDocumentToHomeModel(doc);
                        if (post != null) {
                            postList.add(post);
                        }
                    }
                    homeAdapter.notifyDataSetChanged();
                } else {
                    Log.d(TAG, "No documents found in the 'dogs' collection.");
                }
            }
        });
    }

    private HomeModel mapDocumentToHomeModel(DocumentSnapshot doc) {
        try {
            String dogId = doc.getId();
            String creator = doc.getString("creator");
            String dogName = doc.getString("dogName");
            String mainImageUrl = doc.getString("mainImage");
            int urgencyLevel = doc.getLong("urgencylevel") != null ? doc.getLong("urgencylevel").intValue() : 0;

            ArrayList<String> galleryImageUrls = doc.contains("galleryImages") ?
                    (ArrayList<String>) doc.get("galleryImages") : new ArrayList<>();

            int fundingProgress = doc.contains("fundingPercentage") ?
                    doc.getLong("fundingPercentage").intValue() : 0;

            ArrayList<String> supporters = doc.contains("supporters") ?
                    (ArrayList<String>) doc.get("supporters") : new ArrayList<>();

            return new HomeModel(
                    creator,
                    dogId,
                    dogName,
                    fundingProgress,
                    mainImageUrl,
                    supporters,
                    urgencyLevel
            );
        } catch (Exception e) {
            Log.e(TAG, "Error mapping Firestore document to HomeModel: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dogsListener != null) {
            dogsListener.remove();
        }
    }
}
