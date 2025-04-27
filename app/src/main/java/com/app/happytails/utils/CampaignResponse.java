package com.app.happytails.utils;

import java.util.List;

public class CampaignResponse {
    private List<Data> data;

    public List<Data> getData() {
        return data;
    }

    public static class Data {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}

