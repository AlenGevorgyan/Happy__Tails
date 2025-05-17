package com.app.happytails.utils.Adapters;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    private List<Uri> imageUris;
    private Context context;
    private static final String TAG = "GalleryAdapter";

    public GalleryAdapter(Context context, List<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.images_item, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);
        Log.d(TAG, "Loading image from URI: " + imageUri);

        if (imageUri != null) {
            Glide.with(context)
                    .load(imageUri)
                    .placeholder(R.drawable.user_icon)
                    .error(R.drawable.user_icon)
                    .timeout(6500)
                    .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.user_icon);
        }
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    public void setImages(List<Uri> uriList) {
        this.imageUris = uriList;
        notifyDataSetChanged(); // Notify the adapter that the data set has changed
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}