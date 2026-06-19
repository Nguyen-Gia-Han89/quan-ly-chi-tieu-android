package com.example.quanlychitieu.model;

public class SavingGoal {
    private String id;
    private String categoryId;
    private String categoryName;
    private double targetAmount;
    private String color;

    public SavingGoal() {
    }

    public SavingGoal(String id, String categoryName, double targetAmount, String color) {
        this.id = id;
        this.categoryId = "";
        this.categoryName = categoryName;
        this.targetAmount = targetAmount;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

}
