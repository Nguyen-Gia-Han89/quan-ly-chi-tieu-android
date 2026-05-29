package com.example.quanlychitieu.view;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.quanlychitieu.R;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout btnHistory, btnStats, btnCreateExpense;
    private CardView btnProfile;
    private TextView txtTotalSpent;
    private TextView txtHomeUserName; // Hiển thị tên người dùng lấy từ SharedPreferences

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        btnProfile = findViewById(R.id.btnProfile);
        btnHistory = findViewById(R.id.btnHistory);
        btnStats = findViewById(R.id.btnStats);
        btnCreateExpense = findViewById(R.id.btnCreateExpense); // Cả cụm "Tạo chi tiêu mới" ở góc dưới
        txtTotalSpent = findViewById(R.id.txtTotalSpent);


        txtHomeUserName = findViewById(R.id.txtHomeUserName);


        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
        });


        btnHistory.setOnClickListener(v -> {
            Toast.makeText(this, "Mở Nhật ký chi tiêu", Toast.LENGTH_SHORT).show();
        });


        btnStats.setOnClickListener(v -> {
            Toast.makeText(this, "Mở Thống kê chi tiêu", Toast.LENGTH_SHORT).show();
        });


        btnCreateExpense.setOnClickListener(v -> {
            Toast.makeText(this, "Mở form thêm chi tiêu mới", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkBudgetLimit();
    }

    private void checkBudgetLimit() {
        SharedPreferences pref = getSharedPreferences("MoneyMate", MODE_PRIVATE);


        long limit = pref.getLong("budget_limit", 0);
        String userName = pref.getString("user_name", "");


        if (txtHomeUserName != null) {
            if (!userName.isEmpty()) {
                txtHomeUserName.setText("Chào " + userName + "!");
            } else {
                txtHomeUserName.setText("Chào bạn!");
            }
        }


        long spent = 0;
        txtTotalSpent.setText(spent + "VNĐ");


        if (limit > 0 && spent > limit) {

            txtTotalSpent.setTextColor(Color.RED);
            Toast.makeText(this, "Cảnh báo: Chi tiêu vượt quá ngân sách cài đặt!", Toast.LENGTH_LONG).show();
        } else {

            txtTotalSpent.setTextColor(Color.parseColor("#5E17EB"));
        }
    }
}