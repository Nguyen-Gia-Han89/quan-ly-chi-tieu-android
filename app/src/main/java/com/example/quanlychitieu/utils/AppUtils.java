package com.example.quanlychitieu.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppUtils {

    private static final String PREF_NAME = "UserPrefs";
    private static final String KEY_BUDGET = "monthly_budget";

    /**
     * Hàm lưu hạn mức ngân sách vào bộ nhớ đệm thiết bị (SharedPreferences)
     */
    public static void saveBudgetLocal(Context context, float budgetLimit) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(KEY_BUDGET, budgetLimit);
        editor.apply();
    }

    /**
     * Hàm lấy hạn mức ngân sách động cục bộ.
     * Thành viên 2 và Thành viên 4 sẽ gọi trực tiếp hàm này để kiểm tra điều kiện vượt ngân sách.
     */
    public static float getBudgetLimitLocal(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Mặc định trả về 0.0f nếu người dùng chưa thiết lập ngân sách
        return prefs.getFloat(KEY_BUDGET, 0.0f);
    }
}