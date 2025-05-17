package com.app.happytails.utils.Adapters;

import android.app.AlertDialog; // Import AlertDialog
import android.content.Context;
import android.content.DialogInterface; // Import DialogInterface
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.ChangeDogActivity;
import com.app.happytails.utils.FirebaseUtil;
import com.app.happytails.utils.Fragments.DogProfile;
import com.app.happytails.utils.model.HomeModel;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<HomeModel> postList;
    private final Context context;

    public PostAdapter(Context context, List<HomeModel> postList) {
        this.context = context;
        this.postList = postList != null ? postList : new ArrayList<>();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Ensure this inflates the correct layout for a single post item (e.layout.profile_post)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, final int position) {
        HomeModel post = postList.get(position);

        // Load dog image
        Glide.with(context)
                .load(post.getMainImage())
                .placeholder(R.drawable.user_icon) // Placeholder while loading
                .error(R.drawable.user_icon) // Error image if loading fails
                .into(holder.dogPic);

        // Show delete/change buttons only for the post creator
        boolean isCreator = post.getCreator() != null && post.getCreator().equals(FirebaseUtil.currentUserId());
        holder.deleteBtn.setVisibility(isCreator ? View.VISIBLE : View.GONE);
        holder.changeBtn.setVisibility(isCreator ? View.VISIBLE : View.GONE);

        // --- Delete post logic with confirmation dialog ---
        holder.deleteBtn.setOnClickListener(v -> {
            if (post.getDogId() != null) {
                // Show confirmation dialog
                new AlertDialog.Builder(context)
                        .setTitle("Delete Post")
                        .setMessage("Are you sure you want to delete this post?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            // User confirmed deletion, proceed with Firestore deletion
                            FirebaseFirestore.getInstance().collection("dogs").document(post.getDogId()) // This line deletes from Firestore
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        // Remove the post from the list and notify adapter
                                        postList.remove(position);
                                        notifyItemRemoved(position);
                                        Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to delete post", Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null) // Do nothing on cancel
                        .setIcon(android.R.drawable.ic_dialog_alert) // Optional icon
                        .show();
            } else {
                Toast.makeText(context, "Cannot delete: Dog ID not found for this post", Toast.LENGTH_SHORT).show();
            }
        });
        // --- End delete post logic ---

        // Edit post
        holder.changeBtn.setOnClickListener(v -> {
            if (post.getDogId() != null && !post.getDogId().isEmpty()) {
                Intent intent = new Intent(context, ChangeDogActivity.class);
                intent.putExtra("dogId", post.getDogId());
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "Cannot edit: Dog ID not found", Toast.LENGTH_SHORT).show();
            }
        });

        // Set text fields
        holder.dogName.setText(post.getDogName() != null ? post.getDogName() : "No Name");
        // Display supporters count
        holder.supportersList.setText(post.getSupporters() != null ? "Supporters count: " + post.getSupporters().size() : "Supporters count: 0");
        // Set funding progress
        holder.fundingBar.setProgress(post.getFundingPercentage());

        // Set urgency level text based on the integer value
        String[] urgencyLevels = {"Unknown", "Basic Needs", "Mild Support", "Moderate Help", "Urgent Care", "Critical"};
        int level = post.getUrgencylevel();
        // Ensure level is within bounds
        if (level >= 0 && level < urgencyLevels.length) {
            holder.urgencyLevel.setText("Urgency Level: " + urgencyLevels[level]);
        } else {
            holder.urgencyLevel.setText("Urgency Level: Unknown");
        }


        // View dog profile
        holder.viewProfile.setOnClickListener(v -> {
            if (post.getDogId() != null) {
                DogProfile fragment = new DogProfile();
                Bundle args = new Bundle();
                args.putString("dogId", post.getDogId());
                fragment.setArguments(args);

                if (context instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) context;
                    activity.getSupportFragmentManager()
                            .beginTransaction()
                            // Assuming R.id.fragment_container is the ID of the container in your activity's layout
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null) // Add to back stack to allow navigating back
                            .commit();
                } else {
                    // Handle case where context is not an AppCompatActivity (e.g., show error)
                    Toast.makeText(context, "Error navigating to profile.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Cannot view profile: Dog ID not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // Method to update post list and refresh adapter
    public void updatePosts(List<HomeModel> newPosts) {
        this.postList = newPosts != null ? newPosts : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ViewHolder class to hold references to the views in each list item
    static class PostViewHolder extends RecyclerView.ViewHolder {
        CircleImageView dogPic;
        TextView dogName, urgencyLevel, supportersList;
        ProgressBar fundingBar;
        Button viewProfile;
        ImageButton deleteBtn, changeBtn;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views from the profile_post layout
            dogPic = itemView.findViewById(R.id.post_picture); // Assuming this ID in profile_post.xml
            dogName = itemView.findViewById(R.id.dog_name_profile); // Assuming this ID in profile_post.xml
            urgencyLevel = itemView.findViewById(R.id.urgency_level); // Assuming this ID in profile_post.xml
            supportersList = itemView.findViewById(R.id.supporters_list); // Assuming this ID in profile_post.xml
            fundingBar = itemView.findViewById(R.id.funding_bar); // Assuming this ID in profile_post.xml
            viewProfile = itemView.findViewById(R.id.view_profile_p); // Assuming this ID in profile_post.xml
            deleteBtn = itemView.findViewById(R.id.delete_button); // Assuming this ID in profile_post.xml
            changeBtn = itemView.findViewById(R.id.change_button); // Assuming this ID in profile_post.xml
        }
    }
}
