package com.app.happytails.utils.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.FirebaseUtil;
import com.app.happytails.utils.model.MessageModel;
import com.bumptech.glide.Glide;
import com.denzcoskun.imageslider.ImageSlider;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class MessagesAdapter extends FirestoreRecyclerAdapter<MessageModel, MessagesAdapter.MessagesViewHolder> {
    private static Context context;
    private static OnPressed onPressed;
    private Handler handler;
    private Runnable runnable;
    private String userId1, userId2;
    private boolean isHolding = false;

    public MessagesAdapter(FirestoreRecyclerOptions<MessageModel> options, Context context, String userId1, String userId2) {
        super(options);
        this.context = context;
        this.userId1 = userId1;
        this.userId2 = userId2;
    }

    @NonNull
    @Override
    public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.chat_message_recycler_row, parent, false);
        return new MessagesViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull MessagesViewHolder holder, int position, @NonNull MessageModel model) {
        if (model.getSenderId().equals(FirebaseUtil.currentUserId())) {
            holder.leftChatLayout.setVisibility(View.GONE);
            holder.rightChatLayout.setVisibility(View.VISIBLE);
            holder.rightChatTextview.setText(model.getMessage());
            holder.sliderRight.setVisibility(View.GONE);
            holder.hint.setVisibility(View.GONE);

            holder.rightChatLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isHolding = true;
                            handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(runnable = () -> {
                                if (isHolding) {
                                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                                    if (vibrator != null) {
                                        vibrator.vibrate(100);
                                    }
                                    showDeleteDialog(model.getId(), holder.getAdapterPosition());
                                }
                            }, 1000);
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            isHolding = false;
                            if (handler != null) {
                                handler.removeCallbacks(runnable);
                            }
                            break;
                    }
                    return true;
                }
            });

            if (model.getImageUris() != null && !model.getImageUris().isEmpty()) {
                ArrayList<SlideModel> imageList = new ArrayList<>();
                for (String url : model.getImageUris()) {
                    imageList.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
                }
                adjustSliderDimensions(holder.sliderRight);
                holder.sliderRight.setVisibility(View.VISIBLE);
                holder.sliderRight.setImageList(imageList);
                holder.hint.setVisibility(View.VISIBLE);
            }

            if (model.getProfileImage() != null) {
                holder.postLayoutRight.setVisibility(View.VISIBLE);
                holder.posterNameRight.setText(model.getUsername());
                Glide.with(context)
                        .load(model.getProfileImage())
                        .placeholder(R.drawable.user_icon)
                        .timeout(6500)
                        .into(holder.profilePicRight);
            } else {
                holder.postLayoutRight.setVisibility(View.GONE);
            }

        } else {
            holder.rightChatLayout.setVisibility(View.GONE);
            holder.leftChatLayout.setVisibility(View.VISIBLE);
            holder.leftChatTextview.setText(model.getMessage());
            holder.sliderLeft.setVisibility(View.GONE);

            if (model.getImageUris() != null && !model.getImageUris().isEmpty()) {
                ArrayList<SlideModel> imageList = new ArrayList<>();
                for (String url : model.getImageUris()) {
                    imageList.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
                }
                adjustSliderDimensions(holder.sliderLeft);
                holder.sliderLeft.setVisibility(View.VISIBLE);
                holder.sliderLeft.setImageList(imageList);
            }

            if (model.getProfileImage() != null) {
                holder.postLayoutLeft.setVisibility(View.VISIBLE);
                holder.posterNameLeft.setText(model.getUsername());
                Glide.with(context)
                        .load(model.getProfileImage())
                        .placeholder(R.drawable.user_icon)
                        .timeout(6500)
                        .into(holder.profilePicLeft);
            } else {
                holder.postLayoutLeft.setVisibility(View.GONE);
            }
        }
    }

    private void adjustSliderDimensions(ImageSlider slider) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), R.drawable.map_icon, options);
        float aspectRatio = options.outWidth / (float) options.outHeight;
        int calculatedHeight = (int) (screenWidth / aspectRatio);
        ViewGroup.LayoutParams layoutParams = slider.getLayoutParams();
        layoutParams.height = calculatedHeight;
        slider.setLayoutParams(layoutParams);
    }

    public interface OnPressed {
        void delete(String id);
    }

    public void OnPressed(OnPressed onPressed) {
        this.onPressed = onPressed;
    }

    public static class MessagesViewHolder extends RecyclerView.ViewHolder {
        LinearLayout leftChatLayout, rightChatLayout;
        TextView leftChatTextview, rightChatTextview, posterNameRight, posterNameLeft, hint;
        ImageSlider sliderLeft, sliderRight;
        ImageView profilePicLeft, profilePicRight;
        LinearLayout postLayoutLeft, postLayoutRight;

        public MessagesViewHolder(@NonNull View itemView) {
            super(itemView);
            leftChatLayout = itemView.findViewById(R.id.left_chat_layout);
            rightChatLayout = itemView.findViewById(R.id.right_chat_layout);
            leftChatTextview = itemView.findViewById(R.id.left_chat_textview);
            rightChatTextview = itemView.findViewById(R.id.right_chat_textview);
            sliderLeft = itemView.findViewById(R.id.image_slider);
            sliderRight = itemView.findViewById(R.id.image_slider2);
            postLayoutLeft = itemView.findViewById(R.id.postLayoutLeft);
            postLayoutRight = itemView.findViewById(R.id.postLayoutRight);
            profilePicLeft = itemView.findViewById(R.id.image_profile);
            profilePicRight = itemView.findViewById(R.id.image_profile2);
            posterNameLeft = itemView.findViewById(R.id.poster_name);
            posterNameRight = itemView.findViewById(R.id.poster_name2);
            hint = itemView.findViewById(R.id.hint);
        }
    }

    private void showDeleteDialog(String messageId, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Delete from Firestore
                    FirebaseFirestore.getInstance()
                            .collection("chatrooms")
                            .document(FirebaseUtil.getChatroomId(userId1, userId2)) // Assumes FirebaseUtil.getChatId() returns the current chatId
                            .collection("chats")
                            .document(messageId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show();
                                // Notify adapter of data change (FirestoreRecyclerAdapter handles UI update)
                                if (onPressed != null) {
                                    onPressed.delete(messageId);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to delete message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
}