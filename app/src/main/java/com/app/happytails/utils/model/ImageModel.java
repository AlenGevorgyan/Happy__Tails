package com.app.happytails.utils.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class ImageModel {
    private String imageUrl, id;

    @ServerTimestamp
    private Date timestamp;

    public ImageModel(){

    }

    public ImageModel(String imageUrl, String id, Date timestamp) {
        this.imageUrl = imageUrl;
        this.id = id;
        this.timestamp = timestamp;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
