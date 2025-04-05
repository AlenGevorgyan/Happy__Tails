package com.app.happytails.utils.model;

public class UserSearchModel {
    private String userId;
    private String username;

    // Default constructor
    public UserSearchModel() {
    }

    public UserSearchModel(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    // Getters and setters for userId and username
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

}