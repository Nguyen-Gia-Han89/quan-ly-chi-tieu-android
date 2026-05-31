package com.example.quanlychitieu.model;

import java.io.Serializable;

public class Transaction implements Serializable {
    private String id;
    private String userId;
    private double amount;
    private String category;
    private String date;
    private String note;
    private String type; // "EXPENSE" (Chi) hoặc "INCOME" (Thu)

    public Transaction() {
    }

    public Transaction(String id, String userId, double amount, String category, String date, String note, String type) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
        this.type = type;
    }

    // Getters và Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}