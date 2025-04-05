package com.app.happytails.utils.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class FollowingsAdapter extends RecyclerView.Adapter<FollowingsAdapter.ViewHolder> {

    private Context context;
    private List<String> followingsList;
    private FirebaseFirestore db;

    public FollowingsAdapter(Context context, List<String> followingsList) {
        this.context = context;
        this.followingsList = followingsList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (followingsList != null && !followingsList.isEmpty()) {
            String userId = followingsList.get(position);
            loadUserData(userId, holder);
        }
    }

    private void loadUserData(String userId, ViewHolder holder) {
        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String username = task.getResult().getString("username");
                String userImage = task.getResult().getString("userImage");

                holder.followingTextView.setText(username != null ? username : "Unknown");

                if (userImage != null && !userImage.isEmpty()) {
                    Glide.with(context)
                            .load(userImage)
                            .placeholder(R.drawable.user_icon)
                            .error(R.drawable.user_icon)
                            .into(holder.profileImage);
                } else {
                    holder.profileImage.setImageResource(R.drawable.user_icon);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return followingsList != null ? followingsList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView followingTextView;
        CircleImageView profileImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            followingTextView = itemView.findViewById(R.id.usernameText);
            profileImage = itemView.findViewById(R.id.profilePic);
        }
    }
}