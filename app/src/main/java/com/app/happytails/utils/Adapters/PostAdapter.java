package com.app.happytails.utils.Adapters;

import android.content.Context;
import android.os.Bundle;
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
import com.app.happytails.utils.model.HomeModel;
import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<HomeModel> postList;
    private WeakReference<Context> contextRef;

    public PostAdapter(Context context, List<HomeModel> postList) {
        this.contextRef = new WeakReference<>(context);
        this.postList = postList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.profile_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        HomeModel post = postList.get(position);
        Context context = contextRef.get();

        if (context == null) return;

        if (post.getMainImage() != null && !post.getMainImage().isEmpty()) {
            Glide.with(context)
                    .load(post.getMainImage())
                    .placeholder(R.drawable.user_icon)
                    .into(holder.dogPic);
        } else {
            holder.dogPic.setImageResource(R.drawable.user_icon);
        }

        holder.dogName.setText(post.getDogName() != null ? post.getDogName() : "No Data");
        holder.supportersList.setText(post.getSupporters() != null ? "Supporters count: " + post.getSupporters().size() : "Supporters count: 0");
        holder.fundingBar.setProgress(post.getFundingPercentage());

        if (post.getUrgencylevel() == 1) {
            holder.urgencyLevel.setText("Urgency Level: " + "Basic Needs");
        }else if (post.getUrgencylevel() == 2){
            holder.urgencyLevel.setText("Urgency Level: " + "Mild Support");
        }if (post.getUrgencylevel() == 3){
            holder.urgencyLevel.setText("Urgency Level: " + "Moderate Help");
        }if (post.getUrgencylevel() == 4){
            holder.urgencyLevel.setText("Urgency Level: " + "Urgent Care");
        }if (post.getUrgencylevel() == 5){
            holder.urgencyLevel.setText("Urgency Level: " + "Critical");
        }

        holder.viewProfile.setOnClickListener(v -> {
            DogProfile fragment = new DogProfile();
            Bundle args = new Bundle();
            args.putString("dogId", post.getDogId());
            fragment.setArguments(args);

            if (context instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                activity.getSupportFragmentManager()
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

    static class PostViewHolder extends RecyclerView.ViewHolder {
        CircleImageView dogPic;
        TextView dogName, urgencyLevel, supportersList;
        ProgressBar fundingBar;
        Button viewProfile;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            dogPic = itemView.findViewById(R.id.post_picture);
            dogName = itemView.findViewById(R.id.dog_name);
            urgencyLevel = itemView.findViewById(R.id.urgency_level);
            supportersList = itemView.findViewById(R.id.supporters_list);
            fundingBar = itemView.findViewById(R.id.funding_bar);
            viewProfile = itemView.findViewById(R.id.view_profile_p);
        }
    }
}