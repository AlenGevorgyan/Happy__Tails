package com.app.happytails.utils;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.Adapters.MessagesAdapter;
import com.app.happytails.utils.model.ChatroomModel;
import com.app.happytails.utils.model.MessageModel;
import com.app.happytails.utils.model.UserModel;
import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final int PICK_IMAGE_REQUEST = 1001;

    private CircleImageView otherProfilePic;

    private TextView otherUsername;

    private UserModel otherUser;
    private String chatroomId;
    private ImageButton backBtn;
    private ChatroomModel chatroomModel;
    private MessagesAdapter adapter;

    private EditText messageInput;
    private ImageButton sendTextBtn;
    private ImageView sendImageBtn, imagePreview;
    private RecyclerView recyclerView;

    private Uri pendingImageUri;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // ensure chat list resizes above the keyboard
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        storageReference = FirebaseStorage.getInstance().getReference("chat_images");

        otherUser   = AndroidUtil.getUserModelFromIntent(getIntent());
        chatroomId  = FirebaseUtil.getChatroomId(
                FirebaseUtil.currentUserId(),
                otherUser.getUserId()
        );

        otherProfilePic = findViewById(R.id.profile_pic_inchat);
        otherUsername = findViewById(R.id.other_username);
        backBtn = findViewById(R.id.back_btn_chat);

        backBtn.setOnClickListener(view -> finish());

        Glide.with(ChatActivity.this)
                .load(otherUser.getUserImage())
                .placeholder(R.drawable.user_icon)
                .centerCrop()
                .into(otherProfilePic);
        otherUsername.setText(otherUser.getUsername());

        messageInput = findViewById(R.id.chat_message_input);
        sendTextBtn  = findViewById(R.id.message_send_btn);
        sendImageBtn = findViewById(R.id.buttonSendImage);
        imagePreview = findViewById(R.id.imagePreview);
        recyclerView = findViewById(R.id.chat_recycler_view);

        // pick an image, but don't send yet
        sendImageBtn.setOnClickListener(v -> openImagePicker());

        // send combined text+image
        sendTextBtn.setOnClickListener(v -> {
            String text = messageInput.getText().toString().trim();
            sendCombinedMessage(text, pendingImageUri);
        });

        getOrCreateChatroomModel();
        setupChatRecyclerView();
    }

    private void openImagePicker() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == PICK_IMAGE_REQUEST && res == RESULT_OK
                && data != null && data.getData() != null) {
            pendingImageUri = data.getData();
            imagePreview.setImageURI(pendingImageUri);
            imagePreview.setVisibility(View.VISIBLE);
        }
    }

    private void sendCombinedMessage(String text, Uri imageUri) {
        sendTextBtn.setEnabled(false);

        // 1) update chatroom metadata
        chatroomModel.setLastMessage(text.isEmpty() ? "[Image]" : text);
        chatroomModel.setLastMessageTimestamp(Timestamp.now());
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        FirebaseUtil.getChatroomReference(chatroomId).set(chatroomModel);

        // 2) prepare MessageModel fields
        String msgId = UUID.randomUUID().toString();
        List<String> imageList = new ArrayList<>();

        Runnable sendMsgRunnable = () -> {
            FirebaseUtil.getUserDetails(currentUser -> {
                MessageModel msg = new MessageModel(
                        text.isEmpty() ? null : text,
                        currentUser.getUserId(),
                        currentUser.getUserId(),
                        imageList.isEmpty() ? null : imageList,
                        Timestamp.now(),
                        currentUser.getUserImage(),
                        currentUser.getUsername(),
                        msgId
                );

                FirebaseUtil.getChatroomMessageReference(chatroomId)
                        .document(msgId)
                        .set(msg)
                        .addOnSuccessListener(a -> {
                            // clear UI
                            messageInput.setText("");
                            pendingImageUri = null;
                            imagePreview.setImageURI(null);
                            imagePreview.setVisibility(View.GONE);
                            // notification
                            NotificationHelper.sendChatNotification(
                                    otherUser.getUserId(),
                                    currentUser.getUsername(),
                                    text.isEmpty() ? "Sent you an image" : text
                            );
                            sendTextBtn.setEnabled(true);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "sendCombinedMessage failed", e);
                            sendTextBtn.setEnabled(true);
                        });
            });
        };

        if (imageUri != null) {
            // start slide_fade_in animation on preview
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_slide_in);
            anim.setRepeatCount(Animation.INFINITE);
            imagePreview.startAnimation(anim);

            imagePreview.clearAnimation();


            // upload image first
            String fileName = UUID.randomUUID().toString();
            storageReference.child(fileName)
                    .putFile(imageUri)
                    .addOnSuccessListener(task -> task.getStorage()
                            .getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                // stop animation
                                imagePreview.clearAnimation();
                                imageList.add(uri.toString());
                                sendMsgRunnable.run();
                            })
                    )
                    .addOnFailureListener(e -> {
                        // stop animation on failure
                        imagePreview.clearAnimation();
                        Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                        sendTextBtn.setEnabled(true);
                    });
        } else {
            // no image, just send text
            sendMsgRunnable.run();
        }
    }

    private void setupChatRecyclerView() {
        Query q = FirebaseUtil.getChatroomMessageReference(chatroomId)
                .orderBy("time", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<MessageModel> opts =
                new FirestoreRecyclerOptions.Builder<MessageModel>()
                        .setQuery(q, MessageModel.class)
                        .build();

        adapter = new MessagesAdapter(opts, this, FirebaseUtil.currentUserId(), otherUser.getUserId());
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setReverseLayout(false);
        lm.setStackFromEnd(true);

        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);
        adapter.startListening();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onItemRangeInserted(int pos, int cnt) {
                recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
            }
        });
    }

    @Override protected void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }
    @Override protected void onStop() {
        super.onStop();
        if (adapter != null) adapter.stopListening();
    }

    private void getOrCreateChatroomModel() {
        FirebaseUtil.getChatroomReference(chatroomId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatroomModel = task.getResult().toObject(ChatroomModel.class);
                        if (chatroomModel == null) {
                            chatroomModel = new ChatroomModel(
                                    chatroomId,
                                    Arrays.asList(
                                            FirebaseUtil.currentUserId(),
                                            otherUser.getUserId()
                                    ),
                                    Timestamp.now(),
                                    ""
                            );
                            FirebaseUtil.getChatroomReference(chatroomId)
                                    .set(chatroomModel);
                        }
                    } else {
                        Log.e(TAG, "getOrCreateChatroomModel failed", task.getException());
                    }
                });
    }
}
