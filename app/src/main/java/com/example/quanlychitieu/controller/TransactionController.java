package com.example.quanlychitieu.controller;

import com.example.quanlychitieu.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class TransactionController {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public TransactionController() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public interface TransactionCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface TransactionListCallback {
        void onLoaded(List<Transaction> transactions, long totalSpent);
        void onFailure(String errorMessage);
    }

    // [CREATE] - Thêm mới giao dịch
    public void addTransaction(Transaction tx, TransactionCallback callback) {
        db.collection("transactions").document(tx.getId()).set(tx)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // [UPDATE] - Cập nhật thông tin giao dịch
    public void updateTransaction(Transaction tx, TransactionCallback callback) {
        db.collection("transactions").document(tx.getId()).set(tx)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // [DELETE] - Xóa bỏ giao dịch
    public void deleteTransaction(String txId, TransactionCallback callback) {
        db.collection("transactions").document(txId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // [READ] - Lấy danh sách + Lọc chuỗi theo Tháng/Năm (MM/yyyy) + Tính tổng tiền chi tiêu thực tế
    public void getTransactionsByMonth(String monthYear, TransactionListCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("Người dùng chưa đăng nhập!");
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("transactions")
                .whereEqualTo("userId", uid)
                .whereEqualTo("type", "EXPENSE")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> filteredList = new ArrayList<>();
                    long totalSpent = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction tx = doc.toObject(Transaction.class);
                        if (tx != null && tx.getDate().endsWith(monthYear)) {
                            filteredList.add(tx);
                            totalSpent += tx.getAmount();
                        }
                    }
                    callback.onLoaded(filteredList, totalSpent);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}