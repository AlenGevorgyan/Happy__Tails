package com.app.happytails.utils.Fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.PostAdapter;
import com.app.happytails.utils.AndroidUtil;
import com.app.happytails.utils.ChatActivity;
import com.app.happytails.utils.FollowersActivity;
import com.app.happytails.utils.FollowingsActivity;
import com.app.happytails.utils.model.HomeModel;
import com.app.happytails.utils.model.UserModel;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String ARG_CONTAINER_ID = "container_id";

    private TextView statusTv, followingCountTv, postCountTv, username, followerCountTv, followerTv, followingTv;
    private CircleImageView profileImage;
    private RecyclerView recyclerView;
    private ImageButton settingsBtn, back_profile, chatBtn;
    private Button followBtn;
    private FirebaseUser currentUser;
    private PostAdapter postAdapter;
    private String profileUid;
    private String currentUserId;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Context context;

    public interface OnFragmentInteractionListener {
        void onProfileFragmentClosed();
    }

    private OnFragmentInteractionListener listener;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(int containerId) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CONTAINER_ID, containerId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;
        listener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        init(view);
        loadBasicData();
        setupRecyclerView();
        loadUserPosts();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up click listeners
        followerTv.setOnClickListener(v -> openFollowersActivity());
        followingTv.setOnClickListener(v -> openFollowingActivity());
    }

    private void openFollowersActivity() {
        Intent intent = new Intent(getActivity(), FollowersActivity.class);
        intent.putExtra("userId", profileUid);
        startActivity(intent);
    }

    private void openFollowingActivity() {
        Intent intent = new Intent(getActivity(), FollowingsActivity.class);
        intent.putExtra("userId", profileUid);
        startActivity(intent);
    }

    private void init(View view) {
        statusTv = view.findViewById(R.id.statusTV);
        followerCountTv = view.findViewById(R.id.folower_countTv);
        followingCountTv = view.findViewById(R.id.following_countTv);
        postCountTv = view.findViewById(R.id.post_counttv);
        profileImage = view.findViewById(R.id.profileImage);
        recyclerView = view.findViewById(R.id.recyclerViewProfile);
        username = view.findViewById(R.id.nameTV);
        followBtn = view.findViewById(R.id.subscribeBtn);
        settingsBtn = view.findViewById(R.id.settings_profile);
        back_profile = view.findViewById(R.id.back_profile);
        chatBtn = view.findViewById(R.id.chatBtn);
        followerTv = view.findViewById(R.id.folower_Tv);
        followingTv = view.findViewById(R.id.following_Tv);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        currentUserId = currentUser != null ? currentUser.getUid() : null;
        profileUid = getArguments() != null ? getArguments().getString("creator") : currentUserId;

        if (profileUid != null && profileUid.equals(currentUserId)) {
            followBtn.setVisibility(View.GONE);
            chatBtn.setVisibility(View.GONE);
            settingsBtn.setVisibility(View.VISIBLE);
        } else {
            followBtn.setVisibility(View.VISIBLE);
            settingsBtn.setVisibility(View.GONE);
            chatBtn.setVisibility(View.VISIBLE);
            checkIfFollowing();
        }

        back_profile.setOnClickListener(v -> handleBackPress());
        settingsBtn.setOnClickListener(v -> openSettingsFragment());
        chatBtn.setOnClickListener(v -> navigateToTheChat());
        followBtn.setOnClickListener(v -> handleFollowButtonClick());
    }

    private void navigateToTheChat() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            DocumentReference userRef = db.collection("users").document(profileUid);
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    UserModel model = task.getResult().toObject(UserModel.class);
                    if (model != null) {
                        AndroidUtil.passUserModelAsIntent(intent, model);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        Log.e(TAG, "User model not found");
                    }
                } else {
                    Log.e(TAG, "Failed to load user data for chat");
                }
            });
        }
    }

    private void handleBackPress() {
        if (listener != null) {
            listener.onProfileFragmentClosed();
        }
        if (getActivity() != null && getActivity().getSupportFragmentManager() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void openSettingsFragment() {
        int containerId = getArguments() != null ? getArguments().getInt(ARG_CONTAINER_ID, R.id.fragment_container) : R.id.fragment_container;

        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(containerId, new UserSettingsFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void loadBasicData() {
        DocumentReference userRef = db.collection("users").document(profileUid);

        userRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(context, "Error loading profile data", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading profile data", error);
                return;
            }

            if (value != null && value.exists()) {
                String name = value.getString("username");
                List<String> followersList = (List<String>) value.get("followers");
                List<String> followingsList = (List<String>) value.get("followings");
                int followerCount = (followersList != null) ? followersList.size() : 0;
                int followingsCount = (followingsList != null) ? followingsList.size() : 0;
                Long posts = value.getLong("postCount");
                String profileURL = value.getString("userImage");
                String status = value.getString("status");

                username.setText(name != null ? name : "Unknown");
                statusTv.setText(status != null ? status : "No status");
                followerCountTv.setText(String.valueOf(followerCount));
                followingCountTv.setText(String.valueOf(followingsCount));
                postCountTv.setText(String.valueOf(posts != null ? posts : 0));

                if (profileURL != null && !profileURL.isEmpty()) {
                    Glide.with(context)
                            .load(profileURL)
                            .placeholder(R.drawable.user_icon)
                            .error(R.drawable.user_icon)
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.user_icon);
                }

                if (followersList != null && followersList.contains(currentUserId)) {
                    followBtn.setText("Unfollow");
                } else {
                    followBtn.setText("Follow");
                }
            } else {
                Log.d(TAG, "No profile data found for UID: " + profileUid);
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    private void loadUserPosts() {
        Query query = db.collection("dogs")
                .whereEqualTo("creator", profileUid);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<HomeModel> postList = new ArrayList<>();
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        HomeModel post = document.toObject(HomeModel.class);
                        if (post != null) {
                            postList.add(post);
                        }
                    }
                }
                postAdapter = new PostAdapter(context, postList);
                recyclerView.setAdapter(postAdapter);
            } else {
                Toast.makeText(context, "Error loading posts", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading posts", task.getException());
            }
        });
    }

    private void checkIfFollowing() {
        DocumentReference userRef = db.collection("users").document(profileUid);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot documentSnapshot = task.getResult();
                if (documentSnapshot.exists()) {
                    List<String> followers = (List<String>) documentSnapshot.get("followers");
                    if (followers != null && followers.contains(currentUserId)) {
                        followBtn.setText("Unfollow");
                    } else {
                        followBtn.setText("Follow");
                    }
                }
            }
        });
    }

    private void handleFollowButtonClick() {
        DocumentReference userRef = db.collection("users").document(profileUid);
        DocumentReference otherUserRef = db.collection("users").document(currentUserId);
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot documentSnapshot = task.getResult();
                if (documentSnapshot.exists()) {
                    List<String> followers = (List<String>) documentSnapshot.get("followers");
                    if (followers == null) {
                        followers = new ArrayList<>();
                    }
                    if (followers.contains(currentUserId)) {
                        followers.remove(currentUserId);
                        List<String> finalFollowers = followers;
                        userRef.update("followers", followers).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                followBtn.setText("Follow");
                                followerCountTv.setText(String.valueOf(finalFollowers.size()));
                            } else {
                                Toast.makeText(context, "Error unfollowing", Toast.LENGTH_SHORT).show();
                            }
                        });

                        otherUserRef.get().addOnCompleteListener(otherTask -> {
                            if (otherTask.isSuccessful() && otherTask.getResult() != null) {
                                DocumentSnapshot otherUserSnapshot = otherTask.getResult();
                                if (otherUserSnapshot.exists()) {
                                    List<String> followings = (List<String>) otherUserSnapshot.get("followings");
                                    if (followings == null) {
                                        followings = new ArrayList<>();
                                    }
                                    followings.remove(profileUid);
                                    List<String> finalFollowings = followings;
                                    otherUserRef.update("followings", followings).addOnCompleteListener(otherUpdateTask -> {
                                        if (otherUpdateTask.isSuccessful()) {
                                            followingCountTv.setText(String.valueOf(finalFollowings.size()));
                                        } else {
                                            Toast.makeText(context, "Error updating followings", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        });
                    } else {
                        followers.add(currentUserId);
                        List<String> finalFollowers1 = followers;
                        userRef.update("followers", followers).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                followBtn.setText("Unfollow");
                                followerCountTv.setText(String.valueOf(finalFollowers1.size()));
                            } else {
                                Toast.makeText(context, "Error following", Toast.LENGTH_SHORT).show();
                            }
                        });

                        otherUserRef.get().addOnCompleteListener(otherTask -> {
                            if (otherTask.isSuccessful() && otherTask.getResult() != null) {
                                DocumentSnapshot otherUserSnapshot = otherTask.getResult();
                                if (otherUserSnapshot.exists()) {
                                    List<String> followings = (List<String>) otherUserSnapshot.get("followings");
                                    if (followings == null) {
                                        followings = new ArrayList<>();
                                    }
                                    followings.add(profileUid);
                                    List<String> finalFollowings = followings;
                                    otherUserRef.update("followings", followings).addOnCompleteListener(otherUpdateTask -> {
                                        if (otherUpdateTask.isSuccessful()) {
                                            followingCountTv.setText(String.valueOf(finalFollowings.size()));
                                        } else {
                                            Toast.makeText(context, "Error updating followings", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (postAdapter != null) {
            postAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}