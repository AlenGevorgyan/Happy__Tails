package com.app.happytails.utils.model;


public class GalleryImage {
    public String picUri;

    public GalleryImage() {

    }

    public GalleryImage(String picUrl) {
        this.picUri = picUrl;
    }
    public String getPicUrl() {
        return picUri;
    }

    public void setPicUrl(String picUrl) {
        this.picUri = picUrl;
    }

}
