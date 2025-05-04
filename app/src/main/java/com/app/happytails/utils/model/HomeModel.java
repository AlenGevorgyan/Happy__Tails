package com.app.happytails.utils.model;

import java.util.ArrayList;
import java.util.List;

public class HomeModel {
    private String creator;
    private String dogId;
    private String dogName;
    public List<String> dog_keywords;
    public String dog_lower;
    private int fundingPercentage;
    private ArrayList<String> galleryImages;
    private String mainImage;
    private ArrayList<String> supporters;
    private double fundingAmount;
    private ArrayList<String> donationsAmount;
    private int urgencylevel;
    private String patreonUrl;
    private String accessToken;

    // Empty constructor for Firestore
    public HomeModel() {
    }

    public HomeModel(String creator, String dogId, String dogName, int fundingProgress, String mainImageUrl, ArrayList<String> supporters, int urgencylevel) {
        this.creator = creator;
        this.dogName = dogName;
        this.fundingPercentage = fundingPercentage;
        this.galleryImages = galleryImages;
        this.mainImage = mainImageUrl;
        this.supporters = supporters;
        this.dogId = dogId;
        this.urgencylevel = urgencylevel;
    }

    public HomeModel(String creator, String dogId, String dogName,
                     int fundingPercentage, ArrayList<String> galleryImages, String mainImage,
                     ArrayList<String> supporters, double fundingAmount, ArrayList<String> donationsAmount, int urgencyLevel, String patreonUrl, String accessToken) {
        this.creator = creator;
        this.dogName = dogName;
        this.dog_keywords = generateThreeCharKeywords(dogName);
        this.dog_lower = dogName.toLowerCase();
        this.fundingPercentage = fundingPercentage;
        this.dogId = dogId;
        this.galleryImages = galleryImages;
        this.mainImage = mainImage;
        this.supporters = supporters;
        this.fundingAmount = fundingAmount;
        this.donationsAmount = donationsAmount;
        this.urgencylevel = urgencyLevel;
        this.patreonUrl = patreonUrl;
        this.accessToken = accessToken;
    }

    // Getters and setters


    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPatreonUrl() {
        return patreonUrl;
    }

    public void setPatreonUrl(String patreonUrl) {
        this.patreonUrl = patreonUrl;
    }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public int getUrgencylevel() {
        return urgencylevel;
    }

    public void setUrgencylevel(int urgencylevel) {
        this.urgencylevel = urgencylevel;
    }

    public double getFundingAmount() {
        return fundingAmount;
    }

    public void setFundingAmount(double fundingAmount) {
        this.fundingAmount = fundingAmount;
    }

    public ArrayList<String> getDonationsAmount() {
        return donationsAmount;
    }

    public void setDonationsAmount(ArrayList<String> donationsAmount) {
        this.donationsAmount = donationsAmount;
    }

    public String getDogId() { return dogId; }
    public void setDogId(String dogId) { this.dogId = dogId; }

    public String getDogName() { return dogName; }
    public void setDogName(String dogName) { this.dogName = dogName; }

    public int getFundingPercentage() { return fundingPercentage; }
    public void setFundingPercentage(int fundingPercentage) { this.fundingPercentage = fundingPercentage; }

    public ArrayList<String> getGalleryImages() { return galleryImages; }
    public void setGalleryImages(ArrayList<String> galleryImages) { this.galleryImages = galleryImages; }

    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }

    public ArrayList<String> getSupporters() { return supporters; }
    public void setSupporters(ArrayList<String> supporters) { this.supporters = supporters; }

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