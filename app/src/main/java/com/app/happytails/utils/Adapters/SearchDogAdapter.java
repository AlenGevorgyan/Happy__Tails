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
import com.app.happytails.utils.Fragments.DogProfile;
import com.app.happytails.utils.model.DogSearchModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.List;

public class SearchDogAdapter extends FirestoreRecyclerAdapter<DogSearchModel, SearchDogAdapter.DogModelViewHolder> {

    private final Context context;
    private final FragmentManager fragmentManager;
    private List<DogSearchModel> dogList;

    public SearchDogAdapter(@NonNull FirestoreRecyclerOptions<DogSearchModel> options, Context context, FragmentManager fragmentManager) {
        super(options);
        this.context = context;
        this.fragmentManager = fragmentManager;
    }

    @Override
    protected void onBindViewHolder(@NonNull DogModelViewHolder holder, int position, @NonNull DogSearchModel model) {
        holder.dogNameText.setText(model.getDogName());
        loadDogImage(model.getDogId(), holder.profilePic);

        holder.itemView.setOnClickListener(v -> {
            DogProfile dogProfileFragment = new DogProfile();
            Bundle bundle = new Bundle();
            bundle.putString("dogId", model.getDogId());
            bundle.putInt("container_id", R.id.search_container);
            dogProfileFragment.setArguments(bundle);

            fragmentManager.beginTransaction()
                    .replace(R.id.search_container, dogProfileFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadDogImage(String dogId, CircleImageView profilePic) {
        FirebaseUtil.getDogProfileImage(dogId).addOnCompleteListener(imageTask -> {
            if (imageTask.isSuccessful() && imageTask.getResult() != null) {
                AndroidUtil.setProfilePic(profilePic.getContext(), Uri.parse(imageTask.getResult()), profilePic);
            } else {
                profilePic.setImageResource(R.drawable.baseline_add_24);
            }
        });
    }

    @NonNull
    @Override
    public DogModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new DogModelViewHolder(view);
    }

    public void updateData(List<DogSearchModel> newDogs) {
        this.dogList = newDogs;
        notifyDataSetChanged();
    }

    static class DogModelViewHolder extends RecyclerView.ViewHolder {
        TextView dogNameText;
        CircleImageView profilePic;

        public DogModelViewHolder(@NonNull View itemView) {
            super(itemView);
            dogNameText = itemView.findViewById(R.id.usernameText);
            profilePic = itemView.findViewById(R.id.profilePic);
        }
    }
}