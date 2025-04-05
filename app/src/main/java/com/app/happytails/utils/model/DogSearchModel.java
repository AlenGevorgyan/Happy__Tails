package com.app.happytails.utils.model;

public class DogSearchModel {
    private String dogId;
    private String dogName;

    // Default constructor
    public DogSearchModel() {
    }

    public DogSearchModel(String dogId, String dogName) {
        this.dogId = dogId;
        this.dogName = dogName;
    }

    // Getters and setters for dogId and dogName
    public String getDogId() {
        return dogId;
    }

    public void setDogId(String dogId) {
        this.dogId = dogId;
    }

    public String getDogName() {
        return dogName;
    }

    public void setDogName(String dogName) {
        this.dogName = dogName;
    }
}