package com.app.happytails.utils;

import android.content.Context;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.ChatRecyclerAdapter;
import com.app.happytails.utils.model.MessageModel;
import com.app.happytails.utils.model.ChatroomModel;
import com.app.happytails.utils.model.UserModel;
import com.app.happytails.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;

import java.util.Arrays;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";

    private UserModel otherUser;
    private String chatroomId;
    private ChatroomModel chatroomModel;
    private ChatRecyclerAdapter adapter;

    private EditText messageInput;
    private ImageButton sendMessageBtn;
    private RecyclerView recyclerView;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    /**
     * Static factory method to create Intent for ChatActivity
     */
    public static Intent newInstance(Context context, String userId, String username, String email, String fcmToken) {
        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("username", username);
        intent.putExtra("email", email);
        intent.putExtra("fcmToken", fcmToken);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Set up notification permission launcher
        notificationPermissionLauncher = NotificationHelper.createPermissionLauncher(this);
        
        // Check notification permission
        if (!NotificationHelper.hasNotificationPermission(this)) {
            NotificationHelper.requestNotificationPermissionWithLauncher(notificationPermissionLauncher);
        }

        // Initialize views and data
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        recyclerView = findViewById(R.id.chat_recycler_view);

        sendMessageBtn.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) sendMessageToUser(message);
        });

        getOrCreateChatroomModel();
        setupChatRecyclerView();
    }

    private void sendMessageToUser(String message) {
        Log.d(TAG, "Sending message: " + message);

        // 1) Update chatroom metadata
        chatroomModel.setLastMessage(message);
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        // 2) Add the message document
        MessageModel msg = new MessageModel(message, FirebaseUtil.currentUserId(), otherUser.getUserId(), Timestamp.now());
        FirebaseUtil.getChatroomMessageReference(chatroomId).add(msg)
                .addOnSuccessListener(docRef -> {
                    messageInput.setText("");

                    // 3) Send FCM notification to recipient
                    FirebaseUtil.getUserDetails(currentUser -> {
                        if (currentUser != null) {
                            String senderName = currentUser.getUsername();
                            // Use the new NotificationHelper to send the chat notification
                            NotificationHelper.sendChatNotification(
                                otherUser.getUserId(),
                                senderName,
                                message
                            );
                        } else {
                            Log.e(TAG, "Could not retrieve current user details");
                        }
                    });
                })
                .addOnFailureListener(e -> Log.e(TAG, "Message send failed", e));
    }

    void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<MessageModel> options = new FirestoreRecyclerOptions.Builder<MessageModel>()
                .setQuery(query, MessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null)
            adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null)
            adapter.stopListening();
    }

    void getOrCreateChatroomModel() {
        Log.d(TAG, "getOrCreateChatroomModel called");
        FirebaseUtil.getChatroomReference(chatroomId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                chatroomModel = task.getResult().toObject(ChatroomModel.class);
                if (chatroomModel == null) {
                    Log.d(TAG, "Chatroom does not exist, creating new chatroom");
                    chatroomModel = new ChatroomModel(
                            chatroomId,
                            Arrays.asList(FirebaseUtil.currentUserId(), otherUser.getUserId()),
                            Timestamp.now(),
                            ""
                    );
                    FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);
                } else {
                    Log.d(TAG, "Chatroom exists");
                }
            } else {
                Log.e(TAG, "Error getting chatroom", task.getException());
            }
        });
    }
}
