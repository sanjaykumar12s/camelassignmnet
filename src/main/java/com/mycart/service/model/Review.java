package com.mycart.service.model;

public class Review {

    private int rating;  // Changed to int for numerical processing
    private String comment;

    // Getters and Setters

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
