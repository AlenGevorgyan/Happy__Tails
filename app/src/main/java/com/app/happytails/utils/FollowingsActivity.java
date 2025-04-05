package com.app.happytails.utils;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.FollowersAdapter;
import com.app.happytails.utils.Adapters.FollowingsAdapter;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FollowingsActivity extends AppCompatActivity {

    private RecyclerView followersRecyclerView;
    private ImageButton imageButton;
    private static final String TAG = "FollowingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_followings);

        followersRecyclerView = findViewById(R.id.recyclerViewFollowing);
        imageButton = findViewById(R.id.followings_back);

        String userId = getIntent().getStringExtra("userId");

        if (userId == null || userId.isEmpty()) {
            Log.d(TAG, "No userId passed to activity.");
            return;
        }

        DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(userId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    List<String> followersList = (List<String>) document.get("followings");
                    if (followersList == null) {
                        followersList = new ArrayList<>();
                        Log.d(TAG, "Followers list is null, initializing to empty list.");
                    } else {
                        Log.d(TAG, "Followers list received with size: " + followersList.size());
                        for (String follower : followersList) {
                            Log.d(TAG, "Follower: " + follower);
                        }
                    }

                    FollowingsAdapter adapter = new FollowingsAdapter(this, followersList);
                    followersRecyclerView.setAdapter(adapter);
                    followersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                } else {
                    Log.d(TAG, "No document found for user: " + userId);
                }
            } else {
                Log.d(TAG, "Failed to retrieve document for user: " + userId);
            }
        });

        imageButton.setOnClickListener(v -> onBackPressed());
    }
}