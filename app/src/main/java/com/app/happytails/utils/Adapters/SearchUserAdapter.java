package com.app.happytails.utils.Adapters;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.AndroidUtil;
import com.app.happytails.utils.FirebaseUtil;
import com.app.happytails.utils.Fragments.ProfileFragment;
import com.app.happytails.utils.model.UserSearchModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.List;

public class SearchUserAdapter extends FirestoreRecyclerAdapter<UserSearchModel, SearchUserAdapter.UserModelViewHolder> {

    private final Context context;
    private final FragmentManager fragmentManager;
    private List<UserSearchModel> userList;

    public SearchUserAdapter(@NonNull FirestoreRecyclerOptions<UserSearchModel> options, Context context, FragmentManager fragmentManager) {
        super(options);
        this.context = context;
        this.fragmentManager = fragmentManager;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserModelViewHolder holder, int position, @NonNull UserSearchModel model) {
        holder.usernameText.setText(model.getUsername());
        if (model.getUserId().equals(FirebaseUtil.currentUserId())) {
            holder.usernameText.setText(model.getUsername() + " (Me)");
        }
        loadProfileImage(model.getUserId(), holder.profilePic);

        holder.itemView.setOnClickListener(v -> {
            ProfileFragment profileFragment = new ProfileFragment();
            Bundle bundle = new Bundle();
            bundle.putString("creator", model.getUserId());
            bundle.putInt("container_id", R.id.search_container);
            profileFragment.setArguments(bundle);

            fragmentManager.beginTransaction()
                    .replace(R.id.search_container, profileFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadProfileImage(String userId, CircleImageView profilePic) {
        FirebaseUtil.getOtherProfileImage(userId).addOnCompleteListener(imageTask -> {
            if (imageTask.isSuccessful() && imageTask.getResult() != null) {
                AndroidUtil.setProfilePic(profilePic.getContext(), Uri.parse(imageTask.getResult()), profilePic);
            } else {
                profilePic.setImageResource(R.drawable.user_icon);
            }
        });
    }

    @NonNull
    @Override
    public UserModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserModelViewHolder(view);
    }

    public void updateData(List<UserSearchModel> newUsers) {
        this.userList = newUsers;
        notifyDataSetChanged();
    }

    static class UserModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        CircleImageView profilePic;

        public UserModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.usernameText);
            profilePic = itemView.findViewById(R.id.profilePic);
        }
    }
}