package com.app.happytails.utils.Adapters;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.app.happytails.R;
import com.app.happytails.utils.Fragments.DogProfile;
import com.app.happytails.utils.Fragments.ProfileFragment;
import com.app.happytails.utils.model.HomeModel;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.HomeViewHolder> {

    private List<HomeModel> postList;
    private Context context;

    public HomeAdapter(Context context, List<HomeModel> postList) {
        this.context = context;
        this.postList = postList;
    }

    @NonNull
    @Override
    public HomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_post, parent, false);
        return new HomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HomeViewHolder holder, int position) {
        HomeModel post = postList.get(position);

        // Check if the URI for the dog image is valid
        String dogImageUri = post.getMainImage();
        if (!dogImageUri.isEmpty() && dogImageUri != null) {
            Log.d("Home adapter", dogImageUri);
            Glide.with(context)
                    .load(Uri.parse(dogImageUri))
                    .placeholder(R.drawable.user_icon)
                    .error(R.drawable.user_icon)
                    .into(holder.dogPic);
        } else {

            holder.dogPic.setImageResource(R.drawable.user_icon); // Fallback image
        }

        // Set dog details
        holder.dogName.setText(post.getDogName() != null ? post.getDogName() : "No Data");
        holder.supportersList.setText(post.getSupporters() != null ?
                "Supporters count: " + String.valueOf(post.getSupporters().size()) : "Supporters count: 0");
        holder.fundingBar.setProgress(post.getFundingPercentage());

        if (post.getUrgencylevel() == 1) {
            holder.urgencyLevel.setText("Urgency Level: " + "Basic Needs");
        } else if (post.getUrgencylevel() == 2) {
            holder.urgencyLevel.setText("Urgency Level: " + "Mild Support");
        } else if (post.getUrgencylevel() == 3) {
            holder.urgencyLevel.setText("Urgency Level: " + "Moderate Help");
        } else if (post.getUrgencylevel() == 4) {
            holder.urgencyLevel.setText("Urgency Level: " + "Urgent Care");
        } else if (post.getUrgencylevel() == 5) {
            holder.urgencyLevel.setText("Urgency Level: " + "Critical");
        }

        // Fetch and set creator's details (username and picture)
        DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(post.getCreator());
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String username = task.getResult().getString("username");
                String userProfileImage = task.getResult().getString("userImage");

                holder.creatorName.setText(username != null ? username : "Unknown");

                // Check if the user profile image URI is valid
                if (!userProfileImage.isEmpty() && userProfileImage != null) {
                    Log.d("Home adapter", userProfileImage);
                    Glide.with(context)
                            .load(userProfileImage)
                            .placeholder(R.drawable.user_icon)
                            .into(holder.creatorImage);
                } else {
                    holder.creatorImage.setImageResource(R.drawable.user_icon); // Fallback image
                }
            } else {
                holder.creatorName.setText("Unknown");
                holder.creatorImage.setImageResource(R.drawable.user_icon);
            }
        });

        // Open DogProfile fragment when "View Profile" button is clicked
        holder.viewProfile.setOnClickListener(v -> {
            DogProfile fragment = new DogProfile();
            Bundle args = new Bundle();
            args.putString("dogId", post.getDogId());
            fragment.setArguments(args);
            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Open ProfileFragment when creator's image is clicked
        holder.creatorImage.setOnClickListener(v -> {
            ProfileFragment fragment = new ProfileFragment();
            Bundle args = new Bundle();
            args.putString("creator", post.getCreator());
            fragment.setArguments(args);
            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }




    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class HomeViewHolder extends RecyclerView.ViewHolder {
        CircleImageView dogPic, creatorImage;
        TextView dogName, supportersList, creatorName, urgencyLevel;
        ProgressBar fundingBar;
        Button viewProfile;

        public HomeViewHolder(@NonNull View itemView) {
            super(itemView);
            dogPic = itemView.findViewById(R.id.post_picture_home);
            dogName = itemView.findViewById(R.id.dog_name_home);
            supportersList = itemView.findViewById(R.id.supporters_list_home);
            fundingBar = itemView.findViewById(R.id.funding_bar_home);
            viewProfile = itemView.findViewById(R.id.view_profile);
            creatorImage = itemView.findViewById(R.id.post_user_image);
            creatorName = itemView.findViewById(R.id.usernameTv);
            urgencyLevel = itemView.findViewById(R.id.urgency_level_home);
        }
    }
}