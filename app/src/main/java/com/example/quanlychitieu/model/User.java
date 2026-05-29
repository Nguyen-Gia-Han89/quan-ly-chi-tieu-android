package com.example.quanlychitieu.model; // Thuộc package model

public class User {
    private String email;
    private double budgetLimit;

    public User() {}
    public User(String email, double budgetLimit) {
        this.email = email;
        this.budgetLimit = budgetLimit;
    }
    public String getEmail() { return email; }
    public double getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(double budgetLimit) { this.budgetLimit = budgetLimit; }
}