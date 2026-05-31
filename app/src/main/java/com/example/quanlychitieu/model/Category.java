package com.example.quanlychitieu.model;

import java.io.Serializable;

public class Category implements Serializable {
    private String id;
    private String name;
    private String userId; // Nếu là "SYSTEM" -> Tất cả mọi người đều thấy. Nếu là UID -> Chỉ user đó thấy.

    public Category() {
    }

    public Category(String id, String name, String userId) {
        this.id = id;
        this.name = name;
        this.userId = userId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    // Ghi đè phương thức này để hiển thị trực tiếp chuỗi Tên danh mục
    @Override
    public String toString() {
        return name;
    }
}