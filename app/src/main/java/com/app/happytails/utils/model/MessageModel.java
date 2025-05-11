package com.app.happytails.utils.model;

import android.net.Uri;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;

public class MessageModel {
    private String message,senderId,id;
    List<String> imageUris;
    private Timestamp time;
    private String  profileImage,posterId;
    private String username;




    public MessageModel() {
    }
    public MessageModel(String message, String senderId, String posterId, List<String> imageUris, Timestamp time, String profileImage, String username,String id) {
        this.message = message;
        this.senderId = senderId;
        this.posterId = posterId;
        this.imageUris = imageUris;
        this.time = time;
        this.profileImage = profileImage;
        this.username = username;
        this.id = id;

    }


    public MessageModel(String message, String senderId, Timestamp time, List<String>imageUris) {
        this.message = message;
        this.senderId = senderId;
        this.time = time;
        this.imageUris = imageUris;
    }
    public MessageModel(String message, String senderId, Timestamp time, List<String>imageUris,String id) {
        this.message = message;
        this.senderId = senderId;
        this.time = time;
        this.imageUris = imageUris;
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public List<String> getImageUris() {
        return imageUris;
    }

    public void setImageUris(List<String> imageUris) {
        this.imageUris = imageUris;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPosterId() {
        return posterId;
    }

    public void setPosterId(String posterId) {
        this.posterId = posterId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
