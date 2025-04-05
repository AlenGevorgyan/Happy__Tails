package com.app.happytails.utils.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class UserModel {
    private String username;
    public List<String> username_keywords;
    public String username_lower;
    private String email;
    private String userId;

    private List<String> followers, followings;
    private int posts;
    private Timestamp createdTimestamp;

    private String userImage;
    private String public_id;

    private String status;

    private String fcmToken;

    public UserModel() {
    }

    public UserModel(Timestamp createdTimestamp, String username, String email, String userId, String userImage, List<String> followers, List<String> followings, int posts, String status) {
        this.createdTimestamp = createdTimestamp;
        this.username = username;
        this.username_keywords = generateThreeCharKeywords(username);
        this.username_lower = username.toLowerCase();
        this.email = email;
        this.userId = userId;
        this.followers = followers;
        this.posts = posts;
        this.userImage = userImage;
        this.status = status;
        this.followings = followings;
    }

    public List<String> getFollowings() {
        return followings;
    }

    public void setFollowings(List<String> followings) {
        this.followings = followings;
    }

    public String getUserImage() {
        return userImage;
    }

    public void setUserImage(String userImage) {
        this.userImage = userImage;
    }

    public List<String> getFollowers() {
        return followers;  // Get the list of followers
    }

    public void setFollowers(List<String> followers) {
        this.followers = followers;  // Set the list of followers
    }

    public int getPosts() {
        return posts;
    }

    public void setPosts(int posts) {
        this.posts = posts;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Timestamp getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getPublic_id() {
        return public_id;
    }

    public void setPublic_id(String public_id) {
        this.public_id = public_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public List<String> generateThreeCharKeywords(String input) {
        List<String> keywords = new ArrayList<>();

        if (input == null || input.length() < 3) {
            return keywords;
        }

        for (int i = 0; i <= input.length() - 3; i++) {
            String part = input.substring(i, i + 3).toLowerCase();
            keywords.add(part);
        }

        return keywords;
    }
}
