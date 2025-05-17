package com.app.happytails.utils.model;

import java.util.ArrayList;
import java.util.List;

public class HomeModel {
    private String dogId;
    private String creator;
    private String dogName;
    private String description;
    private String patreonUrl;
    private int urgencylevel;
    private String mainImage;
    private ArrayList<String> galleryImages;
    private String vetName;
    private String clinicName;
    private String clinicAddress;
    private String vetPhone;
    private String vetEmail;
    private String medicalHistory;
    private int fundingPercentage;
    private double fundingAmount;
    private double donationsAmount;
    private List<String> supporters;
    private String accessToken;
    public String dog_lower;
    public ArrayList<String> dog_keywords;

    public HomeModel() {}

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

    // Constructor with all fields
    public HomeModel(String dogId, String creator, String dogName, String description, String patreonUrl, int urgencylevel,
                     String mainImage, ArrayList<String> galleryImages, String vetName, String clinicName,
                     String clinicAddress, String vetPhone, String vetEmail, String medicalHistory,
                     int fundingPercentage, double fundingAmount, double donationsAmount, List<String> supporters,
                     String accessToken, String dog_lower, ArrayList<String> dog_keywords) {
        this.dogId = dogId;
        this.creator = creator;
        this.dogName = dogName;
        this.description = description;
        this.patreonUrl = patreonUrl;
        this.urgencylevel = urgencylevel;
        this.mainImage = mainImage;
        this.galleryImages = galleryImages;
        this.vetName = vetName;
        this.clinicName = clinicName;
        this.clinicAddress = clinicAddress;
        this.vetPhone = vetPhone;
        this.vetEmail = vetEmail;
        this.medicalHistory = medicalHistory;
        this.fundingPercentage = fundingPercentage;
        this.fundingAmount = fundingAmount;
        this.donationsAmount = donationsAmount;
        this.supporters = supporters;
        this.accessToken = accessToken;
        this.dog_lower = dog_lower;
        this.dog_keywords = dog_keywords;
    }

    // Getters and Setters
    public String getDogId() { return dogId; }
    public void setDogId(String dogId) { this.dogId = dogId; }
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    public String getDogName() { return dogName; }
    public void setDogName(String dogName) {
        this.dogName = dogName;
        this.dog_lower = dogName != null ? dogName.toLowerCase() : "";
        this.dog_keywords = generateKeywords(dogName);
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPatreonUrl() { return patreonUrl; }
    public void setPatreonUrl(String patreonUrl) { this.patreonUrl = patreonUrl; }
    public int getUrgencylevel() { return urgencylevel; }
    public void setUrgencylevel(int urgencylevel) { this.urgencylevel = urgencylevel; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public ArrayList<String> getGalleryImages() { return galleryImages; }
    public void setGalleryImages(ArrayList<String> galleryImages) { this.galleryImages = galleryImages; }
    public String getVetName() { return vetName; }
    public void setVetName(String vetName) { this.vetName = vetName; }
    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }
    public String getClinicAddress() { return clinicAddress; }
    public void setClinicAddress(String clinicAddress) { this.clinicAddress = clinicAddress; }
    public String getVetPhone() { return vetPhone; }
    public void setVetPhone(String vetPhone) { this.vetPhone = vetPhone; }
    public String getVetEmail() { return vetEmail; }
    public void setVetEmail(String vetEmail) { this.vetEmail = vetEmail; }
    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
    public int getFundingPercentage() { return fundingPercentage; }
    public void setFundingPercentage(int fundingPercentage) { this.fundingPercentage = fundingPercentage; }
    public double getFundingAmount() { return fundingAmount; }
    public void setFundingAmount(double fundingAmount) { this.fundingAmount = fundingAmount; }
    public double getDonationsAmount() { return donationsAmount; }
    public void setDonationsAmount(double donationsAmount) { this.donationsAmount = donationsAmount; }
    public List<String> getSupporters() { return supporters; }
    public void setSupporters(List<String> supporters) { this.supporters = supporters; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    private ArrayList<String> generateKeywords(String dogName) {
        ArrayList<String> keywords = new ArrayList<>();
        if (dogName != null && !dogName.isEmpty()) {
            String lowerName = dogName.toLowerCase();
            for (int i = 0; i < lowerName.length(); i++) {
                for (int j = i + 1; j <= lowerName.length(); j++) {
                    keywords.add(lowerName.substring(i, j));
                }
            }
        }
        return keywords;
    }
}