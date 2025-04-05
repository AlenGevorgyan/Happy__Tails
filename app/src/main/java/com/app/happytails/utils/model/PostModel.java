package com.app.happytails.utils.model;

import java.util.ArrayList;

public class PostModel {
    private int dogAge;
    private String dogGender;
    private String dogId;
    private String dogName;
    private int fundingPercentage;
    private String mainImage;
    private ArrayList<String> supporters;
    private String description;

    // Empty constructor for Firestore
    public PostModel() {
    }

    public ArrayList<String> getSupporters() {
        return supporters;
    }

    public void setSupporters(ArrayList<String> supporters) {
        this.supporters = supporters;
    }

    public PostModel(String dogId, int dogAge, String dogGender, String dogName, int fundingProgress, String mainImageUrl, ArrayList<String> supporters) {
        this.dogAge = dogAge;
        this.dogGender = dogGender;
        this.dogName = dogName;
        this.fundingPercentage = fundingPercentage;
        this.mainImage = mainImageUrl;
        this.supporters = supporters;
        this.dogId = dogId;
    }

    public PostModel(String dogId, int dogAge, String dogGender, String dogName,
                     int fundingPercentage, String mainImage,
                     ArrayList<String> supporters, String description) {
        this.dogAge = dogAge;
        this.dogGender = dogGender;
        this.dogName = dogName;
        this.fundingPercentage = fundingPercentage;
        this.dogId = dogId;
        this.mainImage = mainImage;
        this.supporters = supporters;
        this.description = description;
    }


    public int getDogAge() { return dogAge; }
    public void setDogAge(int dogAge) { this.dogAge = dogAge; }

    public String getDogGender() { return dogGender; }
    public void setDogGender(String dogGender) { this.dogGender = dogGender; }

    public String getDogId() { return dogId; }
    public void setDogId(String dogId) { this.dogId = dogId; }

    public String getDogName() { return dogName; }
    public void setDogName(String dogName) { this.dogName = dogName; }

    public int getFundingPercentage() { return fundingPercentage; }
    public void setFundingPercentage(int fundingPercentage) { this.fundingPercentage = fundingPercentage; }


    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}