package com.app.happytails.utils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.ChatRecyclerAdapter;
import com.app.happytails.utils.model.ChatMessageModel;
import com.app.happytails.utils.model.ChatroomModel;
import com.app.happytails.utils.model.UserModel;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    UserModel otherUser;
    String chatroomId;
    ChatroomModel chatroomModel;
    ChatRecyclerAdapter adapter;

    EditText messageInput;
    ImageButton sendMessageBtn;
    ImageButton backBtn;
    TextView otherUsername;
    RecyclerView recyclerView;
    ImageView imageView;
    String currUsrName;

    private static final String TAG = "ChatActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get UserModel
        otherUser = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId = FirebaseUtil.getChatroomId(FirebaseUtil.currentUserId(), otherUser.getUserId());

        messageInput = findViewById(R.id.chat_message_input);
        sendMessageBtn = findViewById(R.id.message_send_btn);
        backBtn = findViewById(R.id.back_btn);
        otherUsername = findViewById(R.id.other_username);
        recyclerView = findViewById(R.id.chat_recycler_view);
        imageView = findViewById(R.id.profile_pic_inchat);

        FirebaseUtil.getOtherProfileImage(otherUser.getUserId())
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Uri uri = Uri.parse(t.getResult());
                        AndroidUtil.setProfilePic(this, uri, imageView);
                    }
                });

        backBtn.setOnClickListener((v) -> onBackPressed());
        otherUsername.setText(otherUser.getUsername());

        sendMessageBtn.setOnClickListener((v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessageToUser(message);
            }
        }));

        getOrCreateChatroomModel();
        setupChatRecyclerView();
    }

    void setupChatRecyclerView() {
        Query query = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<ChatMessageModel> options = new FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class).build();

        adapter = new ChatRecyclerAdapter(options, getApplicationContext());
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setReverseLayout(true);
        recyclerView.setLayoutManager(manager);
        recyclerView.setAdapter(adapter);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                recyclerView.smoothScrollToPosition(0);
            }
        });
    }

    void sendMessageToUser(String message) {
        Log.d(TAG, "sendMessageToUser called with message: " + message);

        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setLastMessage(message);
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        ChatMessageModel chatMessageModel = new ChatMessageModel(message, FirebaseUtil.currentUserId(), Timestamp.now());
        FirebaseUtil.getChatroomMessageReference(chatroomId).add(chatMessageModel)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Message sent successfully");
                        messageInput.setText("");
                        sendNotification(message, "");
                    } else {
                        Log.e(TAG, "Error sending message", task.getException());
                    }
                });
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

    public void sendNotification(String message, String lastPhoto) {
        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                // Use SendNotification helper to send the notification via backend
                SendNotification sendNotificationHelper = new SendNotification();
                sendNotificationHelper.sendChatMessage(
                        otherUser.getFcmToken(),
                        currentUser.getUsername(),
                        message
                );
                // Also show a local notification for the sender
                NotificationHelper.initNotificationChannel(this);
                NotificationHelper.showInfoNotification(
                        this,
                        "Message sent to " + otherUser.getUsername(),
                        message
                );
            } else {
                Log.e(TAG, "Failed to get current user details", task.getException());
            }
        });
    }

    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";   private static final String API_KEY = "ac84eaf87b4f708e5e0e7df84ce139caafaabc38";
    private static final MediaType APPLICATION_JSON_MEDIA_TYPE = MediaType.get("application/json");

    public void callApi(JSONObject jsonObject) {
        OkHttpClient client = new OkHttpClient.Builder().build();

        RequestBody body = RequestBody.create(jsonObject.toString(), APPLICATION_JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(FCM_URL)
                .header("Authorization", "key=" + API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Handle failure more meaningfully, e.g., re-throw the exception
                throw new RuntimeException("Failed to call API", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try{
                    if (response!= null && response.isSuccessful()) {
                        Log.d("API Response", "Success");
                    } else {
                        Log.e("API Response", "Failed: " + response);
                    }
                }finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }

            }
        });
    }
}

