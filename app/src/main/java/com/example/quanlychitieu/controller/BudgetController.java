package com.example.quanlychitieu.controller; // Thuộc package controller

import com.example.quanlychitieu.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // 🌟 Nhớ import thêm thư viện này

import java.util.HashMap;
import java.util.Map;

public class BudgetController {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public BudgetController() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }


    public interface BudgetCallback {
        void onSuccess(double budgetLimit);
        void onFailure(String errorMessage);
    }


    public void updateBudget(double newBudget, BudgetCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("Người dùng chưa đăng nhập!");
            return;
        }
        String currentUid = mAuth.getCurrentUser().getUid();


        Map<String, Object> data = new HashMap<>();
        data.put("budgetLimit", newBudget);


        db.collection("users").document(currentUid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(newBudget))
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}