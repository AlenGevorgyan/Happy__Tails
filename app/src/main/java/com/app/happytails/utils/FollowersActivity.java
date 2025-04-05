package com.app.happytails.utils;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.FollowersAdapter;
import com.app.happytails.utils.Fragments.ProfileFragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FollowersActivity extends AppCompatActivity implements ProfileFragment.OnFragmentInteractionListener {

    private RecyclerView followersRecyclerView;
    private ImageButton imageButton;
    private static final String TAG = "FollowersActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_followers);

        followersRecyclerView = findViewById(R.id.recyclerViewFollower);
        imageButton = findViewById(R.id.followers_back);

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
                    List<String> followersList = (List<String>) document.get("followers");
                    if (followersList == null) {
                        followersList = new ArrayList<>();
                        Log.d(TAG, "Followers list is null, initializing to empty list.");
                    } else {
                        Log.d(TAG, "Followers list received with size: " + followersList.size());
                        for (String follower : followersList) {
                            Log.d(TAG, "Follower: " + follower);
                        }
                    }

                    FollowersAdapter adapter = new FollowersAdapter(this, followersList);
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

    @Override
    public void onProfileFragmentClosed() {
        // Handle the fragment close interaction if needed
    }
}