package com.app.happytails.utils;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.app.happytails.utils.model.UserModel;

import java.text.SimpleDateFormat;
import java.util.List;

public class FirebaseUtil {

    private static String accessToken;
    private static long tokenExpiryTime = 0;

    public static String getAccessToken() {
        // Check if we need to refresh the token
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiryTime) {
            // TODO: Implement proper OAuth2 token refresh
            // For now, return a placeholder token
            // In production, you should:
            // 1. Use Firebase Admin SDK to generate a token
            // 2. Cache the token with proper expiry time
            // 3. Implement token refresh logic
            return "\"{\\n\" +\n" +
                    "                    \"  \\\"type\\\": \\\"service_account\\\",\\n\" +\n" +
                    "                    \"  \\\"project_id\\\": \\\"rational-photon-380817\\\",\\n\" +\n" +
                    "                    \"  \\\"private_key_id\\\": \\\"ac84eaf87b4f708e5e0e7df84ce139caafaabc38\\\",\\n\" +\n" +
                    "                    \"  \\\"private_key\\\": \\\"-----BEGIN PRIVATE KEY-----\\\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDivcXkBq3/he/a\\\\nwiKQzRFrnzO5XWP8EilFhUUoegtC85o+DM+Z17WSPf+AnlwOLq+5jCtdnMW8kQTz\\\\nDmVAOwPCzuG5t7DC383fCkIL1Kl2YhSrf3u8FVJ5FEWt36ZOHrbQW0r7HPqfWwbI\\\\nzSbgHr2buRtNx1Qhzu7VMkUzIgm/t73c74bhrMUseKerHUvnMq0XdZa4TnXPgOfT\\\\noEC4/1WScmv+HRZ73s9aB4Oq0gpFV53LEvTkFMXt3gX6I5BrTxEM+BUWne+MUTCX\\\\n3S5m8VQuD1UdRbr/ppn6D5Q3sWU3nh6uIQMlo+84IBjpq6jFeWLAs9t1n2pKmNfM\\\\n7pM6JG03AgMBAAECggEAUWt++6ZwZdDFNANBYUjaKBDHhJkmFba5zkaQcnv8vkJ2\\\\nkTCNfbtpboXH0XpMKSWXoWPVkKyCjvduVQ9GyX1HmPUsNkHhfeDa7uwiklf/sEyB\\\\nCqJhHsVzU5o1eT3V3LiHwiL+NlUOJBoOQh31B0bSpHAgf1oD+o3x0mnWUom3AXn+\\\\nyN1j5D07C71Oe1atwIoZnbwpddvkQRJcWCtLuuYGcJwwi7gFPSvgiMSSmiiqwUqF\\\\numdoRho1aM1NuSIO+WziCBJohhhmkUtob2rEkU8c8M9mC3xngMZTl5eooSdrVsHS\\\\nNjyd9tDXaswa2V2m/QOjvPAFHNMcAouhShlJUufTOQKBgQD7mIWyXOjFbbDWVBC/\\\\nufeUuGs7nerKXbV64yNwcrxmvcdh/7nKD8yxjg6jlOpSQJTJKA8V3v48khZtVT9m\\\\nIoGFyXdTZP/JK4Jd+hpd6xXCrj+rp3i/QpdOKXmXBBYuFS/wOR8gCfqigtyrAZXY\\\\nPF1JfzXnHA1s1PANbcUZpPGCTwKBgQDmtd7EXPuowNlXB8oTbWLXlUEBX/CtDKd2\\\\nowzaCneXFiAwa+9nUlZbpIboI2bms9SRMXv4EQ3cc2pdeo+9LrwtdPBuJPsfk8RE\\\\nuV4PvqAk2t+jVkgtleO4Ssx5oKBOcF1oY0clWHjZ+D1blS18qHmzuWHd3dRQ0oNr\\\\nqhbwgXK0mQKBgQDmFAjPn59OTI1WsvHOIyaB3lRR5Iv+G8wGYQjboFEiM5LNz6n7\\\\nWo96H8rLVTcjmON3QSbqfU5J3d6chUTBBfUkf6SbotU3Bo7lmf3avUzdB7Q6KaCG\\\\nZ0Muu0byD06pPb7lE5efGQEW9E0QJRb+89TrjWWhv0mXqPMNlMCWPvyMiQKBgQDg\\\\nUxhun+aGmDT7nXRL3YFNEy/o0UtoR7SQ80ssux67BmV4D4rxUKrtYpVWJA4K5fIa\\\\n1x2t/48VuhdDG0el8EpCfMDGqCiQ9JHTLNYbwwNdsn/fBqcZw/NunzQgUyFsA2+f\\\\nb2CfHF4tumSWpv9ahUoIiYlyPB4UFAx65CB367YHiQKBgAZ9Y3wEr9taDlHlbXxl\\\\ngr6SndeQWL2Q68V4InRukIpdpI/Shx3KQayA/2vgdne71bDdy/q9jywCFB+Vw+ie\\\\nYvu+5v8HEsT96N9FDz29zQA2h6ZRJensYn2zVE8BilU8ioYJWstpFRCzv1y8AyIL\\\\nYtNBVXrcy2w3NjU5DDD+J2ut\\\\n-----END PRIVATE KEY-----\\\\n\\\",\\n\" +\n" +
                    "                    \"  \\\"client_email\\\": \\\"firebase-adminsdk-kq8br@rational-photon-380817.iam.gserviceaccount.com\\\",\\n\" +\n" +
                    "                    \"  \\\"client_id\\\": \\\"109336487132653487451\\\",\\n\" +\n" +
                    "                    \"  \\\"auth_uri\\\": \\\"https://accounts.google.com/o/oauth2/auth\\\",\\n\" +\n" +
                    "                    \"  \\\"token_uri\\\": \\\"https://oauth2.googleapis.com/token\\\",\\n\" +\n" +
                    "                    \"  \\\"auth_provider_x509_cert_url\\\": \\\"https://www.googleapis.com/oauth2/v1/certs\\\",\\n\" +\n" +
                    "                    \"  \\\"client_x509_cert_url\\\": \\\"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-kq8br%40rational-photon-380817.iam.gserviceaccount.com\\\",\\n\" +\n" +
                    "                    \"  \\\"universe_domain\\\": \\\"googleapis.com\\\"\\n\" +\n" +
                    "                    \"}\\n\";";
        }
        return accessToken;
    }

    public static void setAccessToken(String token, long expiryTimeInMillis) {
        accessToken = token;
        tokenExpiryTime = System.currentTimeMillis() + expiryTimeInMillis;
    }

    public static String currentUserId() {
        return FirebaseAuth.getInstance().getUid();
    }

    public static boolean isLoggedIn() {
        return currentUserId() != null;
    }

    public static DocumentReference currentUserDetails() {
        String currentUserId = FirebaseAuth.getInstance().getUid();

        if (currentUserId == null) {
            throw new IllegalStateException("User is not signed in.");
        }

        return FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUserId);
    }


    public static CollectionReference allUserCollectionReference() {
        return FirebaseFirestore.getInstance().collection("users");
    }

    public static DocumentReference getChatroomReference(String chatroomId) {
        return FirebaseFirestore.getInstance().collection("chatrooms").document(chatroomId);
    }

    public static CollectionReference getChatroomMessageReference(String chatroomId) {
        return getChatroomReference(chatroomId).collection("chats");
    }

    public static String getChatroomId(String userId1, String userId2) {
        if (userId1.hashCode() < userId2.hashCode()) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }

    public static CollectionReference allChatroomCollectionReference() {
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }

    public static DocumentReference getOtherUserFromChatroom(List<String> userIds) {
        return allUserCollectionReference().document(
                userIds.get(0).equals(currentUserId()) ? userIds.get(1) : userIds.get(0)
        );
    }

    public static String timestampToString(Timestamp timestamp) {
        return new SimpleDateFormat("HH:mm").format(timestamp.toDate());
    }

    public static Task<String> getOtherProfileImage(String otherUserId) {
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(otherUserId);
        return docRef.get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                return task.getResult().getString("userImage");
            } else {
                Log.d("FirebaseUtil", "No profile image found for user: " + otherUserId);
                return null;
            }
        });
    }

    public static Task<String> getDogProfileImage(String dogId) {
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("dogs").document(dogId);
        return docRef.get().continueWith(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                return task.getResult().getString("mainImage");
            } else {
                Log.d("FirebaseUtil", "No profile image found for user: " + dogId);
                return null;
            }
        });
    }

    public static StorageReference getOtherProfilePicStorageRef(String otherUserId){
        return FirebaseStorage.getInstance().getReference().child("profile_pic")
                .child(otherUserId);
    }

    // Get reference to a specific dog
    public static DocumentReference getDogReference(String dogId) {
        return FirebaseFirestore.getInstance().collection("dogs").document(dogId);
    }

    // Get current user details
    public interface UserDetailsCallback {
        void onCallback(UserModel user);
    }

    public static void getUserDetails(UserDetailsCallback callback) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId != null) {
            FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        callback.onCallback(user);
                    } else {
                        callback.onCallback(null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseUtil", "Error getting user details", e);
                    callback.onCallback(null);
                });
        } else {
            callback.onCallback(null);
        }
    }
}
