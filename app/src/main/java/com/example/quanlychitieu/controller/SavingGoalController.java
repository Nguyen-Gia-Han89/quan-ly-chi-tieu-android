package com.example.quanlychitieu.controller;

import com.example.quanlychitieu.model.SavingGoal;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SavingGoalController {
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    public SavingGoalController() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public interface SavingGoalCallback {
        void onLoaded(List<SavingGoal> goals);
        void onFailure(String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String message);
    }
    //lấy danh sách mục tiêu
    public void getSavingGoals(String monthKey, SavingGoalCallback callback) {

        if (auth.getCurrentUser() == null) {
            callback.onFailure("User chưa đăng nhập");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("saving_goals")
                .document(monthKey)
                .collection("goals")
                .get()
                .addOnSuccessListener(query -> {

                    List<SavingGoal> list = new ArrayList<>();
                    query.forEach(doc -> {
                        SavingGoal goal = doc.toObject(SavingGoal.class);
                        goal.setId(doc.getId());
                        list.add(goal);
                    });

                    callback.onLoaded(list);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e.getMessage());
                });
    }

    public void addSavingGoal(String monthKey, SavingGoal goal, ActionCallback callback) {
        if (auth.getCurrentUser() == null) {
            callback.onFailure("User chưa đăng nhập");
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        // 1. Tạo đường dẫn gốc tới bộ sưu tập goals của tháng
        var goalsCollection = db.collection("users")
                .document(uid)
                .collection("saving_goals")
                .document(monthKey)
                .collection("goals");

        // 2. Tìm kiếm trên Firestore xem đã có tài liệu nào trùng "categoryName" chưa
        goalsCollection.whereEqualTo("categoryName", goal.getCategoryName())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // ĐÃ TỒN TẠI CATEGORY NÀY -> TIẾN HÀNH CỘNG DỒN
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        SavingGoal existingGoal = doc.toObject(SavingGoal.class);

                        if (existingGoal != null) {
                            String docId = doc.getId();
                            double oldAmount = existingGoal.getTargetAmount();
                            double newAmount = oldAmount + goal.getTargetAmount();

                            // Cập nhật lại số tiền lên Firestore tại đúng ID cũ
                            goalsCollection.document(docId)
                                    .update("targetAmount", newAmount)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                        }
                    } else {
                        // CHƯA TỒN TẠI -> TẠO MỚI HOÀN TOÀN
                        goalsCollection.add(goal)
                                .addOnSuccessListener(documentReference -> callback.onSuccess())
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Lỗi kiểm tra trùng: " + e.getMessage()));
    }
}
