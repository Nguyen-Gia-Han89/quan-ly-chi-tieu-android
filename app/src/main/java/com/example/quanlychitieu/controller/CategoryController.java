package com.example.quanlychitieu.controller;

import com.example.quanlychitieu.model.Category;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class CategoryController {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public CategoryController() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    public interface CategoryCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface CategoryListCallback {
        void onLoaded(List<Category> categories);
        void onFailure(String errorMessage);
    }

    // [CREATE] - Cho phép người dùng tự nhập tạo Danh mục mới
    public void addCustomCategory(String categoryName, CategoryCallback callback) {
        if (mAuth.getCurrentUser() == null) {
            callback.onFailure("Người dùng chưa đăng nhập!");
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();
        String id = db.collection("categories").document().getId();

        Category newCategory = new Category(id, categoryName, uid);

        db.collection("categories").document(id).set(newCategory)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    // [READ] - Lấy danh sách danh mục (Hệ thống + Cá nhân tự tạo)
    public void getAllCategories(CategoryListCallback callback) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";

        db.collection("categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Category cat = doc.toObject(Category.class);
                        if (cat != null) {
                            if (cat.getUserId().equals("SYSTEM") || cat.getUserId().equals(uid)) {
                                list.add(cat);
                            }
                        }
                    }

                    // Dự phòng nếu db trống, tự cung cấp list danh mục cơ bản ban đầu
                    if (list.isEmpty()) {
                        list.add(new Category("c1", "Ăn uống", "SYSTEM"));
                        list.add(new Category("c2", "Đi lại", "SYSTEM"));
                        list.add(new Category("c3", "Mua sắm", "SYSTEM"));
                        list.add(new Category("c4", "Giải trí", "SYSTEM"));
                    }
                    callback.onLoaded(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}