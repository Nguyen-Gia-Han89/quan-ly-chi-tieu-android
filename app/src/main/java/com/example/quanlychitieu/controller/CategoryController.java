package com.example.quanlychitieu.controller;

import com.example.quanlychitieu.model.Category;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
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
        void onLoaded(List<Category> categories);
        void onFailure(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    /**
     * [READ] - Lấy danh mục hợp lệ cho User hiện tại đang đăng nhập.
     * Tự động kiểm tra và đẩy danh mục mặc định lên Firebase nếu database trống.
     */
    public void getAllCategories(CategoryCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("Người dùng chưa đăng nhập hệ thống!");
            return;
        }

        String currentUid = currentUser.getUid();

        db.collection("categories")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Category> accessibleCategories = new ArrayList<>();
                    boolean hasSystemCategories = false;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Category category = doc.toObject(Category.class);
                        if (category != null) {
                            if (category.getId() == null || category.getId().isEmpty()) {
                                category.setId(doc.getId());
                            }

                            String catUserId = category.getUserId();
                            if ("SYSTEM".equalsIgnoreCase(catUserId)) {
                                hasSystemCategories = true;
                                accessibleCategories.add(category);
                            } else if (currentUid.equals(catUserId)) {
                                accessibleCategories.add(category);
                            }
                        }
                    }

                    if (!hasSystemCategories) {
                        initializeSystemCategoriesInFirebase(new ActionCallback() {
                            @Override
                            public void onSuccess() {
                                getAllCategories(callback);
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                callback.onFailure("Không thể khởi tạo danh mục hệ thống: " + errorMessage);
                            }
                        });
                    } else {
                        callback.onLoaded(accessibleCategories);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Hàm phụ trợ: Tự động lưu hàng loạt (Batch Write) các danh mục mặc định lên Firebase
     */
    private void initializeSystemCategoriesInFirebase(ActionCallback callback) {
        WriteBatch batch = db.batch();
        String[] defaultNames = {"Ăn uống", "Tiền lương", "Mua sắm", "Di chuyển", "Giải trí"};

        for (String name : defaultNames) {
            String generatedId = db.collection("categories").document().getId();
            Category systemCat = new Category(generatedId, name, "SYSTEM");
            batch.set(db.collection("categories").document(generatedId), systemCat);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * [CREATE] - Thêm một danh mục cá nhân mới
     * Tự động gán userId của tài khoản đang thao tác để bảo mật danh mục này.
     */
    public void addCustomCategory(String categoryName, ActionCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("Vui lòng đăng nhập để thực hiện chức năng này!");
            return;
        }

        if (categoryName == null || categoryName.trim().isEmpty()) {
            callback.onFailure("Tên danh mục không được để trống!");
            return;
        }

        String generatedId = db.collection("categories").document().getId();
        Category newCategory = new Category(generatedId, categoryName.trim(), currentUser.getUid());

        db.collection("categories")
                .document(generatedId)
                .set(newCategory)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * [UPDATE] - Chỉnh sửa thông tin danh mục
     */
    public void updateCategory(Category category, ActionCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("Chưa đăng nhập!");
            return;
        }

        if (category == null || category.getId() == null || category.getId().isEmpty()) {
            callback.onFailure("Dữ liệu cập nhật không hợp lệ!");
            return;
        }

        if ("SYSTEM".equalsIgnoreCase(category.getUserId())) {
            callback.onFailure("Không có quyền chỉnh sửa danh mục mặc định của hệ thống!");
            return;
        }

        if (!currentUser.getUid().equals(category.getUserId())) {
            callback.onFailure("Bạn không có quyền chỉnh sửa danh mục này!");
            return;
        }

        db.collection("categories")
                .document(category.getId())
                .set(category)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * [DELETE] - Xóa danh mục dựa vào đối tượng cụ thể kèm kiểm tra đặc quyền sở hữu
     */
    public void deleteCategory(Category category, ActionCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure("Chưa đăng nhập!");
            return;
        }

        if (category == null || category.getId() == null || category.getId().isEmpty()) {
            callback.onFailure("Danh mục xóa không tồn tại!");
            return;
        }

        if ("SYSTEM".equalsIgnoreCase(category.getUserId())) {
            callback.onFailure("Không thể xóa danh mục mặc định của hệ thống!");
            return;
        }

        if (!currentUser.getUid().equals(category.getUserId())) {
            callback.onFailure("Bạn không có quyền xóa danh mục của người khác!");
            return;
        }

        db.collection("categories")
                .document(category.getId())
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}