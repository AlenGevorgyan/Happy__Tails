package com.app.happytails.utils.Adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.app.happytails.R;
import com.app.happytails.utils.Fragments.ProfileFragment;
import com.app.happytails.utils.model.UserModel;
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class SupportersAdapter extends RecyclerView.Adapter<SupportersAdapter.SupporterViewHolder> {

    private Context context;
    private List<UserModel> supportersList;
    private FragmentManager fragmentManager;

    public SupportersAdapter(Context context, List<UserModel> supportersList, FragmentManager fragmentManager) {
        this.context = context;
        this.supportersList = supportersList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public SupporterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_supporter, parent, false);
        return new SupporterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SupporterViewHolder holder, int position) {
        UserModel supporter = supportersList.get(position);

        holder.usernameTextView.setText(supporter.getUsername());

        if (supporter.getUserImage() != null && !supporter.getUserImage().isEmpty()) {
            Glide.with(context)
                    .load(supporter.getUserImage())
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.user_icon);
        }

        holder.profileImageView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("creator", supporter.getUserId());
            ProfileFragment profileFragment = new ProfileFragment();
            profileFragment.setArguments(bundle);

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, profileFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });
    }

    @Override
    public int getItemCount() {
        return supportersList.size();
    }

    public static class SupporterViewHolder extends RecyclerView.ViewHolder {
        TextView usernameTextView;
        CircleImageView profileImageView;

        public SupporterViewHolder(View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.usernameTextS);
            profileImageView = itemView.findViewById(R.id.profile_pic_ImageView);
        }
    }
}