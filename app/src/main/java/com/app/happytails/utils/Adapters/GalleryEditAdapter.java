package com.app.happytails.utils.Adapters;

import android.app.AlertDialog; // Import AlertDialog
import android.content.Context;
import android.content.DialogInterface; // Import DialogInterface
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.model.GalleryImage;
import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class GalleryEditAdapter extends RecyclerView.Adapter<GalleryEditAdapter.GalleryViewHolder> {

    private static final String TAG = "GalleryEditAdapter";

    private List<GalleryImage> list;
    private Context context;
    // Removed OnImageDeletedListener as deletion is handled internally and we get the final list on save.
    // private OnImageDeletedListener deleteListener;

    // Removed OnImageDeletedListener interface
    // public interface OnImageDeletedListener {
    //     void onImageDeleted(String imageUrl);
    // }

    // Constructor - removed deleteListener parameter
    public GalleryEditAdapter(List<GalleryImage> list) {
        this.list = list;
    }

    public static class GalleryViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton deleteButton;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewEdit);
            deleteButton = itemView.findViewById(R.id.button_delete_image); // Initialize delete button
        }
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_edit_dog_image, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, final int position) {
        GalleryImage galleryImage = list.get(position);
        Object picUrlObject = galleryImage.getPicUrl();

        String imageUrl = null;
        if (picUrlObject instanceof String) {
            imageUrl = (String) picUrlObject;
        } else if (picUrlObject != null) {
            Log.w(TAG, "GalleryImage.getPicUrl() returned an unexpected type (" + picUrlObject.getClass().getName() + ") for item at position " + position);
        }


        // --- Load Image using Glide ---
        if (holder.imageView != null) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.color.light_gray)
                        .error(R.drawable.user_icon)
                        .into(holder.imageView);
            } else {
                holder.imageView.setImageResource(R.drawable.user_icon);
            }
        } else {
            Log.e(TAG, "ImageView is null for item at position: " + position + ". Image URL: " + imageUrl);
        }


        // --- Delete Button Listener ---
        if (holder.deleteButton != null) {
            // Only show and enable the delete button if we have a valid String URL
            if (imageUrl != null && !imageUrl.isEmpty()) {
                holder.deleteButton.setVisibility(View.VISIBLE);
                String finalImageUrl = imageUrl; // Need final variable for inner class
                holder.deleteButton.setOnClickListener(v -> {
                    // Show confirmation dialog before deleting
                    showDeleteConfirmationDialog(finalImageUrl, position);
                });
            } else {
                holder.deleteButton.setVisibility(View.GONE); // Hide button if no valid URL
            }
        } else {
            Log.e(TAG, "Delete button is null for item at position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void setImages(List<GalleryImage> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    public void removeImageAt(int position) {
        if (position >= 0 && position < list.size()) {
            // No need to notify activity here, as the activity will get the final list on save.
            list.remove(position);
            notifyItemRemoved(position);
        }
    }

    // Method to show delete confirmation dialog
    private void showDeleteConfirmationDialog(String imageUrl, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to delete this image?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // User confirmed deletion, proceed with Firebase Storage deletion
                    deleteImageFromFirebaseStorage(imageUrl, position);
                })
                .setNegativeButton("Cancel", null) // Do nothing on cancel
                .setIcon(android.R.drawable.ic_dialog_alert) // Optional icon
                .show();
    }


    // Method to delete an image from Firebase Storage
    private void deleteImageFromFirebaseStorage(String imageUrl, int position) {
        try {
            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

            imageRef.delete().addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Image deleted from Storage: " + imageUrl);
                // Remove the item from the adapter's list and update UI
                removeImageAt(position);
                Toast.makeText(context, "Image deleted", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(exception -> {
                Log.e(TAG, "Failed to delete image from Storage: " + imageUrl, exception);
                Toast.makeText(context, "Failed to delete image", Toast.LENGTH_SHORT).show();
                // Do not remove the item from the list if deletion failed
            });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid URL for Firebase Storage reference: " + imageUrl, e);
            Toast.makeText(context, "Invalid image URL", Toast.LENGTH_SHORT).show();
            // Optionally remove the item visually even if URL is invalid
            removeImageAt(position); // Remove locally if URL is invalid
        }
    }

    // Method to get the current list of images (for the activity to save to Firestore)
    public List<GalleryImage> getCurrentImages() {
        return list;
    }
}
