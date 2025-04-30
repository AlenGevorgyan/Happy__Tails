package com.app.happytails.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SendNotification {

    private static final String TAG = "SendNotification";

    // Method to send a chat message (which triggers notification via Firestore Cloud Function)
    public void sendChatMessage(String chatroomId, String senderId, String messageText) {
        Map<String, Object> messageData = new HashMap<>();
        messageData.put("message", messageText);
        messageData.put("senderId", senderId);
        messageData.put("timestamp", FieldValue.serverTimestamp());
        // Add more fields as needed (e.g., recipientId)

        FirebaseFirestore.getInstance()
            .collection("chatrooms")
            .document(chatroomId)
            .collection("chats")
            .add(messageData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Message sent and will trigger notification");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending message: ", e);
            });
    }
}
