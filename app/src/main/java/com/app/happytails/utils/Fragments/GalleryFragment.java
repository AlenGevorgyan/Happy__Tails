package com.app.happytails.utils.Fragments;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.GalleryAdapter;

import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment {

    private static final String TAG = "GalleryFragment";
    private RecyclerView galleryRecyclerView;
    private GalleryAdapter galleryAdapter;
    private List<Uri> imageUris;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        galleryRecyclerView = view.findViewById(R.id.galleryRecyclerView);
        galleryRecyclerView.setHasFixedSize(true);
        galleryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));

        if (getArguments() != null && getArguments().containsKey("galleryImageUrls")) {
            ArrayList<String> imageUrls = getArguments().getStringArrayList("galleryImageUrls");
            Log.d(TAG, "Image URLs: " + imageUrls);
            if (imageUrls != null && !imageUrls.isEmpty()) {
                imageUris = new ArrayList<>();
                for (String url : imageUrls) {
                    Uri uri = Uri.parse(url);
                    imageUris.add(uri);
                }
                galleryAdapter = new GalleryAdapter(getContext(), imageUris);
                galleryRecyclerView.setAdapter(galleryAdapter);
            } else {
                Log.d(TAG, "No images to display");
                Toast.makeText(getContext(), "No images to display", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "Error loading gallery");
        }
    }
}