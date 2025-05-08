package com.app.happytails.utils;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryUtil extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Map config = new HashMap();
        config.put("cloud_name", "dzwoyslx4");
        config.put("api_key", "936129888839456");
        config.put("api_secret", "K4vL432ZheS8N6uJARlvzUh1Yww");
        MediaManager.init(this, config);

    }
}