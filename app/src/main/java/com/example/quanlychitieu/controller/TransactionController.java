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

    // Giao diện callback được cập nhật: trả về cả danh sách, tổng thu và tổng chi kiểu double
    public interface TransactionListCallback {
        void onLoaded(List<Transaction> transactions, double totalIncome, double totalExpense);
        void onFailure(String errorMessage);
    }

    // [CREATE] - Thêm mới giao dịch kèm sinh ID ngẫu nhiên ổn định từ Firestore nếu chưa có
    public void addTransaction(Transaction tx, TransactionCallback callback) {
        if (tx.getId() == null || tx.getId().isEmpty()) {
            String id = db.collection("transactions").document().getId();
            tx.setId(id);
        }
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

    // [DELETE] - Xóa giao dịch khỏi Firestore
    public void deleteTransaction(String txId, TransactionCallback callback) {
        db.collection("transactions").document(txId).delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // [READ] - Lấy danh sách, Lọc theo Tháng/Năm (MM/yyyy), tính Tổng Thu & Tổng Chi
    public void getTransactionsByMonth(String monthYear, TransactionListCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("Người dùng chưa đăng nhập!");
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();

        // Lấy tất cả giao dịch của User này (không giới hạn riêng EXPENSE)
        db.collection("transactions")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> filteredList = new ArrayList<>();
                    double totalIncome = 0;
                    double totalExpense = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction tx = doc.toObject(Transaction.class);
                        // Lọc những giao dịch kết thúc bằng chuỗi ngày "MM/yyyy" (Ví dụ: "08/06/2026" kết thúc bằng "06/2026")
                        if (tx != null && tx.getDate() != null && tx.getDate().endsWith(monthYear)) {
                            filteredList.add(tx);

                            // Phân loại tính tổng tiền
                            if ("INCOME".equals(tx.getType())) {
                                totalIncome += tx.getAmount();
                            } else if ("EXPENSE".equals(tx.getType())) {
                                totalExpense += tx.getAmount();
                            }
                        }
                    }
                    // Trả kết quả chuẩn về cho Activity hiển thị lên View
                    callback.onLoaded(filteredList, totalIncome, totalExpense);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    //Lấy tất cả giao dịch của user
    public void getAllTransactions(TransactionListCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("Người dùng chưa đăng nhập!");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("transactions")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    List<Transaction> list = new ArrayList<>();
                    double totalIncome = 0;
                    double totalExpense = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction tx = doc.toObject(Transaction.class);

                        if (tx == null) continue;
                        list.add(tx);

                        if ("INCOME".equals(tx.getType())) {
                            totalIncome += tx.getAmount();
                        } else if ("EXPENSE".equals(tx.getType())) {
                            totalExpense += tx.getAmount();
                        }
                    }

                    callback.onLoaded(list, totalIncome, totalExpense);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage())
                );
    }
}