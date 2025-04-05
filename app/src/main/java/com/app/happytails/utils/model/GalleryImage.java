package com.app.happytails.utils.model;

import android.net.Uri;

public class GalleryImage {
    public Uri picUri;

    public GalleryImage() {
    }

    public GalleryImage(Uri picUrl) {
        this.picUri = picUrl;
    }

    public Uri getPicUrl() {
        return picUri;
    }

    public void setPicUrl(Uri picUrl) {
        this.picUri = picUrl;
    }

}
